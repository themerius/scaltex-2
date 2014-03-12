// sbt "run-main akka.Main scai.test.DocumentAreal"

package scai.scaltex.model

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef

object Msg {
  case class Varname(name: String)
  case class Content(content: String)
  case object State
  case object DiscoverReferences
}

object Ack {
  case class Varname(from: Int)
  case class Content(from: Int)
}

trait Properties {
  var varname = ""
  var content = ""
  val regex = "entity[0-9]*".r
}

abstract class EntityOrRef(val id: Int, updater: ActorRef)
  extends Actor
  with Properties

class SectionActor(override val id: Int, updater: ActorRef)
  extends EntityOrRef(id: Int, updater: ActorRef) {

  def receive = {
    case Msg.Varname(n: String) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c: String) => this.content = c; sender ! Ack.Content(id)
    case Msg.State => sender ! SectionArgs(1, content, varname, id)
  }
}

case class SectionArgs(nr: Int, heading: String, varname: String, from: Int)

class TextActor(override val id: Int, updater: ActorRef)
  extends EntityOrRef(id: Int, updater: ActorRef) {

  def receive = {
    case Msg.Varname(n: String) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c: String) => this.content = c; sender ! Ack.Content(id)
    case Msg.State => sender ! TextArgs(content, varname, id)
    case Msg.DiscoverReferences =>
      for (actorRef <- regex.findAllMatchIn(content))
        context.actorSelection(s"../$actorRef") ! Msg.State
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
  }
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