package quickstart

import scala.collection.mutable.ListBuffer

import xitrum.Server
import xitrum.Config
import xitrum.WebSocketAction
import xitrum.annotation.WEBSOCKET
import xitrum.{WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong}

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import com.github.pathikrit.dijon

import de.fraunhofer.scai.scaltex.ast._

// Messages for registring Websockets
case class Register(ref: ActorRef)
case class Deregister(ref: ActorRef)

// Start the Webserver
object Boot {

  class UpdaterActor extends Actor {
    var websockets = Set[ActorRef]()

    def receive = {
      case Register(ref) => websockets += ref
      case Deregister(ref) => websockets -= ref
      case x => if (websockets.size > 0) websockets.map(_ ! x)
    }
  }

  def prepareActors {
    Factory.system = Config.actorSystem
    Factory.updater = Config.actorSystem.actorOf(Props[UpdaterActor], "updater")

    Factory.makeEntityActor[EntityActor] ! Msg.Content("Introduction")
    Factory.makeEntityActor[EntityActor] ! Msg.Content(
      "The heading is ${entity1.heading}!")
    Factory.makeEntityActor[EntityActor] ! Msg.Content("Experiment")
    Factory.makeEntityActor[EntityActor] ! Msg.Content(
      """url = "http://upload.wikimedia.org/wikipedia/commons/a/a1/""" +
      """Koffein_-_Caffeine.svg",\ndesc = "Strukturformel von Koffein." """)
    Factory.makeEntityActor[EntityActor] ! Msg.Content("Summary")
  }

  def main(args: Array[String]) {
    prepareActors
    Server.start()
  }

}


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

        val json = dijon.parse(text)
        json.function.as[String].get match {
          case "createEntityAndAppend" =>
            this.createEntityAndAppend(json.params.cls.as[String].get, json.params.content.as[String].get)
          case "updateEntity" =>
            this.updateEntity(json.params.id.as[Double].get.toInt, json.params.cls.as[String].get, json.params.content.as[String].get)
          case x => log.info("Got unknown message: " + x)
        }

        // Send the document graph root an Update
        context.actorSelection("../entity1") ! Msg.Update

      case WebSocketBinary(bytes) =>
        log.info("onBinaryMessage: " + bytes)
        respondWebSocketBinary(bytes)

      case WebSocketPing =>
        log.debug("onPing")

      case WebSocketPong =>
        log.debug("onPong")

      case Msg.StateAnswer(json) =>
        respondWebSocketText(json)
    }
  }

  def createEntityAndAppend(cls: String, content: String) = cls match {
    case "Section" =>
      val newActor = Factory.makeEntityActor[EntityActor]
      newActor ! Msg.Content(content)
    case "Text" =>
      val newActor = Factory.makeEntityActor[EntityActor]
      newActor ! Msg.Content(content)
    case x => println("Unknown Actor " + x)
  }

  def updateEntity(id: Int, cls: String, content: String) = {
    val actor = context.actorSelection(s"../entity$id")
    actor ! Msg.ClassDef(cls)
    actor ! Msg.Content(content)
  }

  override def postStop() {
    log.debug("onClose")
    context.actorSelection("../updater") ! Deregister(self)
    super.postStop()
  }
}
