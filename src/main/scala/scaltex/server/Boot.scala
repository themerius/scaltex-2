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
  val url = "http://127.0.0.1:5984/snapshot"

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

  // fill the db with testdata if not existing
  HTTP.put(url, "".getBytes, "")
  HTTP.put(url + "/root", bootRootTopology.getBytes, "text/json")
  HTTP.put(url + "/meta", bootMetaTopology.getBytes, "text/json")

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
    root ! DocumentHome(url)
    root ! AddNeighbor(meta)
    meta ! DocumentHome(url)
    Server.start()
    fillActorsWithTestdata
  }

}

@WEBSOCKET("root")
class RootWebSock extends WebSocketBase {

  val root = Boot.root
  val updater = Boot.updater

}

@WEBSOCKET("meta")
class MetaWebSock extends WebSocketBase {

  val root = Boot.meta
  val updater = Boot.updaterMeta

  override val neighbors = Boot.root :: Nil

}
