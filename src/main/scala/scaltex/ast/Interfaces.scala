package de.fraunhofer.scai.scaltex.ast

import akka.actor.ActorRef
import akka.actor.Actor


trait Properties {
  var varname = ""
  var content = ""
  var nextRef: ActorRef = null
  object next {
    def !(msg: Any) = if (nextRef != null) nextRef ! msg
  }
  def state: Msg.StateAnswer
}


abstract class Entity(val id: Int, val updater: ActorRef)
  extends Actor with Properties
