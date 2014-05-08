package quickstart

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

    def loadFromCouchDB = {
      val url = "http://127.0.0.1:5984/test_document/_all_docs"
      val jsonAllDocs = dijon.parse(HTTP.get(url).getTextBody)  // load the testset from db
      val actorAndNext = ListBuffer[Tuple2[ActorRef, String]]()
      for (row <- jsonAllDocs.rows.toSeq) {
        val id = row.id.as[String].get
        val jsonDoc = dijon.parse(HTTP.get("http://127.0.0.1:5984/test_document/" + id).getTextBody)
        val nextId = jsonDoc.next.as[String].get
        val content = jsonDoc.content.as[String].get
        val cls = jsonDoc.classDef.as[String].get
        val actor = Factory.makeWithIdAndNextRef[EntityActor](id, content)
        actor ! Msg.ClassDef(cls)
        actorAndNext += Tuple2(actor, nextId)
      }

      import scala.concurrent.duration._
      import scala.concurrent.{Future, Await}
      import scala.language.postfixOps

      for (an <- actorAndNext) {
        val actor = an._1
        val next = an._2
        if (next != "") {
          // create an ActorSelection based on the path
          val sel = Factory.system.actorSelection(s"/user/$next");
          // check if a single actor exists at the path
          val fut: Future[ActorRef] = sel.resolveOne(100 millis);
          val ref = Await.result(fut, 100 millis);

          actor ! Msg.Next(ref)
        }
      }
    }

    def loadFromCode = {
      val source = scala.io.Source.fromFile(getClass.getResource("/plot.py").toURI)
      val plotpy = source.mkString
      source.close()

      Factory.makeEntityActor[EntityActor] ! Msg.Content("Introduction")

      val n2 = Factory.makeEntityActor[EntityActor]
      n2 ! Msg.Content("The heading is ${entity1.heading}!")
      n2 ! Msg.ClassDef("Text")

      Factory.makeEntityActor[EntityActor] ! Msg.Content("Experiment")

      val n4 = Factory.makeEntityActor[EntityActor]
      n4 ! Msg.Content(
        """url = "http://upload.wikimedia.org/wikipedia/commons/a/a1/""" +
        """Koffein_-_Caffeine.svg", desc = "Strukturformel von Koffein." """)
      n4 ! Msg.ClassDef("Figure")

      val n5 = Factory.makeEntityActor[EntityActor]
      n5 ! Msg.Content("Example Python Code")
      n5 ! Msg.ClassDef("SubSection")

      val n6 = Factory.makeEntityActor[EntityActor]
      n6 ! Msg.Content(plotpy)
      n6 ! Msg.ClassDef("Text")

      Factory.makeEntityActor[EntityActor] ! Msg.Content("Summary")
    }

    try {
      loadFromCouchDB
    } catch {
      case e: java.net.ConnectException =>
        println("CouchDB not available.")
        loadFromCode
    }
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
        persistEntityStateIntoCouchDB(json)
    }
  }

  def createEntityAndAppend(cls: String, content: String) = cls match {
    case "Section" =>
      val newActor = Factory.makeEntityActor[EntityActor]
      newActor ! Msg.Content(content)
      newActor ! Msg.ClassDef(cls)
    case "Text" =>
      val newActor = Factory.makeEntityActor[EntityActor]
      newActor ! Msg.Content(content)
      newActor ! Msg.ClassDef(cls)
    case x => println("Unknown Actor " + x)
  }

  def updateEntity(id: Int, cls: String, content: String) = {
    val actor = context.actorSelection(s"../entity$id")
    actor ! Msg.ClassDef(cls)
    actor ! Msg.Content(content)
  }

  def persistEntityStateIntoCouchDB(json: String) = future {
    try {
      val jsonObj = dijon.parse(json)
      val url = "http://127.0.0.1:5984/test_document/entity" + jsonObj.from.as[Double].get.toInt
      val inDb = HTTP.get(url)
      if (inDb.getStatus == 200) {
        val jsonInDb = dijon.parse(inDb.getTextBody)
        val rev = jsonInDb._rev
        jsonObj._rev = rev
      }
      var put = HTTP.put(url, jsonObj.toString.getBytes, "text/json").getStatus
    } catch {
      case e: java.net.ConnectException => println("CouchDB not available.")
    }
  }

  override def postStop() {
    log.debug("onClose")
    context.actorSelection("../updater") ! Deregister(self)
    super.postStop()
  }
}
