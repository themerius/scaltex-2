package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon.{`[]`, `{}`}
import com.github.pathikrit.dijon.Json

import io.bibimbap.bibtex._
import scala.io.Source

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M
import scaltex.Messages.ShortName

class Reference extends DocumentElement {

  this.state.title = ""
  this.state.authors = ""
  this.state.year = ""
  this.state.key = "empty"

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    val parser = new BibTeXParser(Source.fromString(repr), errorHandler)

    val entries = parser.entries.toList
    if (entries.nonEmpty) {
      this.state.title = entries(0).title.get.toJava
      this.state.authors = entries(0).authors.map(_.toJava).mkString("; ")
      this.state.year = entries(0).year.get.toJava
      this.state.key = entries(0).key.get
      refs.self ! ShortName(entries(0).key.get)
    }

    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

  def errorHandler(s : String) : Unit = { println("Reference Error", s) }

  override def toString =
    "(" + this.state.key.as[String].get + ", " + this.state.year.as[String].get + ")"
}
