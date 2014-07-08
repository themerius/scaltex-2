package scaltex.models.report

import akka.actor.ActorSelection
import akka.actor.ActorRef
import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Messages.M
import scaltex.Refs

trait Outline extends DocumentElement {
  var h1 = 1
  var h2 = 0
  var h3 = 0
  var h4 = 0

  this.state.title = "Heading"
  this.state.numbering = ""

  val to = "Section" :: "SubSection" :: "SubSubSection" :: Nil
  def outlineMsg = M(to, s"""{ "h1": $h1, "h2": $h2, "h3": $h3, "h4": $h4 } """)

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    this.state.title = actorState.contentRepr
    if (refs.nextExisting) refs.next ! outlineMsg
    super._gotUpdate(actorState, refs)
  }

  def tocIsCalling(m: M, refs: Refs): Boolean = {
    if (m.any.isInstanceOf[TOC]) {
      val tocRef = m.any.asInstanceOf[TOC].sendTo
      val shouldRespond = m.any.asInstanceOf[TOC].shouldRespond
      if (shouldRespond)
        tocRef ! M("TableOfContents" :: Nil, this.state.toString, refs.self.path.name)
      if (refs.nextExisting) refs.next ! m
      if (refs.firstChildExisting) refs.firstChild ! m
      true
    } else {
      false
    }
  }

  def nr = this.state.numbering.as[String].get
}

class Chapter extends Outline {

  this.state.numbering = s"$h1"

  def _processMsg(m: M, refs: Refs) = {
    if (!tocIsCalling(m, refs)) {
      var json = dijon.parse(m.jsonMsg)
      h1 = json.h1.as[Double].get.toInt + 1
      this.state.numbering = s"$h1"
      if (refs.nextExisting) refs.next ! outlineMsg
    }
  }

}

class Section extends Outline {

  this.state.numbering = s"$h1.$h2"

  def _processMsg(m: M, refs: Refs) = {
    if (!tocIsCalling(m, refs)) {
      var json = dijon.parse(m.jsonMsg)
      h1 = json.h1.as[Double].get.toInt
      h2 = json.h2.as[Double].get.toInt + 1
      this.state.numbering = s"$h1.$h2"
      if (refs.nextExisting) refs.next ! outlineMsg
    }
  }

}

class SubSection extends Outline {

  this.state.numbering = s"$h1.$h2.$h3"

  def _processMsg(m: M, refs: Refs) = {
    if (!tocIsCalling(m, refs)) {
      var json = dijon.parse(m.jsonMsg)
      h1 = json.h1.as[Double].get.toInt
      h2 = json.h2.as[Double].get.toInt
      h3 = json.h3.as[Double].get.toInt + 1
      this.state.numbering = s"$h1.$h2.$h3"
      if (refs.nextExisting) refs.next ! outlineMsg
    }
  }

}

class SubSubSection extends Outline {

  this.state.numbering = s"$h1.$h2.$h3.$h4"

  def _processMsg(m: M, refs: Refs) = {
    if (!tocIsCalling(m, refs)) {
      var json = dijon.parse(m.jsonMsg)
      h1 = json.h1.as[Double].get.toInt
      h2 = json.h2.as[Double].get.toInt
      h3 = json.h3.as[Double].get.toInt
      h4 = json.h4.as[Double].get.toInt + 1
      this.state.numbering = s"$h1.$h2.$h3.$h4"
      if (refs.nextExisting) refs.next ! outlineMsg
    }
  }

}