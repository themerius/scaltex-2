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
  case class ClassDef(cls: String)
  case object State
  case class StateAnswer(json: String)
  case object Update

  case class SectionCount(h1: Int, h2: Int)
}


object Ack {
  case class Varname(from: Int)
  case class Content(from: Int)
}


class EntityActor(override val id: Int, updater: ActorRef)
  extends IEntityActor(id: Int, updater: ActorRef) with DiscoverReferences
  with ISection with ISubSection with IText with IFigure {

  var classDef = "Section"

  def receive = {
    case Msg.ClassDef(cls) => this.classDef = cls
    case Msg.Varname(n: String) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c: String) => this.content = c; sender ! Ack.Content(id)
    case Msg.Next(n) => nextRef = n
    case Msg.Update =>
      next ! Msg.Update
      this.update(next)
      updater ! this.state
    case Msg.State => sender ! this.state
    case Msg.SectionCount(h1_, h2_) =>
      if (classDef == "Section") {
        h1 = h1_ + 1
        next ! Msg.SectionCount(h1, h2)
        updater ! this.state
      } else if (classDef == "SubSection") {
        h1 = h1_
        h2 = h2_ + 1
        next ! Msg.SectionCount(h1_, h2)
        updater ! this.state
      } else {
        next ! Msg.SectionCount(h1_, h2_)  // note: pass msg unchanged along,
        // because case allOtherMessages doesn't apply!
      }
    case Msg.StateAnswer(json) =>
        this.receiveStateAndInformUpdater(json)
    case allOtherMessages => next ! allOtherMessages
  }

  def update(next: this.next.type) = classDef match {
    case "Section" => next ! Msg.SectionCount(h1, h2); this.discoverReferences
    case "SubSection" => next ! Msg.SectionCount(h1, h2); this.discoverReferences
    case "Text" => this.discoverReferences
    case "Figure" => this.discoverReferences
    case x => println("Unknown class definition: " + x)
  }

  def state = classDef match {
    case "Section" => Msg.StateAnswer(this.stateSection)
    case "SubSection" => Msg.StateAnswer(this.stateSubSection)
    case "Text" => Msg.StateAnswer(this.stateText)
    case "Figure" => Msg.StateAnswer(this.stateFigure)
  }

}