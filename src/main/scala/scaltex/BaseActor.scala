package scaltex

import akka.actor.Actor
import akka.actor.ActorSelection
import com.github.pathikrit.dijon.`{}`

import Messages._

abstract class BaseActor extends Actor {

  val availableDocElems: Map[String, DocumentElement]

  var state = `{}`
  this.state.documentElement = ""
  this.state.next = ""

  def receive = {
    case Change(to) => this.state.documentElement = to
    case M(to, msg) => documentElement._processMsg(msg)
    case State      => println(documentElement.state ++ this.state)
    case Update     => documentElement._gotUpdate(next)
  }

  def assignedDocElem = this.state.documentElement.as[String].get

  def documentElement: DocumentElement = {
    if (availableDocElems.contains(assignedDocElem))
      availableDocElems(assignedDocElem)
    else
      new EmptyDocumentElement
  }

  def next: ActorSelection = context.actorSelection(this.state.next.as[String].get)

}
