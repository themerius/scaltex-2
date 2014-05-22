package scaltex

import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.Props

import scala.collection.mutable.Map
import scala.collection.mutable.Stack
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

import com.github.pathikrit.dijon.parse

import Messages._

class RootActor(updater: ActorRef, docProps: Props) extends Actor {

  val topology = Map[String, Map[String, String]]()
  this.update("root", "", "")

  val addresses = Map[String, ActorRef]()

  def receive = {
    case Insert(newElem, after) => {
      val next = topology(after)("next")
      val firstChild = topology(after)("firstChild")
      this.update(newElem, next, "")
      this.update(after, newElem, firstChild)
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
      val imTopology = topology.map(kv => (kv._1, kv._2.toMap)).toMap
      val firstChildRef = topology("root")("firstChild")
      if (firstChildRef.nonEmpty) {
        val firstChild = context.actorOf(docProps, firstChildRef)
        self ! UpdateAddress(firstChildRef, firstChild)
        firstChild ! Setup(imTopology)
        val nexts = diggNext(firstChildRef)
        for (next <- nexts) {
          val nextActor = context.actorOf(docProps, next)
          self ! UpdateAddress(next, nextActor)
          nextActor ! Setup(imTopology)
        }
      }
    }

    case InitTopology(json) => {
      val newTopo = parse(json).toMap.map(kv => (kv._1, Map() ++ kv._2.toMap.map(kv => (kv._1, kv._2.as[String].get))))
      for (pair <- newTopo) topology.update(pair._1, pair._2)
    }

    case Pass(to, msg)          => if (addresses.contains(to)) addresses(to) ! msg

    case UpdateAddress(id, ref) => addresses(id) = ref

  }

  def update(key: String, next: String, firstChild: String) = {
    this.topology.update(key, Map("next" -> next, "firstChild" -> firstChild))
  }

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
