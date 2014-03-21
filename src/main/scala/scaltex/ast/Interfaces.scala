package de.fraunhofer.scai.scaltex.ast

import akka.actor.ActorRef
import akka.actor.Actor

import com.github.pathikrit.dijon.`{}`


trait Properties {
  var varname = ""
  var content = ""
  var contentWithResolvedReferences = ""
  var nextRef: ActorRef = null
  object next {
    def !(msg: Any) = if (nextRef != null) nextRef ! msg
  }
  def state: Msg.StateAnswer
}


abstract class IEntityActor(val id: Int, val updater: ActorRef)
  extends Actor with Properties


trait ISection extends IEntityActor {
  var h1 = 1
  def stateSection: String = {  // TODO ?possible to overload like state[Section]
    val json = `{}`
    json.nr = h1
    json.content = content
    json.heading = contentWithResolvedReferences
    json.varname = varname
    json.from = id
    json.classDef = "Section"
    json.toString
  }
}


trait ISubSection extends ISection {
  var h2 = 0
  def stateSubSection: String = {
    val json = `{}`
    json.nr = s"$h1.$h2"
    json.h1 = h1
    json.h2 = h2
    json.content = content
    json.heading = contentWithResolvedReferences
    json.varname = varname
    json.from = id
    json.classDef = "SubSection"
    json.toString
  }
}


trait ISubSubSection extends ISubSection {
  var h3 = 0
  def stateSubSubSection: String = {
    val json = `{}`
    json.nr = s"$h1.$h2.$h3"
    json.h1 = h1
    json.h2 = h2
    json.h3 = h3
    json.content = content
    json.heading = contentWithResolvedReferences
    json.varname = varname
    json.from = id
    json.classDef = "SubSubSection"
    json.toString
  }
}


trait IText extends IEntityActor {
  def stateText: String = {
    val json = `{}`
    json.content = content
    json.text = contentWithResolvedReferences
    json.varname = varname
    json.from = id
    json.classDef = "Text"
    json.toString
  }
}


case class FigureArgs(url: String, desc: String)

trait IFigure extends IEntityActor {
  var figNr = 1
  def parse(content: String): FigureArgs = {
    if (content == "")
      return FigureArgs("", "")
    val code = s"""
      |import de.fraunhofer.scai.scaltex.ast.FigureArgs
      |val args = FigureArgs($content)
      |args""".stripMargin
    Interpreter.run(code, "args").getOrElse(FigureArgs("", ""))
      .asInstanceOf[FigureArgs]
  }
  def stateFigure: String = {
    val json = `{}`
    json.nr = figNr
    json.content = content.replace("\"", "\\\"")
    val figureArgs = this.parse(content)  // contentWithResolvedReferences: this variable is not evaluated when parse processes it
    json.url = figureArgs.url
    json.desc = figureArgs.desc
    json.varname = varname
    json.from = id
    json.classDef = "Figure"
    json.toString
  }
}
