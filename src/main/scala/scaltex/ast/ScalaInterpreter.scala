package de.fraunhofer.scai.scaltex.ast

import scala.collection.mutable.HashMap
import scala.tools.nsc._
import scala.tools.nsc.interpreter._


object Interpreter {
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


trait DiscoverReferences extends Entity {
  val respondedActors = new HashMap[String, Tuple2[String, String]]
  val regex = "entity[0-9]*".r

  /*
   * build first Map, then send Messages. Because that the interpretation doesn't
   * start to early
   */
  def discoverReferences = {
    for (actorRef <- regex.findAllMatchIn(this.content))
      respondedActors += (actorRef.toString -> (null, null))
    for (actorRef <- regex.findAllMatchIn(this.content))
      this.context.actorSelection(s"../$actorRef") ! Msg.State
  }

  def receiveStateAndInformUpdater(cls: String, json: String, from: Int) = {
    respondedActors += (s"entity$from" -> (cls, json))
    val allActorsResponded = respondedActors.filter(_._2 == (null, null)).size == 0
    if (allActorsResponded) this.generateCode
  }

  def generateCode = {  // TODO clean code! make more generic
    val imports = 
      for (state <- respondedActors) yield {
        val cls = state._2._1  // TODO don't do multiple imports of the same cls
        s"import de.fraunhofer.scai.scaltex.ast.$cls"
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
    val content = s"${this.content}"
    content
    """

    val toBeInterpreted = imports.mkString("\n") + code.mkString("\n") + ret

    this.content = Interpreter.run(toBeInterpreted, "content").getOrElse(this.content).toString
    this.updater ! this.state
  }
}
