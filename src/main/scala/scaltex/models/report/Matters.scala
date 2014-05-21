package scaltex.models.report

import akka.actor.ActorSelection

import scaltex.ContainerElement
import scaltex.Refs

class FrontMatter extends ContainerElement {
  def _processMsg(m: String, refs: Refs) = println(m)
}

class BodyMatter extends ContainerElement {
  def _processMsg(m: String, refs: Refs) = println(m)
}

class BackMatter extends ContainerElement {
  def _processMsg(m: String, refs: Refs) = println(m)
}
