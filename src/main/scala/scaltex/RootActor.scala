package scaltex

import akka.actor.ActorRef
import akka.actor.Actor

import scala.collection.mutable.Map
import scala.collection.mutable.Stack
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

import Messages._

class RootActor(updater: ActorRef) extends Actor {

  val topology = Map[String, Map[String, String]]()
  this.update("root", "", "")

  def receive = {
    case Update => ;
    case Next   => ;
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
        res += fcList(fcList.size-1)
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
