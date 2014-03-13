// sbt "run-main akka.Main scai.test.DocumentAreal"

package scai.scaltex.model

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef

import org.json4s.JsonAST.JObject

object Factory {
  var updater: ActorRef = null
  var system: ActorSystem = null
  var id = 0
  var lastActor: ActorRef = null
  def makeEntityActor[T](implicit m: Manifest[T]): ActorRef = {
    if (system == null || updater == null)
      throw new Exception("Factory needs system and updater.")
    id += 1
    val actor = system.actorOf(Props(m.runtimeClass, id, updater), s"entity$id")
    if (lastActor != null)
      lastActor ! Msg.Next(actor)
    lastActor = actor
    actor
  }
}

object Msg {
  case class Varname(name: String)
  case class Content(content: String)
  case class Next(ref: ActorRef)
  case object State
  case class StateAnswer(cls: String, json: JObject, from: Int)
  case object Update

  case class SectionCount(h1: Int)
}

object Ack {
  case class Varname(from: Int)
  case class Content(from: Int)
}

trait Properties {
  var varname = ""
  var content = ""
  var next: ActorRef = null
  val regex = "entity[0-9]*".r
}

abstract class Entity(val id: Int, updater: ActorRef)
  extends Actor with Properties

class SectionActor(override val id: Int, updater: ActorRef)
  extends Entity(id: Int, updater: ActorRef) {

  var h1 = 1

  def receive = {
    case Msg.Varname(n) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c) => this.content = c; sender ! Ack.Content(id)
    case Msg.Next(n) => next = n
    case Msg.Update =>
      if (next != null) next ! Msg.SectionCount(h1)
      updater ! SectionArgs(h1, content, varname, id)
    case Msg.SectionCount(nr) =>
      h1 = nr + 1
      if (next != null) next ! Msg.SectionCount(h1)
      updater ! SectionArgs(h1, content, varname, id)
    case Msg.State => sender ! SectionArgs(h1, content, varname, id)
    case allOtherMessages => if (next != null) next ! allOtherMessages
  }
}

case class SectionArgs(nr: Int, heading: String, varname: String, from: Int)

class TextActor(override val id: Int, updater: ActorRef)
  extends Entity(id: Int, updater: ActorRef) {

  def receive = {
    case Msg.Varname(n: String) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c: String) => this.content = c; sender ! Ack.Content(id)
    case Msg.Next(n) => next = n
    case Msg.Update => if (next != null) next ! Msg.Update; this.discoverReferences
    case Msg.State => sender ! TextArgs(content, varname, id)
    case SectionArgs(nr: Int, heading: String, varname: String, from: Int) =>  // this should be universally valid or be generated
      val toBeInterpreted = s"""
        import scai.scaltex.model.SectionArgs
        val entity$from = SectionArgs($nr, "$heading", "$varname", $from)
        val content = s"$content"
        content
      """
      // damit hier mehrere unterschiedliche Variablen aufgelöst werden können,
      // müssen erstmal die actorRefs und ihre State-Antworten zwischengespeichert
      // werden, um dann in einem mal alles gesammelt zu generieren + zu interpretieren.
      // Vorteil: Das minimiert sogar die verhältnismäßig aufwendigen
      // Generierungs und Interpretierungsvorgänge
      content = Interpreter.run(toBeInterpreted, "content").getOrElse(content).toString
      updater ! TextArgs(content, this.varname, id)
    case allOtherMessages => if (next != null) next ! allOtherMessages
  }

  def discoverReferences =
    for (actorRef <- regex.findAllMatchIn(content))
      context.actorSelection(s"../$actorRef") ! Msg.State
}

case class TextArgs(text: String, varname: String, from: Int)


object Interpreter {
  import scala.tools.nsc._
  import scala.tools.nsc.interpreter._

  class Dummy

  val settings = new Settings
  settings.usejavacp.value = false
  settings.embeddedDefaults[Dummy]  // to make imain useable with sbt.

  val imain = new IMain(settings)

  def run(code: String, returnId: String) = {
    this.imain.beQuietDuring{
      this.imain.interpret(code)
    }
    val ret = this.imain.valueOfTerm(returnId)
    this.imain.reset()
    ret
  }
}