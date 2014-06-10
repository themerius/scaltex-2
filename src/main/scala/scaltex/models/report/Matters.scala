package scaltex.models.report

import akka.actor.ActorSelection

import scaltex.ContainerElement
import scaltex.Refs
import scaltex.Messages.M

class FrontMatter extends ContainerElement {
  def _processMsg(m: M, refs: Refs) = println(m)
}

class BodyMatter extends ContainerElement {
  def _processMsg(m: M, refs: Refs) = println(m)
}

class BackMatter extends ContainerElement {
  def _processMsg(m: M, refs: Refs) = println(m)
}
