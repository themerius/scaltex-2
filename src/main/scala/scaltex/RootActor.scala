package scaltex

import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.Actor
import akka.actor.Props

import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.collection.mutable.Stack
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

import com.github.pathikrit.dijon.Json
import com.github.pathikrit.dijon.parse
import com.github.pathikrit.dijon.`{}`

import com.m3.curly.HTTP

import Messages._

class RootActor(updater: ActorRef, docProps: Props) extends Actor {

  val rootId = self.path.name

  val topology = Map[String, Map[String, String]]()
  this.update(rootId, "", "")

  val graveyard = Set[String]()

  val addresses = Map[String, ActorRef]()
  addresses(rootId) = self
  addresses("") = null

  var documentHome = ""
  var rev = ""

  val neighborDocuments = Set[ActorRef]()

  val moving = Map[String, Tuple3[ActorSelection, Any, Delta]]()

  def receive = {

    case InsertNext(newElem, after) => {
      val newId = newElem.path.name
      val afterId = after.path.name
      val `after.nextId` = topology(afterId)("next")
      val `after.firstChild` = topology(afterId)("firstChild")

      newElem ! Next(`after.nextId`)
      after ! Next(newId)

      this.update(newId, `after.nextId`, "")
      this.update(afterId, newId, `after.firstChild`)
      addresses(newId) = newElem

      val isLeaf = `after.firstChild`.isEmpty
      if (isLeaf) {
        updater ! InsertDelta(newId, after = afterId)
      } else {
        val lastChildId = this.diggNext(`after.firstChild`).reverse.head
        updater ! InsertDelta(newId, after = lastChildId)
      }

      this.persistTopology
    }

    case request @ InsertNextRequest(newId, msgs) => {
      //context.parent ! InsertNextCreateChild(request)
      // TODO: this only works, if parent of root can InsertNextCreateChild
      println("INSERT next to root. (Not implemented yet)")
    }

    case InsertNextCreateChild(request) => {
      val newChild = context.actorOf(docProps, request.newId)
      request.initMsgs.map(msg => newChild ! msg)
      newChild ! DocumentHome(this.documentHome)
      self ! InsertNext(newChild, after = sender)
    }

    case InsertFirstChild(newChild, at) => {
      val oldFirstChildId = topology(at.path.name)("firstChild")
      val next = topology(at.path.name)("next")
      newChild ! Next(oldFirstChildId)
      if (at == self)
        println("INSERT a firstChild to root.")
      else
        at ! FirstChild(newChild)
      this.update(newChild.path.name, oldFirstChildId, "")
      this.update(at.path.name, next, newChild.path.name)
      addresses(newChild.path.name) = newChild
      updater ! InsertDelta(newChild.path.name, after = at.path.name)

      this.persistTopology
    }

    case InsertFirstChildRequest(newId, msgs) => {
      val newChild = context.actorOf(docProps, newId)
      msgs.map(msg => newChild ! msg)
      newChild ! DocumentHome(this.documentHome)
      self ! InsertFirstChild(newChild, at = self)
    }

    case Move(ontoId) => {
      if (addresses.contains(ontoId)) {
        val mover = new utils.ParentPreviousHelper(sender, immutableTopology)
        val onto = new utils.ParentPreviousHelper(addresses(ontoId), immutableTopology)

        // fill the gap
        if (mover.id != onto.previous) {
          this.moveChangeTopology(mover, onto)

          // kill the hierarchy of the element which is hung somewhere else
          context.watch(mover.ref)
          mover.ref ! akka.actor.PoisonPill

          // let the leaf or subtree build from database
          // note: when mover is terminated, this means root got
          // the Terminate message from the poisoned mover
          this.rebuildSubtreeFromDb(mover, onto)

          this.persistTopology
        }
      } else {
        println(s"Move; the element '$ontoId' doesn't exist?")
      }
    }

    case akka.actor.Terminated(ref) => {
      // if leaf or subtree is dead,
      // let the leaf or subtree rebuild from database (at new position)
      if (moving.contains(ref.path.name)) {
        val parent = moving(ref.path.name)._1
        val message = moving(ref.path.name)._2
        val delta = moving(ref.path.name)._3
        updater ! delta
        parent ! message
        moving.remove(ref.path.name)
      }
      context.unwatch(ref)
    }

    case Remove(id) => {
      if (addresses.contains(id)) {
        val removed = new utils.ParentPreviousHelper(sender, immutableTopology)

        val allRemoved = this.order(removed.id)

        this.graveyard ++= allRemoved

        // fill the gap
        this.update(removed.previous, removed.next, removed.previousFirstChild)
        if (removed.id == removed.parentFirstChild)
          this.update(removed.parent, removed.parentNext, removed.next)

        if (removed.previous.nonEmpty) {
          addresses(removed.previous) ! Next(removed.next)
          addresses(removed.previous) ! FirstChild(addresses(removed.previousFirstChild))
        }

        if (removed.id == removed.parentFirstChild) {
          if (removed.parent.nonEmpty)
            addresses(removed.parent) ! FirstChild(addresses(removed.next))
        }

        for (id <- allRemoved) {
          this.topology.remove(id)
          this.addresses.remove(id)
        }

        // kill the hierarchy of the element which is hung somewhere else
        context.watch(removed.ref)
        removed.ref ! akka.actor.PoisonPill

        this.persistTopology
      } else {
        println(s"Remove; the element '$id' doesn't exist?")
      }
    }

    case Update =>
      context.actorSelection(topology(rootId)("firstChild")) ! Update

    case Setup => {
      val firstChildRef = topology(rootId)("firstChild")
      val imTopology = this.immutableTopology
      if (firstChildRef.nonEmpty) {
        val firstChild = context.actorOf(docProps, firstChildRef)
        self ! UpdateAddress(firstChildRef, firstChild)
        firstChild ! Setup(imTopology, DocumentHome(this.documentHome))
        val nexts = diggNext(firstChildRef)
        for (next <- nexts) {
          val nextActor = context.actorOf(docProps, next)
          self ! UpdateAddress(next, nextActor)
          nextActor ! Setup(imTopology, DocumentHome(this.documentHome))
        }
      }
    }

    case SetupSubtree(imTopology, elemId, docHome, _) => {
      if (elemId.nonEmpty) {
        val child = context.actorOf(docProps, elemId)
        self ! UpdateAddress(elemId, child)
        child ! Setup(imTopology, docHome)
      }
    }

    case InitTopology(json) => initTopology(parse(json))

    case Pass(to, msg) =>
      if (addresses.contains(to)) {
        addresses(to) ! msg
      } else { // with inf loop prevention (if neighbors mutally know each other)
        this.neighborDocuments.map(_ ! PassWithoutNeighborCall(to, msg))
      }

    case PassWithoutNeighborCall(to, msg) =>
      if (addresses.contains(to)) addresses(to) ! msg

    case UpdateAddress(id, ref) => addresses(id) = ref

    case TopologyOrder(Nil)     => sender ! TopologyOrder(order(rootId))

    case DocumentHome(url) => {
      this.documentHome = url
      val doc = HTTP.get(this.documentHome + s"/${rootId}")
      if (doc.getStatus == 200) {
        val json = parse(doc.getTextBody)
        this.rev = json._rev.as[String].get
        this.initTopology(json -- "_id" -- "_rev")
        self ! Setup
      }
    }

    case AddNeighbor(ref)                   => this.neighborDocuments += ref

    case uao @ UpdateAutocompleteOnly(json) => updater ! uao

    case iao @ InitAutocompleteOnly(otherUpdater) =>
      context.actorSelection(topology(rootId)("firstChild")) ! iao

  }

