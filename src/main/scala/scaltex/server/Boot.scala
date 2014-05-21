package scaltex.server

import scala.collection.mutable.ListBuffer

import xitrum.Server
import xitrum.Config
import xitrum.WebSocketAction
import xitrum.annotation.WEBSOCKET
import xitrum.{WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong}

import com.m3.curly.HTTP
import scala.concurrent.future

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import com.github.pathikrit.dijon
import dijon.Json

import scaltex._
import scaltex.Messages._
import scaltex.models._


object Boot {

  val system = Config.actorSystem
  val updater = system.actorOf(Props[UpdaterActor], "updater")
  val interpreter = system.actorOf(Props[InterpreterActor], "interpreter")
  val props = AvailableModels.configuredActors(updater)("Report")

  val root = system.actorOf(Props(classOf[RootActor], updater, props), "root")

  def prepareActors {
    root ! InitTopology("""
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
    """)

    root ! Setup
  }

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
  }

  def main(args: Array[String]) {
    prepareActors
    Server.start()
  }

}


@WEBSOCKET("echo")
class WebSocket extends WebSocketAction {

  def execute() {
    log.debug("onOpen")

    Boot.fillActorsWithTestdata

    // Updater should communicate with the websocket
    Boot.updater ! RegisterWebsocket(self)

    // Send the document graph root an Update
    Boot.root ! Update

    context.become {
      case WebSocketText(text) =>
        log.info("onTextMessage: " + text)

        val json = dijon.parse(text)
        json.function.as[String] match {
          case Some("changeContentAndDocElem") => changeContentAndDocElem(json)
          case Some(x) => println("onTextMessage: not supportet function.")
          case None => println("onTextMessage: supplied wrong data type.")
        }

        // Send the document graph root an Update
        Boot.root ! Update

      case WebSocketBinary(bytes) =>
        log.info("onBinaryMessage: " + bytes)
        respondWebSocketBinary(bytes)

      case WebSocketPing =>
        log.debug("onPing")

      case WebSocketPong =>
        log.debug("onPong")

      case CurrentState(json) =>
        respondWebSocketText(json)
    }
  }

  def changeContentAndDocElem(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    Boot.root ! Pass(id, Content(content))
    Boot.root ! Pass(id, Change(documentElement))
  }

  override def postStop() {
    log.debug("onClose")
    Boot.updater ! DeregisterWebsocket(self)
    super.postStop()
  }
}
