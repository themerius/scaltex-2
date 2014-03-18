package quickstart

import scala.collection.mutable.ListBuffer

import xitrum.Server
import xitrum.Config
import xitrum.util.SeriDeseri

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import com.github.pathikrit.dijon

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
        try {
          val json = dijon.parse(text)
          json.function.as[String].get match {
            case "createEntityAndAppend" =>
              this.createEntityAndAppend(json.params.cls.as[String].get, json.params.content.as[String].get)
            case "updateEntity" =>
              this.updateEntity(json.params.id.as[Double].get.toInt, json.params.content.as[String].get)
          }
        } catch {
          case e: java.lang.IllegalArgumentException =>
            context.actorSelection("../entity2") ! Msg.Content(text)
            context.actorSelection("../entity2") ! Msg.Update
        }

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

  def createEntityAndAppend(cls: String, content: String) = cls match {
    case "Section" =>
      val newActor = Factory.makeEntityActor[SectionActor]
      newActor ! Msg.Content(content)
      newActor ! Msg.State
    case "Text" =>
      val newActor = Factory.makeEntityActor[TextActor]
      newActor ! Msg.Content(content)
      newActor ! Msg.State
    case x => println("Unknown Actor " + x)
  }

  def updateEntity(id: Int, content: String) = {
    val actor = context.actorSelection(s"../entity$id")
    actor ! Msg.Content(content)
    actor ! Msg.Update
  }

  override def postStop() {
    log.debug("onClose")
    // Todo Deregister @ updater
    super.postStop()
  }
}
