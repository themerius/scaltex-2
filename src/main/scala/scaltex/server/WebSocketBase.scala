package scaltex.server

import akka.actor.ActorRef

import xitrum.WebSocketAction
import xitrum.{ WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong }

import com.github.pathikrit.dijon
import com.github.pathikrit.dijon.Json

import scaltex.Messages._

abstract class WebSocketBase extends WebSocketAction {

  val root: ActorRef
  val updater: ActorRef

  val neighbors = List[ActorRef]()

  def execute() {
    log.debug("onOpen")

    // Updater should communicate with the websocket
    updater ! RegisterWebsocket(self)

    // Send the frontend the init topology order
    root ! TopologyOrder(Nil)

    // Send the document graph root an init Update
    root ! Update

    context.become {
      case WebSocketText(text) =>
        log.info("onTextMessage: " + text)

        val json = dijon.parse(text)
        json.function.as[String] match {
          case Some("changeContentAndDocElem") => changeContentAndDocElem(json)
          case Some("insertNext")              => insertNext(json)
          case Some("insertFirstChild")        => insertFirstChild(json)
          case Some("move")                    => move(json)
          case Some("remove")                  => remove(json)
          case Some(x)                         => println("onTextMessage: not supportet function.")
          case None                            => println("onTextMessage: supplied wrong data type.")
        }

        // Send the document graph root an Update
        root ! Update

      case WebSocketBinary(bytes) =>
        log.info("onBinaryMessage: " + bytes)
        respondWebSocketBinary(bytes)

      case WebSocketPing =>
        log.debug("onPing")

      case WebSocketPong =>
        log.debug("onPong")

      case CurrentState(json) =>
        respondWebSocketText(json)
        neighbors.map( _ ! UpdateAutocompleteOnly(json) )

      case UpdateAutocompleteOnly(json) =>
        val parsedJson = dijon.parse(json)
        parsedJson.updateAutocompleteOnly = true
        respondWebSocketText(parsedJson)

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
        root ! Update

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
        root ! Update
    }
  }

  def changeContentAndDocElem(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val Some(shortName) = json.params.shortName.as[String]
    root ! Pass(id, Content(content))
    root ! Pass(id, Change(documentElement))
    root ! Pass(id, ChangeName(shortName))
    neighbors.map( _ ! Update )
  }

  def insertNext(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val msgs = List(Content(content), Change(documentElement))
    val uuid = java.util.UUID.randomUUID.toString.replaceAll("-", "")
    root ! Pass(id, InsertNextRequest(uuid, msgs))
    neighbors.map( _ ! Update )
  }

  def insertFirstChild(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(content) = json.params.contentSrc.as[String]
    val Some(documentElement) = json.params.documentElement.as[String]
    val msgs = List(Content(content), Change(documentElement))
    val uuid = java.util.UUID.randomUUID.toString.replaceAll("-", "")

    if (id == root.path.name)
      root ! InsertFirstChildRequest(uuid, msgs)
    else
      root ! Pass(id, InsertFirstChildRequest(uuid, msgs))

    neighbors.map( _ ! Update )
  }

  def move(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    val Some(onto) = json.params.onto.as[String]
    root ! Pass(id, Move(onto))
  }

  def remove(json: Json[_]) = {
    val Some(id) = json.params._id.as[String]
    root ! Pass(id, Remove)
  }

  override def postStop() {
    log.debug("onClose")
    updater ! DeregisterWebsocket(self)
    super.postStop()
  }
}