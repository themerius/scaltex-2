package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Math extends DocumentElement {

  this.state.repr = ""

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    this.state.repr = actorState.contentRepr
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

  override def toString = this.state.repr.as[String].get

}

class Math2 extends Math