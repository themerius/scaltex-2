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
  def makeWithIdAndNextRef[T](id: String, content: String)(implicit m: Manifest[T]): ActorRef = {
    if (system == null || updater == null)
      throw new Exception("Factory needs system and updater.")
    val numericId = id.split("entity")(1).toInt
    val actor = system.actorOf(Props(m.runtimeClass, numericId, updater), id)

    this.id = scala.math.max(this.id, numericId)
    if (lastActor != null)
      lastActor ! Msg.Next(actor)
    lastActor = actor

    actor ! Msg.Content(content)
    actor
  }
}
