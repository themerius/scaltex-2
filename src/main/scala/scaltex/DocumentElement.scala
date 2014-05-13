package scaltex

import akka.actor.ActorSelection
import akka.actor.ActorRef

import com.github.pathikrit.dijon.`{}`

class Refs(val next: ActorSelection, val updater: ActorRef,
           val self: ActorRef) {
  def nextExisting: Boolean = next.pathString != "/"
}

trait DocumentElement {

  var state = `{}`

  def _gotUpdate(refs: Refs) = {
    refs.self ! Messages.State
    if (refs.nextExisting) refs.next ! Messages.Update    
  }

  def _processMsg(m: String, refs: Refs)
}

class EmptyDocumentElement extends DocumentElement {
  def _processMsg(m: String, refs: Refs) = None
}
