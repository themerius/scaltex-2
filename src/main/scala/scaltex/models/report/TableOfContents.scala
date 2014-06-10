package scaltex.models.report

import akka.actor.ActorSelection
import akka.actor.ActorRef

import collection.mutable.ListBuffer

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M
import scaltex.Messages.State

case class TOC(sendTo: ActorRef)

class TableOfContents extends DocumentElement {
  this.state.items = dijon.`[]`
  val responses = ListBuffer[Tuple2[String, String]]()
  val to = "Section" :: "SubSection" :: "SubSubSection" :: Nil

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    responses.clear
	refs.root ! M(to, "", TOC(refs.self))
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = {
	val json = dijon.parse(m.jsonMsg)

	val nr = json.numbering.as[String].get
	val title = json.title.as[String].get
	val tuple = (nr, title)
	responses += tuple

	responses.sortBy(_._1)
	//this.state.items = dijon.`[]`  // reset it
	for ((item, idx) <- responses.view.zipWithIndex) {
	  this.state.items(idx) = dijon.`{}`
	  this.state.items(idx).numbering = item._1
	  this.state.items(idx).title = item._2
	}

	refs.self ! State
  }

}