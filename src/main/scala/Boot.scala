package quickstart

import xitrum.Server
import xitrum.Config
import xitrum.util.SeriDeseri

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import scai.scaltex.model._


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
    val system = Config.actorSystem
    val updater = system.actorOf(Props[ObserverActor], "updater")

    // def makeActor[T](id: Int): ActorRef =
    //   system.actorOf(Props(classOf[T], id, updater), "entity" + id)

     def makeActorSectionActor(id: Int): ActorRef =
       system.actorOf(Props(classOf[SectionActor], id, updater), "entity" + id)

     def makeActorTextActor(id: Int): ActorRef =
       system.actorOf(Props(classOf[TextActor], id, updater), "entity" + id)

    makeActorSectionActor(1) ! Msg.Content("Introduction")
    makeActorTextActor(2) ! Msg.Content("The heading is ${entity1.heading}!")
    makeActorSectionActor(3) ! Msg.Content("Conclusion")
  }

  def main(args: Array[String]) {
    prepareActors
    Server.start()
  }

}


import xitrum.Action
import xitrum.annotation.GET

@GET("hello")
class HelloAction extends Action {
  def execute() {
    respondHtml(
      <xml:group>

        <div id="entity1"></div>
        <div id="entity2"></div>
        <div id="entity3"></div>

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
                var event = JSON.parse(event.data);
                document.getElementById("entity"+event.from).innerHTML = event.content;
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
case class JS(content: String, from: Int)

@WEBSOCKET("echo")
class EchoWebSocketActor extends WebSocketAction {

  def execute() {
    log.debug("onOpen")

    context.actorSelection("../updater") ! Register(self)
    context.actorSelection("../entity1") ! Msg.State
    context.actorSelection("../entity2") ! Msg.DiscoverReferences
    context.actorSelection("../entity2") ! Msg.State
    context.actorSelection("../entity3") ! Msg.State

    context.become {

      case WebSocketText(text) =>
        log.info("onTextMessage: " + text)
        context.actorSelection("../entity2") ! Msg.Content(text)
        context.actorSelection("../entity2") ! Msg.DiscoverReferences
        context.actorSelection("../entity2") ! Msg.State

      case WebSocketBinary(bytes) =>
        log.info("onBinaryMessage: " + bytes)
        respondWebSocketBinary(bytes)

      case WebSocketPing =>
        log.debug("onPing")

      case WebSocketPong =>
        log.debug("onPong")

      case s: SectionArgs =>
        val x = JS(s"<h1>${s.nr} ${s.heading}</h1>", s.from)
        respondWebSocketText(SeriDeseri.toJson(x))

      case t: TextArgs =>
        val x = JS(s"<p>${t.text}</p>", t.from)
        respondWebSocketText(SeriDeseri.toJson(x))
    }

  }

  override def postStop() {
    log.debug("onClose")
    super.postStop()
  }
}
