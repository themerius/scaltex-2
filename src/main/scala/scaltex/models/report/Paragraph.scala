package scaltex.models.report

import akka.actor.ActorSelection

import scaltex.DocumentElement

class Paragraph extends DocumentElement {
  def _processMsg(m: String, next: ActorSelection) = println(m)
}