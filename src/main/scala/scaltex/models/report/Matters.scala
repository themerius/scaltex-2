package scaltex.models.report

import akka.actor.ActorSelection

import scaltex.ContainerElement
import scaltex.Refs
import scaltex.Messages.M

class FrontMatter extends ContainerElement {
  def _processMsg(m: M, refs: Refs) = println(m)
}

class BodyMatter extends ContainerElement {
  def _processMsg(m: M, refs: Refs) = {
    if (m.any.isInstanceOf[TOC]) {
      val tocMsg = m.any.asInstanceOf[TOC]
      val modifiedMsg = M(m.to, m.jsonMsg, TOC(tocMsg.sendTo, true))
      if (refs.nextExisting) refs.next ! m
      if (refs.firstChildExisting) refs.firstChild ! modifiedMsg
    } else {
      if (refs.nextExisting) refs.next ! m
      if (refs.firstChildExisting) refs.firstChild ! m
    }
  }
}

class BackMatter extends ContainerElement {
  def _processMsg(m: M, refs: Refs) = println(m)
}
