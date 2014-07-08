package scaltex.models.report

import akka.actor.ActorSelection
import akka.actor.ActorRef

import collection.mutable.Map

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M
import scaltex.Messages.State

case class TOC(sendTo: ActorRef, shouldRespond: Boolean)
// shouldRespond is only modified by BodyMatter

class TableOfContents extends DocumentElement {
  this.state.items = dijon.`[]`
  val responses = Map[String, Tuple2[String, String]]()
  val to = "BodyMatter" :: "Chapter" :: "Section" :: "SubSection" :: "SubSubSection" :: Nil

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    responses.clear
	refs.root ! M(to, "", TOC(refs.self, false))
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = {
	val json = dijon.parse(m.jsonMsg)

	val nr = json.numbering.as[String].get
	val title = json.title.as[String].get
	val id = m.any.asInstanceOf[String]
	val tuple = (nr, title)
	responses(id) = tuple

	val sorted = responses.map( kv => kv._2).toList.sortBy(_._1)
	for ((item, idx) <- sorted.view.zipWithIndex) {
	  this.state.items(idx) = dijon.`{}`
	  this.state.items(idx).numbering = item._1
	  this.state.items(idx).title = item._2
	}

	val jsonLength = this.state.items.toSeq.length
	if (jsonLength > sorted.length) this.state.items(jsonLength - 1) = null

	refs.self ! State
  }

}