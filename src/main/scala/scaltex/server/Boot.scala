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

import scaltex._
import scaltex.Messages._
import scaltex.models._


object Boot {

  val system = Config.actorSystem
  val updater = system.actorOf(Props[UpdaterActor], "updater")
  val interpreter = system.actorOf(Props[InterpreterActor], "interpreter")
  val props = AvailableModels.configuredActors(updater)("Report")

  val root = system.actorOf(props, "root")
  root ! Change("Section")

  def prepareActors {
    val `1` = system.actorOf(props, "a")
    val `2` = system.actorOf(props, "b")
    val `3` = system.actorOf(props, "c")
    val `4` = system.actorOf(props, "d")
    val `5` = system.actorOf(props, "e")
    val `6` = system.actorOf(props, "f")

    root ! Next(`1`.path.name)

    `1` ! Change("Paragraph")
    `1` ! Next(`2`.path.name)
    `1` ! Content("The heading is ${id_root_id.nr}!")

    `2` ! Change("Paragraph")
    `2` ! Next(`3`.path.name)
    `2` ! Content("1Lorem ipsum dolor sit amet, consetetur sadipscing elitr. " * 5)

    `3` ! Change("SubSection")
    `3` ! Next(`4`.path.name)
    `3` ! Content("Pictures")

    `4` ! Change("Paragraph")
    `4` ! Next(`5`.path.name)
    `4` ! Content("2Lorem ipsum dolor sit amet, consetetur sadipscing elitr. " * 5)

    `5` ! Change("SubSection")
    `5` ! Next(`6`.path.name)
    `5` ! Content("Write and eval python")

    `6` ! Change("Paragraph")
    `6` ! Content("3Lorem ipsum dolor sit amet, consetetur sadipscing elitr. " * 5)
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

    // Updater should communicate with the websocket
    Boot.updater ! RegisterWebsocket(self)

    // Send the document graph root an Update
    Boot.root ! Update

    context.become {
      case WebSocketText(text) =>
        log.info("onTextMessage: " + text)

        val json = dijon.parse(text)

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

  override def postStop() {
    log.debug("onClose")
    Boot.updater ! DeregisterWebsocket(self)
    super.postStop()
  }
}
