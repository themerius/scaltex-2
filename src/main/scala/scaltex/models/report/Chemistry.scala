package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class IFrame extends DocumentElement {
  this.state.domElem = """<iframe
    id="ketcherFrame"
    src="lib/ketcher/ketcher.html"
    sandbox="allow-scripts allow-same-origin"
    scrolling="no"
    width="100%"
    height="600px"
    seamless></iframe>"""  // TODO: editorDomElem, viewDomElem

  val configRegex = "(?s)config\\(.*\\)".r

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    val config = configRegex.findFirstIn(repr).getOrElse("config(NONE)")
    this.state.exampleCfg = "<p>" + config.slice(7, config.size - 1) + "</p>"
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

}