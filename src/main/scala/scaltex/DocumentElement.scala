package scaltex

import akka.actor.ActorSelection
import akka.actor.ActorRef

import com.github.pathikrit.dijon.`{}`

trait DocumentElement {

  var state = `{}`

  def _gotUpdate(next: ActorSelection, updater: ActorRef, self: ActorRef) = {
    updater ! self.path.name + " got update"
    if (next.pathString != "/")
      next ! Messages.Update
  }

  def _processMsg(m: String, next: ActorSelection)
}

class EmptyDocumentElement extends DocumentElement {
  def _processMsg(m: String, next: ActorSelection) = None
}
