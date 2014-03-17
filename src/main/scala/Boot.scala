package quickstart

import scala.collection.mutable.ListBuffer

import xitrum.Server
import xitrum.Config
import xitrum.util.SeriDeseri

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import de.fraunhofer.scai.scaltex.ast._


object Boot {

  class ObserverActor extends Actor {
    val websocket = new ListBuffer[ActorRef]

    def receive = {
      case Register(ref: ActorRef) =>
        websocket += ref
        println("Got ActorRef")
      case x => if (websocket.size > 0) websocket.map(_ ! x)
    }
  }

  def prepareActors {
    Factory.system = Config.actorSystem
    Factory.updater = Config.actorSystem.actorOf(Props[ObserverActor], "updater")

    Factory.makeEntityActor[SectionActor] ! Msg.Content("Introduction")
    Factory.makeEntityActor[TextActor] ! Msg.Content("The heading is ${entity1.heading}!")
    Factory.makeEntityActor[SectionActor] ! Msg.Content("Experiment")
    Factory.makeEntityActor[SectionActor] ! Msg.Content("Summary")
  }

  def main(args: Array[String]) {
    prepareActors
    Server.start()
  }

}


import xitrum.WebSocketAction
import xitrum.annotation.WEBSOCKET
import xitrum.{WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong}

case class Register(ref: ActorRef)

@WEBSOCKET("echo")
class EchoWebSocketActor extends WebSocketAction {

  def execute() {
    log.debug("onOpen")

    // Updater should communicate with the websocket
    context.actorSelection("../updater") ! Register(self)

    // Send the document graph root an Update
    context.actorSelection("../entity1") ! Msg.Update

    context.become {

      case WebSocketText(text) =>
        log.info("onTextMessage: " + text)
        context.actorSelection("../entity2") ! Msg.Content(text)
        context.actorSelection("../entity2") ! Msg.Update

      case WebSocketBinary(bytes) =>
        log.info("onBinaryMessage: " + bytes)
        respondWebSocketBinary(bytes)

      case WebSocketPing =>
        log.debug("onPing")

      case WebSocketPong =>
        log.debug("onPong")

      case Msg.StateAnswer(cls, json, from) =>
        respondWebSocketText(json)

    }

  }

  override def postStop() {
    log.debug("onClose")
    // Todo Deregister @ updater
    super.postStop()
  }
}
