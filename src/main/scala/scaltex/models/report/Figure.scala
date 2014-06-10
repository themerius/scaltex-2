package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Figure extends DocumentElement {
  var figNr = 1

  this.state.title = "Some figure."
  this.state.numbering = s"$figNr"
  this.state.url = ""

  val to = "Figure" :: Nil
  def outlineMsg = M(to, s"""{ "figNr": $figNr } """)

  val titleRegex = "title\\(.*\\)".r
  val urlRegex = "url\\(.*\\)".r

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    val title = titleRegex.findFirstIn(repr).getOrElse("title(no title parsed)")
    val url = urlRegex.findFirstIn(repr).getOrElse("url(favicon.ico)")

    this.state.title = title.slice(6, title.size - 1)
    this.state.url = url.slice(4, url.size - 1)

    if (refs.nextExisting) refs.next ! outlineMsg

    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = {
    var json = dijon.parse(m.jsonMsg)
    figNr = json.figNr.as[Double].get.toInt + 1
    this.state.numbering = s"$figNr"
    if (refs.nextExisting) refs.next ! outlineMsg
  }

  def nr = this.state.numbering.as[String].get

}