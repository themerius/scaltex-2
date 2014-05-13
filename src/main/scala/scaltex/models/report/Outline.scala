package scaltex.models.report

import akka.actor.ActorSelection
import akka.actor.ActorRef
import com.github.pathikrit.dijon

import scaltex.DocumentElement
import scaltex.Messages.M
import scaltex.Refs

trait Outline {
  var h1 = 1
  var h2 = 0
  var h3 = 0

  val to = "Section" :: "SubSection" :: "SubSubSection" :: Nil

  def outlineMsg = M(to, s"""{ "h1": $h1, "h2": $h2, "h3": $h3 } """)
}

class Section extends DocumentElement with Outline {

  this.state.title = "Heading"
  this.state.numbering = s"$h1"

  override def _gotUpdate(refs: Refs) = {
    if (refs.nextExisting) refs.next ! outlineMsg
    super._gotUpdate(refs)
  }

  def _processMsg(m: String, refs: Refs) = {
    var json = dijon.parse(m)
    h1 = json.h1.as[Double].get.toInt + 1
    this.state.numbering = s"$h1"
    if (refs.nextExisting) refs.next ! outlineMsg
  }

}

class SubSection extends DocumentElement with Outline {

  this.state.title = "Heading"
  this.state.numbering = s"$h1.$h2"

  override def _gotUpdate(refs: Refs) = {
    if (refs.nextExisting) refs.next ! outlineMsg
    super._gotUpdate(refs)
  }

  def _processMsg(m: String, refs: Refs) = {
    var json = dijon.parse(m)
    h1 = json.h1.as[Double].get.toInt
    h2 = json.h2.as[Double].get.toInt + 1
    this.state.numbering = s"$h1.$h2"
    if (refs.nextExisting) refs.next ! outlineMsg
  }

}

class SubSubSection extends DocumentElement with Outline {

  this.state.title = "Heading"
  this.state.numbering = s"$h1.$h2.$h3"

  override def _gotUpdate(refs: Refs) = {
    if (refs.nextExisting) refs.next ! outlineMsg
    super._gotUpdate(refs)
  }

  def _processMsg(m: String, refs: Refs) = {
    var json = dijon.parse(m)
    h1 = json.h1.as[Double].get.toInt
    h2 = json.h2.as[Double].get.toInt
    h3 = json.h3.as[Double].get.toInt + 1
    this.state.numbering = s"$h1.$h2.$h3"
    if (refs.nextExisting) refs.next ! outlineMsg
  }

}