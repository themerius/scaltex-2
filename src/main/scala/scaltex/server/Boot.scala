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
class WebSocket extends WebSocketAction {

  def execute() {
    log.debug("onOpen")

    // Updater should communicate with the websocket
    Boot.updater ! RegisterWebsocket(self)

    // Send the frontend the init topology order
    Boot.root ! TopologyOrder(Nil)

    // Send the document graph root an init Update
    Boot.root ! Update

    context.become {
      case WebSocketText(text) =>
        log.info("onTextMessage: " + text)

        val json = dijon.parse(text)
        json.function.as[String] match {
          case Some("changeContentAndDocElem") => changeContentAndDocElem(json)
          case Some("insertNext")              => insertNext(json)
          case Some("insertFirstChild")        => insertFirstChild(json)
          case Some("move")                    => move(json)
          case Some(x)                         => println("onTextMessage: not supportet function.")
          case None                            => println("onTextMessage: supplied wrong data type.")
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

      case TopologyOrder(order) =>
        val json = dijon.`{}`
        json.topologyOrder = dijon.`[]`
        for ((entry, idx) <- order.view.zipWithIndex)
          json.topologyOrder(idx) = entry
        respondWebSocketText(json.toString)

        val json2 = dijon.`{}`
        json2.availableDocElems = dijon.`[]`
        for ((key, idx) <- Boot.availableDocElems.view.zipWithIndex)
          json2.availableDocElems(idx) = key
        respondWebSocketText(json2.toString)

      case InsertDelta(newId, afterId) =>
        val json = dijon.`{}`
        json.insert = dijon.`{}`
        json.insert.newId = newId
        json.insert.afterId = afterId
        respondWebSocketText(json)
        Boot.root ! Update

      case RemoveDelta(id) =>
        val json = dijon.`{}`
        json.remove = id
        respondWebSocketText(json)

      case Delta(subtree, afterId) =>
        val json = dijon.`{}`
        json.insert = dijon.`{}`
        json.insert.afterId = afterId
        json.insert.ids = dijon.`[]`
        for ((entry, idx) <- subtree.view.zipWithIndex)
          json.insert.ids(idx) = entry
        respondWebSocketText(json)
        Boot.root ! Update
    }
  }

  def changeContentAndDocElem(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val Some(shortName) = json.params.shortName.as[String]
    Boot.root ! Pass(id, Content(content))
    Boot.root ! Pass(id, Change(documentElement))
    Boot.root ! Pass(id, ChangeName(shortName))
  }

  def insertNext(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val msgs = List(Content(content), Change(documentElement))
    val uuid = java.util.UUID.randomUUID.toString.replaceAll("-", "")
    Boot.root ! Pass(id, InsertNextRequest(uuid, msgs))
  }

  def insertFirstChild(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val msgs = List(Content(content), Change(documentElement))
    val uuid = java.util.UUID.randomUUID.toString.replaceAll("-", "")
    if (id == "root")
      Boot.root ! InsertFirstChildRequest(uuid, msgs)
    else
      Boot.root ! Pass(id, InsertFirstChildRequest(uuid, msgs))
  }

  def move(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(onto) = json.params.onto.as[String]
    Boot.root ! Pass(id, Move(onto))
  }

  override def postStop() {
    log.debug("onClose")
    Boot.updater ! DeregisterWebsocket(self)
    super.postStop()
  }
}

@WEBSOCKET("meta")
class WebSocketForMetaElems extends WebSocketAction {

  def execute() {
    log.debug("onOpen")

    // Updater should communicate with the websocket
    Boot.updaterMeta ! RegisterWebsocket(self)

    // Send the frontend the init topology order
    Boot.meta ! TopologyOrder(Nil)

    // Send the document graph root an init Update
    Boot.meta ! Update

    context.become {
      case WebSocketText(text) =>
        log.info("onTextMessage: " + text)

        val json = dijon.parse(text)
        json.function.as[String] match {
          case Some("changeContentAndDocElem") => changeContentAndDocElem(json)
          case Some("insertNext")              => insertNext(json)
          case Some("insertFirstChild")        => insertFirstChild(json)
          case Some("move")                    => move(json)
          case Some(x)                         => println("onTextMessage: not supportet function.")
          case None                            => println("onTextMessage: supplied wrong data type.")
        }

        // Send the document graph root an Update
        Boot.meta ! Update

      case WebSocketBinary(bytes) =>
        log.info("onBinaryMessage: " + bytes)
        respondWebSocketBinary(bytes)

      case WebSocketPing =>
        log.debug("onPing")

      case WebSocketPong =>
        log.debug("onPong")

      case CurrentState(json) =>
        respondWebSocketText(json)

      case TopologyOrder(order) =>
        val json = dijon.`{}`
        json.topologyOrder = dijon.`[]`
        for ((entry, idx) <- order.view.zipWithIndex)
          json.topologyOrder(idx) = entry
        respondWebSocketText(json.toString)

        val json2 = dijon.`{}`
        json2.availableDocElems = dijon.`[]`
        for ((key, idx) <- Boot.availableDocElems.view.zipWithIndex)
          json2.availableDocElems(idx) = key
        respondWebSocketText(json2.toString)

      case InsertDelta(newId, afterId) =>
        val json = dijon.`{}`
        json.insert = dijon.`{}`
        json.insert.newId = newId
        json.insert.afterId = afterId
        respondWebSocketText(json)
        Boot.meta ! Update

      case RemoveDelta(id) =>
        val json = dijon.`{}`
        json.remove = id
        respondWebSocketText(json)

      case Delta(subtree, afterId) =>
        val json = dijon.`{}`
        json.insert = dijon.`{}`
        json.insert.afterId = afterId
        json.insert.ids = dijon.`[]`
        for ((entry, idx) <- subtree.view.zipWithIndex)
          json.insert.ids(idx) = entry
        respondWebSocketText(json)
        Boot.meta ! Update
    }
  }

  def changeContentAndDocElem(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val Some(shortName) = json.params.shortName.as[String]
    Boot.meta ! Pass(id, Content(content))
    Boot.meta ! Pass(id, Change(documentElement))
    Boot.meta ! Pass(id, ChangeName(shortName))
  }

  def insertNext(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val msgs = List(Content(content), Change(documentElement))
    val uuid = java.util.UUID.randomUUID.toString.replaceAll("-", "")
    Boot.meta ! Pass(id, InsertNextRequest(uuid, msgs))
  }

  def insertFirstChild(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val msgs = List(Content(content), Change(documentElement))
    val uuid = java.util.UUID.randomUUID.toString.replaceAll("-", "")
    if (id == "meta")
      Boot.meta ! InsertFirstChildRequest(uuid, msgs)
    else
      Boot.meta ! Pass(id, InsertFirstChildRequest(uuid, msgs))
  }

  def move(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(onto) = json.params.onto.as[String]
    Boot.meta ! Pass(id, Move(onto))
  }

  override def postStop() {
    log.debug("onClose")
    Boot.updaterMeta ! DeregisterWebsocket(self)
    super.postStop()
  }
}
