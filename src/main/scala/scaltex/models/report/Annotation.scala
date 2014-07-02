package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Annotation extends DocumentElement {

  this.state.identifier = ""
  this.state.prefLabel = ""
  this.state.source = ""
  this.state.repr = ""

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    this.state.repr = actorState.contentRepr
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

  override def toString = this.state.repr.as[String].get

  def identifier = this.state.identifier.as[String].get

  def prefLabel = this.state.prefLabel.as[String].get

  def source = this.state.source.as[String].get

}