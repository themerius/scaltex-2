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
  this.state.previous = ""
  this.state.contentSrc = ""
  this.state.contentRepr = ""
  this.state.contentEval = ""

  def receive = {
    case Change(to)   => this.state.documentElement = to
    case Next(id)     => this.state.next = id
    case Previous(id) => this.state.previous = id
    case m @ M(to, jsonMsg) =>
      if (to.contains(assignedDocElem))
        documentElement._processMsg(jsonMsg, refs)
      else
        next ! m
    case State => updater ! CurrentState(currentState.toString)
    case Update => documentElement._gotUpdate(refs)
    case Content(content) => this.state.contentSrc = content
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

  def previous: ActorSelection = {
    val prev = this.state.previous.as[String].get
    context.actorSelection(if (prev == "") "" else "../" + prev)
  }

  def refs: Refs = new Refs(next, updater, self)
  
  def currentState = documentElement.state ++ this.state

}
