package scaltex.models.report

import akka.actor.ActorSelection
import akka.actor.ActorRef
import com.github.pathikrit.dijon

import scaltex.DocumentElement
import scaltex.Messages.M

class Section extends DocumentElement {
  var h1 = 1
  var h2 = 0
  var h3 = 0

  this.state.title = "Heading"
  this.state.numbering = s"$h1"
    
  override def _gotUpdate(next: ActorSelection, updater: ActorRef, self: ActorRef) = {
    if (next.pathString != "/")
      next ! M("Section" :: "SubSection" :: "SubSubSection" :: Nil, s"""{ "h1": $h1, "h2": $h2, "h3": $h3 } """)
    super._gotUpdate(next, updater, self)
  }
  
  def _processMsg(m: String, next: ActorSelection) = {
    var json = dijon.parse(m)
    h1 = json.h1.as[Double].get.toInt + 1
    this.state.numbering = s"$h1"
    if (next.pathString != "/")
      next ! M("Section" :: "SubSection" :: "SubSubSection" :: Nil, s"""{ "h1": $h1, "h2": $h2, "h3": $h3 } """)
  }

}


class SubSection extends DocumentElement {
  var h1 = 1
  var h2 = 0
  var h3 = 0

  this.state.title = "Heading"
  this.state.numbering = s"$h1.$h2"
    
  override def _gotUpdate(next: ActorSelection, updater: ActorRef, self: ActorRef) = {
    if (next.pathString != "/")
      next ! M("Section" :: "SubSection" :: "SubSubSection" :: Nil, s"""{ "h1": $h1, "h2": $h2, "h3": $h3 } """)
    super._gotUpdate(next, updater, self)
  }
  
  def _processMsg(m: String, next: ActorSelection) = {
    var json = dijon.parse(m)
    h1 = json.h1.as[Double].get.toInt
    h2 = json.h2.as[Double].get.toInt + 1
    this.state.numbering = s"$h1.$h2"
    if (next.pathString != "/")
      next ! M("Section" :: "SubSection" :: "SubSubSection" :: Nil, s"""{ "h1": $h1, "h2": $h2, "h3": $h3 } """)
  }

}


class SubSubSection extends DocumentElement {
  var h1 = 1
  var h2 = 0
  var h3 = 0

  this.state.title = "Heading"
  this.state.numbering = s"$h1.$h2.$h3"
    
  override def _gotUpdate(next: ActorSelection, updater: ActorRef, self: ActorRef) = {
    if (next.pathString != "/")
      next ! M("Section" :: "SubSection" :: "SubSubSection" :: Nil, s"""{ "h1": $h1, "h2": $h2, "h3": $h3 } """)
    super._gotUpdate(next, updater, self)
  }
  
  def _processMsg(m: String, next: ActorSelection) = {
    var json = dijon.parse(m)
    h1 = json.h1.as[Double].get.toInt
    h2 = json.h2.as[Double].get.toInt
    h3 = json.h3.as[Double].get.toInt + 1
    this.state.numbering = s"$h1.$h2.$h3"
    if (next.pathString != "/")
      next ! M("Section" :: "SubSection" :: "SubSubSection" :: Nil, s"""{ "h1": $h1, "h2": $h2, "h3": $h3 } """)
  }

}