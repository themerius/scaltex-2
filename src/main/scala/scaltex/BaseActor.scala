package scaltex

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection
import com.github.pathikrit.dijon.`{}`

import Messages._

abstract class BaseActor(updater: ActorRef) extends Actor {

  val availableDocElems: Map[String, DocumentElement]

  var state = `{}`
  this.state._id = self.path.name
  this.state.documentElement = ""
  this.state.next = ""

  def receive = {
    case Change(to) => this.state.documentElement = to
    case Next(id) => this.state.next = id
    case M(to, msg) => documentElement._processMsg(msg)
    case State      => println(documentElement.state ++ this.state)
    case Update     => documentElement._gotUpdate(next, updater, self)
  }

  def assignedDocElem = this.state.documentElement.as[String].get
  
  def id = this.state._id.as[String].get

  def documentElement: DocumentElement = {
    if (availableDocElems.contains(assignedDocElem))
      availableDocElems(assignedDocElem)
    else
      new EmptyDocumentElement
  }

  def next: ActorSelection = {
    val next = this.state.next.as[String].get
    context.actorSelection(if (next == "") "" else "../" + next)
  }

}
