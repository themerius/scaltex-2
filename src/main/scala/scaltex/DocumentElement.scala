package scaltex

import akka.actor.ActorSelection

import com.github.pathikrit.dijon.`{}`

trait DocumentElement {
  var state = `{}`
  def _gotUpdate(next: ActorSelection) = next ! Messages.Update
  def _processMsg(m: String)
}

class EmptyDocumentElement extends DocumentElement {
  def _processMsg(m: String) = None
}
