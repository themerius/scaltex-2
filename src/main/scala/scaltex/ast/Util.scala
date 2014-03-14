package de.fraunhofer.scai.scaltex.ast

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef


object Factory {
  var updater: ActorRef = null
  var system: ActorSystem = null
  var id = 0
  var lastActor: ActorRef = null
  def makeEntityActor[T](implicit m: Manifest[T]): ActorRef = {
    if (system == null || updater == null)
      throw new Exception("Factory needs system and updater.")
    id += 1
    val actor = system.actorOf(Props(m.runtimeClass, id, updater), s"entity$id")
    if (lastActor != null)
      lastActor ! Msg.Next(actor)
    lastActor = actor
    actor
  }
}
