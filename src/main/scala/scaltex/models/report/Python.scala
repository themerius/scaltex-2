package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class Python extends DocumentElement {
  this.state.contentEval = ""

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    this.state.contentEval = (new PythonScript(repr)).run.replace("\n", "")
    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)

  def ret = this.state.contentEval.as[String].get

}

class PythonScript(val script: String) {
  import java.io.{ OutputStreamWriter, FileOutputStream }
  import scala.sys.process._

  val filename = java.security.MessageDigest.getInstance("MD5")
    .digest(script.getBytes).map("%02x".format(_)).mkString
  val filepath = filename.mkString("") + ".py"
  // TODO: cache py files

  def createFile: String = {
    val file = new OutputStreamWriter(
      new FileOutputStream(filepath), "UTF-8")
    file.append(script)
    file.close

    return filepath
  }

  def exec: String = ("python " + createFile).!!

  def rm: String = ("rm " + filepath).!!

  def run: String = {
    try {
      val out = exec
      rm
      out
    } catch {
      case e: java.lang.RuntimeException => {
        rm
        "Invalid Python Code?"
      }
    }
  }
}