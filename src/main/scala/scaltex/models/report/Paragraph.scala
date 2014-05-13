package scaltex.models.report

import akka.actor.ActorSelection

import scaltex.DocumentElement
import scaltex.Refs

class Paragraph extends DocumentElement {
  def _processMsg(m: String, refs: Refs) = println(m)
}