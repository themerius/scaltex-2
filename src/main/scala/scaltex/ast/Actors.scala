package de.fraunhofer.scai.scaltex.ast

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef

import com.github.pathikrit.dijon._


object Msg {
  case class Varname(name: String)
  case class Content(content: String)
  case class Next(ref: ActorRef)
  case object State
  case class StateAnswer(cls: String, json: String, from: Int)
  case object Update

  case class SectionCount(h1: Int)
}


object Ack {
  case class Varname(from: Int)
  case class Content(from: Int)
}


class SectionActor(override val id: Int, updater: ActorRef)
  extends Entity(id: Int, updater: ActorRef) {

  var h1 = 1

  def receive = {
    case Msg.Varname(n) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c) => this.content = c; sender ! Ack.Content(id)
    case Msg.Next(n) => nextRef = n
    case Msg.Update =>
      next ! Msg.Update
      next ! Msg.SectionCount(h1)
      updater ! this.state
    case Msg.SectionCount(nr) =>
      h1 = nr + 1
      next ! Msg.SectionCount(h1)
      updater ! this.state
    case Msg.State => sender ! this.state
    case allOtherMessages => if (next != null) next ! allOtherMessages
  }

  def state = {
    val json = `{}`
    json.nr = h1
    json.heading = content.replace("\n", raw"\n")
    json.varname = varname
    json.from = id
    Msg.StateAnswer("Section", json.toString, id)
  }
}


class TextActor(override val id: Int, updater: ActorRef)
  extends Entity(id: Int, updater: ActorRef) with DiscoverReferences {

  //val refs = new DiscoverReferences(this, self)  // Composition instead of inheritance!

  def receive = {
    case Msg.Varname(n: String) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c: String) => this.content = c; sender ! Ack.Content(id)
    case Msg.Next(n) => nextRef = n
    case Msg.Update => 
      next ! Msg.Update
      this.discoverReferences
      updater ! this.state
    case Msg.State => sender ! this.state
    case Msg.StateAnswer(cls, json, from) =>
      this.receiveStateAndInformUpdater(cls, json, from)
    case allOtherMessages => next ! allOtherMessages
  }

  def state = {
    val json = `{}`
    json.content = contentWithResolvedReferences.replace("\n", raw"\n")
    json.contentUnresolved = content.replace("\n", raw"\n")
    json.varname = varname
    json.from = id
    Msg.StateAnswer("Text", json.toString, id)
  }

}
