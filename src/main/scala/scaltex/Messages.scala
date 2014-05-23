package scaltex

import akka.actor.ActorRef

object Messages {

  case object Update
  case object State
  case object Delete

  // used within document meta model algorithm
  case class M(to: Seq[String], jsonMsg: String)

  // modify actors
  case class Change(to: String)
  case class Content(content: String)

  case class UpdateStateProperty(json: String)
  case class RemoveStateProperty(key: String)
  case class CurrentState(json: String)

  // used for reference discovery
  case class Interpret(code: String, returnId: String)
  case class ReturnValue(is: Any)
  case class RequestForCodeGen(requester: ActorRef, others: List[String])
  case class ReplyForCodeGen(code: String, replyEnd: Boolean)

  // Messages for registring websockets
  case class RegisterWebsocket(ref: ActorRef)
  case class DeregisterWebsocket(ref: ActorRef)

  // Topology
  case class Next(id: String)
  case class InsertNext(newElem: ActorRef, after: ActorRef)
  case class InsertNextRequest(newId: String, afterId: String, initMsgs: List[Any])
  case class InsertNextCreateChild(request: InsertNextRequest)
  case class InsertNextDelta(newElem: String, after: String)
  case class Remove(elem: String)
  case object Setup
  case class Setup(topology: Map[String, Map[String, String]])
  case class InitTopology(json: String)
  case class Pass(to: String, msg: Any)
  case class UpdateAddress(id: String, ref: ActorRef)
  case class TopologyOrder(order: List[String])
}