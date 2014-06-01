package scaltex

import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.Actor
import akka.actor.Props

import scala.collection.mutable.Map
import scala.collection.mutable.Stack
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

import com.github.pathikrit.dijon.Json
import com.github.pathikrit.dijon.parse
import com.github.pathikrit.dijon.`{}`

import com.m3.curly.HTTP

import Messages._

class RootActor(updater: ActorRef, docProps: Props) extends Actor {

  val topology = Map[String, Map[String, String]]()
  this.update("root", "", "")

  val addresses = Map[String, ActorRef]()
  addresses("root") = self
  addresses("") = null

  var documentHome = ""
  var rev = ""

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
      println("HIER", newId, msgs)
      val newChild = context.actorOf(docProps, newId)
      msgs.map(msg => newChild ! msg)
      newChild ! DocumentHome(this.documentHome)
      self ! InsertFirstChild(newChild, at = self)
    }

    case Move(ontoId) => { // TODO: refactor this shit
      val elem = sender
      val elemId = sender.path.name

      if (addresses.contains(ontoId)) {
        val ontoElem = addresses(ontoId)
        // fill the gap
        val elemPrev = topology.filter(_._2("next") == elemId)
        val `elem.previous` = if (elemPrev.nonEmpty) elemPrev.keys.head else ""
        val `elem.previous.firstChild` = topology.getOrElse(`elem.previous`, Map("firstChild" -> ""))("firstChild")

        val `elem.next` = topology(elemId)("next")
        val `elem.firstChild` = topology(elemId)("firstChild")

        val ontoPrev = topology.filter(_._2("next") == ontoId)
        val `ontoElem.previous` = if (ontoPrev.nonEmpty) ontoPrev.keys.head else ""
        val `ontoElem.previous.firstChild` = topology.getOrElse(`ontoElem.previous`, Map("firstChild" -> ""))("firstChild")

        val `ontoElem.parent` = ontoElem.path.parent.name
        val `ontoElem.parent.firstChild` = topology.getOrElse(`ontoElem.parent`, Map("firstChild" -> ""))("firstChild")
        val `ontoElem.parent.next` = topology.getOrElse(`ontoElem.parent`, Map("next" -> ""))("next")

        val `elem.parent` = elem.path.parent.name
        val `elem.parent.firstChild` = topology.getOrElse(`elem.parent`, Map("firstChild" -> ""))("firstChild")
        val `elem.parent.next` = topology.getOrElse(`elem.parent`, Map("next" -> ""))("next")

        if (elemId != `ontoElem.previous`) {
          // change the topology
          if (`ontoElem.parent.firstChild` == ontoId)
            this.update(`ontoElem.parent`, `ontoElem.parent.next`, elemId)
          if (`elem.parent.firstChild` == elemId)
            this.update(`elem.parent`, `elem.parent.next`, `elem.next`)

          this.update(`ontoElem.previous`, elemId, `ontoElem.previous.firstChild`)
          this.update(`elem.previous`, `elem.next`, `elem.previous.firstChild`)
          this.update(elemId, ontoId, `elem.firstChild`)

          // send the new next and firstChild references, to the elements
          // which are not poisened
          if (`ontoElem.parent.firstChild` == ontoId) {
            if (`ontoElem.parent`.nonEmpty)
              addresses(`ontoElem.parent`) ! Next(`ontoElem.parent.next`)
          }
          if (`elem.parent.firstChild` == elemId) {
            if (`elem.parent`.nonEmpty)
              addresses(`elem.parent`) ! FirstChild(addresses(`elem.next`))
          }
          if (`elem.previous`.nonEmpty) {
            addresses(`elem.previous`) ! Next(`elem.next`)
            addresses(`elem.previous`) ! FirstChild(addresses(`elem.previous.firstChild`))
          }
          if (`ontoElem.previous`.nonEmpty) {
            addresses(`ontoElem.previous`) ! Next(elemId)
            addresses(`ontoElem.previous`) ! FirstChild(addresses(`ontoElem.previous.firstChild`))
          }

          // then update the addresses here?

          // kill the hierarchy of the element which is hung somewhere else
          context.watch(elem)
          elem ! akka.actor.PoisonPill

          // let the leaf or subtree build from database
          val ontoParent = context.actorSelection(ontoElem.path.parent)
          val docHome = DocumentHome(this.documentHome)
          val setFirstChild = `ontoElem.parent.firstChild` == ontoId
          val isLeaf = `elem.firstChild`.isEmpty
          val after = if (`ontoElem.previous`.nonEmpty) `ontoElem.previous` else `ontoElem.parent`
          val delta =
          if (topology(after)("firstChild").nonEmpty && after != "root")
            Delta(this.order(elemId), this.order(after).last)
          else
            Delta(this.order(elemId), after)
          if (isLeaf)
            moving(elemId) = (ontoParent, SetupLeaf(elemId, ontoId, docHome, setFirstChild), delta)
          else // is non-leaf
            moving(elemId) = (ontoParent, SetupSubtree(this.immutableTopology, elemId, docHome, setFirstChild), delta)

          this.persistTopology
        }
      } else {
        println(s"Move; the element '$ontoId' doesn't exist?")
      }
    }

    case akka.actor.Terminated(ref) => {
      // if leaf or subtree is dead,
      // let the leaf or subtree rebuild from database (at new position)
      val parent = moving(ref.path.name)._1
      val message = moving(ref.path.name)._2
      val delta = moving(ref.path.name)._3
      updater ! delta
      parent ! message
      moving.remove(ref.path.name)
      context.unwatch(ref)
    }

    case Remove(elem) => {
      val next = topology(elem)("next")
      val firstChild = topology(elem)("firstChild") // must be recursive?
      val order = this.order("root")
      val idx = order.indexOf(elem) - 1
      val prev = if (order.indices.contains(idx)) order(idx) else ""
      if (prev.nonEmpty) {
        val prevElem = topology(prev)
        this.update(prev, next, prevElem("firstChild"))
      }
      topology.remove(elem)
    }

    case Update =>
      context.actorSelection(topology("root")("firstChild")) ! Update

    case Setup => {
      val firstChildRef = topology("root")("firstChild")
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

    case InitTopology(json)     => initTopology(parse(json))

    case Pass(to, msg)          => if (addresses.contains(to)) addresses(to) ! msg

    case UpdateAddress(id, ref) => addresses(id) = ref

    case TopologyOrder(Nil)     => sender ! TopologyOrder(order("root"))

    case DocumentHome(url) => {
      this.documentHome = url
      val doc = HTTP.get(this.documentHome + "/root")
      if (doc.getStatus == 200) {
        val json = parse(doc.getTextBody)
        this.rev = json._rev.as[String].get
        this.initTopology(json -- "_id" -- "_rev")
        self ! Setup
      }
    }

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
      "_rev": "${this.rev}"
    }"""

    if (this.documentHome.nonEmpty) {
      val url = this.documentHome + "/root"
      val reply = HTTP.put(url, jsonTmpl.getBytes, "text/json")
      if (reply.getStatus == 201) {
        this.rev = parse(reply.getTextBody).rev.as[String].get
      }
    }
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
