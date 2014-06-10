package scaltex.models.report

import akka.actor.ActorSelection

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Paragraph extends DocumentElement {
  def _processMsg(m: M, refs: Refs) = println(m)
}