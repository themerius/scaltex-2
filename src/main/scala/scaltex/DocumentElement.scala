package scaltex

import akka.actor.ActorSelection
import akka.actor.ActorRef

import scaltex.Messages.M

import com.github.pathikrit.dijon.`{}`
import com.github.pathikrit.dijon.Json

class Refs(val next: ActorSelection, val updater: ActorRef,
           val self: ActorRef, val firstChild: ActorRef,
           val root: ActorSelection) {
  def nextExisting: Boolean = next.pathString != "/"
  def firstChildExisting: Boolean = firstChild != null
}

trait DocumentElement {

  var state = `{}`

  def _gotUpdate(actorState: Json[_], refs: Refs) = {
    if (refs.firstChildExisting) refs.firstChild ! Messages.Update
    if (refs.nextExisting) refs.next ! Messages.Update
  }

  def _processMsg(m: M, refs: Refs)
}

trait StructureElement extends DocumentElement {
  state.visible = "true"
}

trait ContainerElement extends DocumentElement {
  state.visible = "maybe"
}

trait MetaElement extends DocumentElement {
  state.visible = "false"
}

class EmptyDocumentElement extends DocumentElement {
  def _processMsg(m: M, refs: Refs) = println(m)
}
