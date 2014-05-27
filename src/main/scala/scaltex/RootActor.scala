package scaltex

import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.Props

import scala.collection.mutable.Map
import scala.collection.mutable.Stack
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

import com.github.pathikrit.dijon.Json
import com.github.pathikrit.dijon.parse

import com.m3.curly.HTTP

import Messages._

class RootActor(updater: ActorRef, docProps: Props) extends Actor {

  val topology = Map[String, Map[String, String]]()
  this.update("root", "", "")

  val addresses = Map[String, ActorRef]()
  addresses("root") = self

  var documentHome = ""

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
        updater ! InsertDelta(newId, after=afterId)
      } else {
        val lastChildId = this.diggNext(`after.firstChild`).reverse.head
        updater ! InsertDelta(newId, after=lastChildId)
      }
    }

    case request @ InsertNextRequest(newId, msgs) => {
      //context.parent ! InsertNextCreateChild(request)
      // TODO: this only works, if parent of root can InsertNextCreateChild
      println("INSERT next to root. (Not implemented yet)")
    }

    case InsertNextCreateChild(request) => {
      val newChild = context.actorOf(docProps, request.newId)
      request.initMsgs.map(msg => newChild ! msg)
      self ! InsertNext(newChild, after=sender)
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
      updater ! InsertDelta(newChild.path.name, after=at.path.name)
    }

    case InsertFirstChildRequest(newId, msgs) => {
      println("HIER", newId, msgs)
      val newChild = context.actorOf(docProps, newId)
      msgs.map(msg => newChild ! msg)
      self ! InsertFirstChild(newChild, at=self)
    }

    case Move(ontoId) => {
      val elem = sender
      val elemId = sender.path.name
      val ontoElem = addresses(ontoId)
      // fill the gap
      val elemPrev = topology.filter( _._2("next") == elemId )
      val `elem.previous` = if (elemPrev.nonEmpty) elemPrev.keys.head else ""
      val `elem.previous.firstChild` = topology.getOrElse(`elem.previous`, Map("firstChild" -> ""))("firstChild")

      val `elem.next` = topology(elemId)("next")
      val `elem.firstChild` = topology(elemId)("firstChild")

      val ontoPrev = topology.filter( _._2("next") == ontoId )
      val `ontoElem.previous` = if (ontoPrev.nonEmpty) ontoPrev.keys.head else ""
      val `ontoElem.previous.firstChild` = topology.getOrElse(`ontoElem.previous`, Map("firstChild" -> ""))("firstChild")

      val `ontoElem.parent` = ontoElem.path.parent.name
      val `ontoElem.parent.firstChild` = topology.getOrElse(`ontoElem.parent`, Map("firstChild" -> ""))("firstChild")
      val `ontoElem.parent.next` = topology.getOrElse(`ontoElem.parent`, Map("next" -> ""))("next")

      if (`ontoElem.parent.firstChild` == ontoId)
        this.update(`ontoElem.parent`, `ontoElem.parent.next`, elemId)
      this.update(`elem.previous`, `elem.next`, `elem.previous.firstChild`)
      this.update(elemId, ontoId, `elem.firstChild`)
      this.update(`ontoElem.previous`, elemId, `ontoElem.previous.firstChild`)

      // kill the hierarchy of the element which is hung somewhere else
      elem ! akka.actor.PoisonPill
      // the killed actors should send a Delta (what is to remove) to updater
      // let the subtree build from database
      val ontoParent = context.actorSelection(ontoElem.path.parent)
      val docHome = DocumentHome(this.documentHome)
      ontoParent ! SetupSubtree(this.immutableTopology, docHome)
      // send delta where the new subtree should be planted
    }

    case Remove(elem) => {
      val next = topology(elem)("next")
      val firstChild = topology(elem)("firstChild") // must be recursive?
      val order = this.order
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

    case InitTopology(json) => initTopology(parse(json))

    case Pass(to, msg)          => if (addresses.contains(to)) addresses(to) ! msg

    case UpdateAddress(id, ref) => addresses(id) = ref

    case TopologyOrder(Nil) => sender ! TopologyOrder(order)

    case DocumentHome(url) => {
      this.documentHome = url
      val doc = HTTP.get(this.documentHome + "/root")
      if (doc.getStatus == 200) {
        val json = parse(doc.getTextBody) -- "_id" -- "_rev"
        this.initTopology(json)
        self ! Setup
      }
    }

  }

  def update(key: String, next: String, firstChild: String) = {
    if (key.nonEmpty)
      this.topology.update(key, Map("next" -> next, "firstChild" -> firstChild))
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

  def order: List[String] = {
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

    res += "root"
    digg("root")

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
