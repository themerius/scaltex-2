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
  case class ChangeName(to: String)
  case class Content(content: String)

  case class UpdateStateProperty(json: String)
  case class RemoveStateProperty(key: String)
  case class CurrentState(json: String)
  case class UpdateAutocompleteOnly(json: String)

  // used for reference discovery
  case class Interpret(code: String, names: Map[String, Tuple2[String, String]])
  case class ReturnValue(is: Any, names: Map[String, Tuple2[String, String]])
  case class RequestForCodeGen(requester: ActorRef, others: List[String])
  case class ReplyForCodeGen(code: String, shortName: Tuple2[String, Tuple2[String, String]], replyEnd: Boolean)

  // Messages for registring websockets
  case class RegisterWebsocket(ref: ActorRef)
  case class DeregisterWebsocket(ref: ActorRef)

  // Topology
  case class Next(id: String)
  case class FirstChild(ref: ActorRef)

  case class InsertNext(newElem: ActorRef, after: ActorRef)
  case class InsertNextRequest(newId: String, initMsgs: List[Any])
  case class InsertNextCreateChild(request: InsertNextRequest)
  case class InsertDelta(newElem: String, after: String)
  case class InsertFirstChildRequest(newId: String, initMsgs: List[Any])
  case class InsertFirstChild(newElem: ActorRef, at: ActorRef)

  case class Delta(order: List[String], after: String)

  case class Move(onto: String)
  case class Remove(elem: String)
  case class RemoveDelta(id: String)
  case object Setup
  case class Setup(topology: Map[String, Map[String, String]], docHome: DocumentHome)
  case class SetupSubtree(topology: Map[String, Map[String, String]], child: String, docHome: DocumentHome, setFirstChild: Boolean)
  case class SetupLeaf(id: String, nextId: String, docHome: DocumentHome, setFirstChild: Boolean)
  case class InitTopology(json: String)
  case class Pass(to: String, msg: Any)
  case class PassWithoutNeighborCall(to: String, msg: Any)
  case class AddNeighbor(ref: ActorRef)
  case class UpdateAddress(id: String, ref: ActorRef)
  case class TopologyOrder(order: List[String])

  // Persistance (CouchDB)
  case class DocumentHome(url: String)
  case class ReconstructState(docHome: DocumentHome)
}