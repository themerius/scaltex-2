package quickstart

import xitrum.Server
import xitrum.Config
import xitrum.util.SeriDeseri

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import de.fraunhofer.scai.scaltex.ast._


object Boot {

  class ObserverActor extends Actor {
    var websocket: ActorRef = null

    def receive = {
      case Register(ref: ActorRef) =>
        websocket = ref
        println("Got ActorRef")
      case x => if (websocket != null) websocket ! x
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


import xitrum.Action
import xitrum.annotation.GET

// form code: http://www.javaworld.com/article/2077176/
// xml:unparsed: http://scala-programming-language.1934581.n4.nabble.com/using-JavaScript-in-scala-xml-td1988137.html

@GET("hello")
class HelloAction extends Action {
  def execute() {
    respondHtml(
      <xml:group>

        <div id="entity1"></div>
        <div id="entity2"></div>
        <div id="entity3"></div>
        <div id="entity4"></div>

        <hr />

        <form name="myform" action="" method="GET">
        New content for entity2 <br/>
        <input type="text" name="inputbox" value=""/><p/>
        <input type="button" name="button" value="Click" onClick="updateEntity(this.form)"/>
        </form>

        <hr />

        <p>Your IP = {remoteIp}</p>
        <p>Websocket URL = {webSocketAbsUrl[EchoWebSocketActor]}</p>
        <p id="response"></p>

        <xml:unparsed>
          <script type="text/javascript">
            var socket;

            if (!window.WebSocket) {
              window.WebSocket = window.MozWebSocket;
            }

            if (window.WebSocket) {
              socket = new WebSocket("ws://localhost:8000/echo");
              socket.onmessage = function(event) {
                var json = JSON.parse(event.data);
                document.getElementById("entity"+json.from).innerHTML = event.data;
              };
              socket.onopen = function(event) { 
                document.getElementById("response").innerHTML = "Opened"; 
              };
              socket.onclose = function(event) { 
                document.getElementById("response").innerHTML = "Closed"; 
              };
            } else {
              alert("Your browser does not support Web Socket.");
            }

            var updateEntity = function (form) {
              socket.send(form.inputbox.value)
            }
          </script>
        </xml:unparsed>
      </xml:group>
    )
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
    super.postStop()
  }
}