  def update(key: String, next: String, firstChild: String) = {
    if (key.nonEmpty)
      this.topology.update(key, Map("next" -> next, "firstChild" -> firstChild))
  }

  def persistTopology = {
    val inner =
      for (key <- this.topology.keys) yield {
        s"""
        "$key": {
          "next": "${this.topology(key)("next")}",
          "firstChild": "${this.topology(key)("firstChild")}"
        },"""
      }

    val jsonTmpl = s"""{
      ${inner.mkString("")}
      "_graveyard": [${this.graveyard.map("\"" + _ + "\"").mkString(",")}],
      "_rev": "${this.rev}"
    }"""

    if (this.documentHome.nonEmpty) {
      val url = this.documentHome + s"/${rootId}"
      val reply = HTTP.put(url, jsonTmpl.getBytes, "text/json")
      if (reply.getStatus == 201) {
        this.rev = parse(reply.getTextBody).rev.as[String].get
      }
    }
  }

  def moveChangeTopology(mover: utils.ParentPreviousHelper, onto: utils.ParentPreviousHelper) = {
    if (onto.parentFirstChild == onto.id)
      this.update(onto.parent, onto.parentNext, mover.id)
    if (mover.parentFirstChild == mover.id)
      this.update(mover.parent, mover.parentNext, mover.next)

    this.update(onto.previous, mover.id, onto.previousFirstChild)
    this.update(mover.previous, mover.next, mover.previousFirstChild)
    this.update(mover.id, onto.id, mover.firstChild)

    // send the new next and firstChild references
    // to the elements which are not poisened
    if (onto.parentFirstChild == onto.id) {
      if (onto.parent.nonEmpty)
        addresses(onto.parent) ! Next(onto.parentNext)
    }
    if (mover.parentFirstChild == mover.id) {
      if (mover.parent.nonEmpty)
        addresses(mover.parent) ! FirstChild(addresses(mover.next))
    }
    if (mover.previous.nonEmpty) {
      addresses(mover.previous) ! Next(mover.next)
      addresses(mover.previous) ! FirstChild(addresses(mover.previousFirstChild))
    }
    if (onto.previous.nonEmpty) {
      addresses(onto.previous) ! Next(mover.id)
      addresses(onto.previous) ! FirstChild(addresses(onto.previousFirstChild))
    }
  }

