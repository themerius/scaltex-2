package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import com.m3.curly.HTTP

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Spray extends DocumentElement {
  this.state.domElem = ""//dom("ketcherFrame")   // TODO: editorDomElem, viewDomElem?
  this.state.ecore = dijon.`{}`
  this.state.uuid = "fa5a62f0-1541-4551-9203-843e61ac7253"

  def dom(id: String) = raw"""
    <div id="editor-$id"></div>
    <script>
    require(["models/Spray"], function (Spray) {
      var editor = new Spray("$id", ${this.state.uuid});
      document.getElementById("editor-$id").appendChild(editor.drawEditor());
    });
    </script>"""

  val uuidRegex = "(?s)uuid\\(.*\\)".r

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    println("HIER REPR", repr)
    val uuidR = uuidRegex.findFirstIn(repr).getOrElse("uuid(fa5a62f0-1541-4551-9203-843e61ac7253)")
    println("HIER UUIDR", uuidR)
    val uuid = uuidR.slice(5, uuidR.size - 1)
    println("HIER", uuid)
    this.state.uuid = uuid
    val reply = HTTP.get("http://141.37.31.44:9000/ecoredata/" + uuid)
    if (reply.getStatus == 200) {
      var json = dijon.parse(reply.getTextBody)
      this.state.ecore = json
    }
    this.state.domElem = dom(refs.self.path.name)
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

  def allPlaces = this.state.ecore.place

}