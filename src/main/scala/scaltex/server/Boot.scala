package scaltex.server

import scala.collection.mutable.ListBuffer

import xitrum.Server
import xitrum.Config
import xitrum.WebSocketAction
import xitrum.annotation.WEBSOCKET
import xitrum.{ WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong }

import com.m3.curly.HTTP
import scala.concurrent.future

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import com.github.pathikrit.dijon
import dijon.Json

import com.m3.curly.HTTP

import scaltex._
import scaltex.Messages._
import scaltex.models._

object Boot {

  val system = Config.actorSystem

  val interpreter = system.actorOf(Props[InterpreterActor], "interpreter")

  val updater = system.actorOf(Props[UpdaterActor], "updater")
  val props = AvailableModels.configuredActors(updater)("Report")
  val root = system.actorOf(Props(classOf[RootActor], updater, props), "root")

  val updaterMeta = system.actorOf(Props[UpdaterActor], "updater_meta")
  val propsMeta = AvailableModels.configuredActors(updaterMeta, "meta")("Report")
  val meta = system.actorOf(Props(classOf[RootActor], updaterMeta, propsMeta), "meta")

  val availableDocElems = AvailableModels.availableDocElems("Report").keys

  val bootRootTopology = """
	{
	  "root": {
	    "next": "",
	    "firstChild": "front_matter"
	  },
	  "front_matter": {
	    "next": "body_matter",
	    "firstChild": "sec_a"
	  },
	  "sec_a": {
	    "next": "par_a",
	    "firstChild": ""
	  },
	  "par_a": {
	    "next": "",
	    "firstChild": ""
	  },
	  "body_matter": {
	    "next": "back_matter",
	    "firstChild": "sec_b"
	  },
	  "sec_b": {
	    "next": "par_b",
	    "firstChild": ""
	  },
	  "par_b": {
	    "next": "sec_c",
	    "firstChild": ""
	  },
	  "sec_c": {
	    "next": "par_c",
	    "firstChild": ""
	  },
	  "par_c": {
	    "next": "",
	    "firstChild": ""
	  },
	  "back_matter": {
	    "next": "",
	    "firstChild": "sec_e"
	  },
	  "sec_e": {
	    "next": "",
	    "firstChild": ""
	  }
	}
    """

  val bootMetaTopology = """
	{
	  "meta": {
	    "next": "",
	    "firstChild": "meta_sec_a"
	  },
	  "meta_sec_a": {
	    "next": "",
	    "firstChild": ""
	  }
	}
    """

  def fillActorsWithTestdata = {
    Boot.root ! Pass("front_matter", Change("FrontMatter"))
    Boot.root ! Pass("body_matter", Change("BodyMatter"))
    Boot.root ! Pass("back_matter", Change("BackMatter"))

    Boot.root ! Pass("sec_a", Content("Introduction"))
    Boot.root ! Pass("sec_a", Change("Section"))

    Boot.root ! Pass("par_a", Content("(1) Lorem ipsum dolor. " * 5))
    Boot.root ! Pass("par_a", Change("Paragraph"))

    Boot.root ! Pass("sec_b", Content("Discovery"))
    Boot.root ! Pass("sec_b", Change("Section"))

    Boot.root ! Pass("par_b", Content("(2) Lorem ipsum dolor. " * 5))
    Boot.root ! Pass("par_b", Change("Paragraph"))

    Boot.root ! Pass("sec_c", Content("Data"))
    Boot.root ! Pass("sec_c", Change("SubSection"))

    Boot.root ! Pass("par_c", Content("(3) Lorem ipsum dolor. " * 5))
    Boot.root ! Pass("par_c", Change("Paragraph"))

    Boot.root ! Pass("sec_e", Content("Conclusion"))
    Boot.root ! Pass("sec_e", Change("Section"))

    Boot.meta ! Pass("meta_sec_a", Content("Meta Elements"))
    Boot.meta ! Pass("meta_sec_a", Change("Section"))
  }

  def main(args: Array[String]) {
    case class Config(init: Boolean = false, docHome: String = "http://127.0.0.1:5984/snapshot")

    val parser = new scopt.OptionParser[Config]("scaltex") {

      opt[Unit]("init") action { (_, cfg) =>
        cfg.copy(init = true)
      } text("the init flag will create a little example document.")

      opt[String]("home") action { (url, cfg) =>
        cfg.copy(docHome = url)
      } text("url to the couchdb where the document resides.")
    }

    val config = parser.parse(args, Config()).getOrElse(Config())

    if (config.init) {
      HTTP.delete(config.docHome)
      HTTP.put(config.docHome, "".getBytes, "")
      HTTP.put(config.docHome + "/root", bootRootTopology.getBytes, "text/json")
      HTTP.put(config.docHome + "/meta", bootMetaTopology.getBytes, "text/json")
    }

    root ! DocumentHome(config.docHome)
    root ! AddNeighbor(meta)  // so root can lookup also meta's actor refs
    meta ! DocumentHome(config.docHome)

    Server.start()

    if (config.init)
      fillActorsWithTestdata
  }

}

@WEBSOCKET("root")
class RootWebSock extends WebSocketBase {

  val root = Boot.root
  val updater = Boot.updater

  override def execute() {
    Boot.meta ! InitAutocompleteOnly(otherUpdater=Boot.updater)
    super.execute()
  }

}

@WEBSOCKET("meta")
class MetaWebSock extends WebSocketBase {

  val root = Boot.meta
  val updater = Boot.updaterMeta

  override val neighbors = Boot.root :: Nil  // TODO: attention: possible inf loop?

}
