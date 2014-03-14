// sbt "run-main akka.Main scai.test.DocumentAreal"

package scai.scaltex.model

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef

import com.github.pathikrit.dijon._

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
  case class StateAnswer(cls: String, json: String, from: Int)
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
    //case Msg.State => sender ! SectionArgs(h1, content, varname, id)
    case Msg.State => sender ! this.state
    case allOtherMessages => if (next != null) next ! allOtherMessages
  }

  def state = {
    val json = `{}`
    json.nr = h1
    json.heading = content
    json.varname = varname
    json.from = id
    Msg.StateAnswer("Section", json.toString, id)
  }
}

case class SectionArgs(nr: Int, heading: String, varname: String, from: Int)

class Section(var nr: Int, var heading: String) {
  def this() = this(0, "")

  var varname = ""
  var from = 0

  def fromJson(json: String) = {
    val sec = parse(json)
    nr = sec.nr.as[Double].get.toInt
    heading = sec.heading.as[String].getOrElse("")
    varname = sec.varname.as[String].getOrElse("")
    from = sec.from.as[Double].get.toInt
  }
}

class TextActor(override val id: Int, updater: ActorRef)
  extends Entity(id: Int, updater: ActorRef) {

  val respondedActors = new HashMap[String, Tuple2[String, String]]

  def receive = {
    case Msg.Varname(n: String) => this.varname = n; sender ! Ack.Varname(id)
    case Msg.Content(c: String) => this.content = c; sender ! Ack.Content(id)
    case Msg.Next(n) => next = n
    case Msg.Update => this.discoverReferences
    case Msg.State => sender ! TextArgs(content, varname, id)
    case Msg.StateAnswer(cls, json, from) =>
      respondedActors += (s"entity$from" -> (cls, json))
      val allActorsResponded = respondedActors.filter(_._2 == (null, null)).size == 0
      if (allActorsResponded) this.generateCode
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
    for (actorRef <- regex.findAllMatchIn(content)) {
      respondedActors += (actorRef.toString -> (null, null))
      context.actorSelection(s"../$actorRef") ! Msg.State
      // aus sicherheitsgründen erst die HashMap aufbauen, und dann
      // gesammelt die nachrichten raus schicken??
    }

  def generateCode = {  // TODO clean code! make more generic
    val imports = 
      for (state <- respondedActors) yield {
        val cls = state._2._1  // TODO don't do multiple imports of the same cls
        s"import scai.scaltex.model.$cls"
      }

    val code =
      for (state <- respondedActors) yield {
        val actorRefName = state._1
        val json = "\"\"\"" + state._2._2 + "\"\"\""
        val cls = state._2._1
        s"""
        val $actorRefName = new $cls()
        $actorRefName.fromJson($json)"""
      }

    val ret = s"""
    val content = s"${content}"
    content
    """

    val toBeInterpreted = imports.mkString("\n") + code.mkString("\n") + ret

    content = Interpreter.run(toBeInterpreted, "content").getOrElse(content).toString
    updater ! TextArgs(content, this.varname, id)  // make json
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