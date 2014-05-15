package scaltex

import akka.actor.Actor
import akka.actor.ActorRef

import Messages._

class UpdaterActor extends Actor {
  var websockets = Set[ActorRef]()

  def receive = {
    case RegisterWebsocket(ref)   => websockets += ref
    case DeregisterWebsocket(ref) => websockets -= ref
    case x                        => websockets.map(_ ! x)
  }
}