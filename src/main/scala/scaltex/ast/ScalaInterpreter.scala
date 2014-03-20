package de.fraunhofer.scai.scaltex.ast

import scala.collection.mutable.HashMap
import scala.tools.nsc._
import scala.tools.nsc.interpreter._

import com.github.pathikrit.dijon


object Interpreter {
  class Dummy

  val settings = new Settings
  settings.usejavacp.value = false
  settings.embeddedDefaults[Dummy]  // to make imain useable with sbt.

  val imain = new IMain(settings)

  def run(code: String, returnId: String) = this.synchronized {  // maybe make an actor out of this
    this.imain.beQuietDuring {
      this.imain.interpret(code)
    }
    val ret = this.imain.valueOfTerm(returnId)
    this.imain.reset()
    ret
  }
}


trait DiscoverReferences extends IEntityActor {
  val respondedActors = new HashMap[String, Tuple2[String, String]]
  val regex = "entity[0-9]*".r

  var contentWithResolvedReferences = ""

  /*
   * build first Map, then send Messages. So that the interpretation doesn't
   * start to early
   */
  def discoverReferences = {
    contentWithResolvedReferences = content
    respondedActors.clear()  // start with empty map
    for (actorRef <- regex.findAllMatchIn(this.content))
      respondedActors += (actorRef.toString -> (null, null))
    for (actorRef <- regex.findAllMatchIn(this.content))
      this.context.actorSelection(s"../$actorRef") ! Msg.State
  }

  def receiveStateAndInformUpdater(json: String) = {
    val jsonObj = dijon.parse(json)
    val cls = jsonObj.classDef.as[String].get
    val from = jsonObj.from.as[Double].get.toInt

    respondedActors += (s"entity$from" -> (cls, json))
    val allActorsResponded =
      respondedActors.filter(_._2 == (null, null)).size == 0

    if (allActorsResponded) this.generateCode
  }

  def generateCode = {  // TODO clean code! make more generic
    var imports = Set[String]()
    for (state <- respondedActors) {
      val cls = state._2._1
      imports += s"import de.fraunhofer.scai.scaltex.ast.$cls"
    }

    var code = Set[String]()
    for (state <- respondedActors) {
      val actorRefName = state._1
      val json = "\"\"\"" + state._2._2 + "\"\"\""
      val cls = state._2._1
      code += s"""
        |val $actorRefName = new $cls()
        |$actorRefName.fromJson($json)
      """.stripMargin
    }

    val inner = "s\"\"\"" + this.content + "\"\"\""
    val ret = s"""
      |val content = $inner
      |content
    """.stripMargin

    val toBeInterpreted =
      imports.mkString("\n") + "\n\n" + code.mkString("\n") + ret

    this.contentWithResolvedReferences =
      Interpreter.run(toBeInterpreted, "content")
                 .getOrElse(this.content).toString

    this.updater ! this.state
  }
}
