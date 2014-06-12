package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Chemistry extends DocumentElement {
  this.state.domElem = ""//dom("ketcherFrame")   // TODO: editorDomElem, viewDomElem?

  def dom(id: String) = raw"""
    <div id="editor-$id"></div>
    <script>
    require(["models/Chemistry"], function (Chemistry) {
      var editor = new Chemistry("$id");
      document.getElementById("editor-$id").appendChild(editor.drawEditor());
      var model = document.getElementById("modal-$id-matter").innerText;
      editor.setModel(model);
    });
    </script>"""

  //val configRegex = "(?s)config\\(.*\\)".r

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    //val config = configRegex.findFirstIn(repr).getOrElse("config(NONE)")
    this.state.exampleCfg = "hier ist was"//"<p>" + config.slice(7, config.size - 1) + "</p>"
    this.state.domElem = dom(refs.self.path.name)
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

}