  def rebuildSubtreeFromDb(mover: utils.ParentPreviousHelper, onto: utils.ParentPreviousHelper) = {
    val ontoParent = context.actorSelection(onto.ref.path.parent)
    val docHome = DocumentHome(this.documentHome)
    val setFirstChild = onto.parentFirstChild == onto.id
    val isLeaf = mover.firstChild.isEmpty
    val after = if (onto.previous.nonEmpty) onto.previous else onto.parent
    val delta =
      if (topology(after)("firstChild").nonEmpty && after != rootId)
        Delta(this.order(mover.id), this.order(after).last)
      else
        Delta(this.order(mover.id), after)

    if (isLeaf)
      moving(mover.id) = (
        ontoParent,
        SetupLeaf(mover.id, onto.id, docHome, setFirstChild),
        delta)
    else // is non-leaf
      moving(mover.id) = (
        ontoParent,
        SetupSubtree(this.immutableTopology, mover.id, docHome, setFirstChild),
        delta)
  }

  def initTopology(json: Json[_]) = {
    val newTopo = json.toMap.map(kv => (kv._1, Map() ++ kv._2.toMap.map(kv => (kv._1, kv._2.as[String].get))))
    for (pair <- newTopo) topology.update(pair._1, pair._2)
  }

  def immutableTopology = topology.map(kv => (kv._1, kv._2.toMap)).toMap

  private def diggForFirstChild(id: String, stack: Stack[String]): Stack[String] = {
    if (topology.contains(id)) {
      if (topology(id)("firstChild").nonEmpty) {
        stack.push(topology(id)("firstChild"))
      }
      diggForFirstChild(topology(id)("firstChild"), stack)
    } else {
      stack
    }
  }

  private def diggForNext(id: String, queue: Queue[String]): Queue[String] = {
    if (topology.contains(id)) {
      if (topology(id)("next").nonEmpty) {
        queue.enqueue(topology(id)("next"))
      }
      diggForNext(topology(id)("next"), queue)
    } else {
      queue
    }
  }

  def diggFirstChilds(id: String) = diggForFirstChild(id, Stack[String]())
  def diggNext(id: String) = diggForNext(id, Queue[String]())

  def order(beginAt: String): List[String] = {
    val res = ListBuffer[String]()

    def digg(begin: String): Unit = {
      val fcList = diggFirstChilds(begin)
      if (fcList.size > 1)
        res += fcList(fcList.size - 1)
      while (fcList.nonEmpty) {
        val fc = fcList.pop()
        if (topology(fc)("firstChild").isEmpty)
          res += fc
        val nList = diggNext(fc)
        while (nList.nonEmpty) {
          val next = nList.dequeue()
          res += next
          digg(next)
        }
      }
    }

    res += beginAt
    digg(beginAt)

    res.toList
  }

}

object TopologyUtils { // TOOD: put all digg functions here
  def diggNext(id: String, topology: collection.Map[String, collection.Map[String, String]]) = {
    def inner(id: String, queue: Queue[String]): Queue[String] = {
      if (topology.contains(id)) {
        if (topology(id)("next").nonEmpty) {
          queue.enqueue(topology(id)("next"))
        }
        inner(topology(id)("next"), queue)
      } else {
        queue
      }
    }
    inner(id, Queue[String]())
  }
}
