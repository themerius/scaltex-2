package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Footnote extends DocumentElement {

  this.state.repr = ""

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    this.state.repr = repr
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

  override def toString = "<sup>" + this.state.repr.as[String].get + "</sup>"
}