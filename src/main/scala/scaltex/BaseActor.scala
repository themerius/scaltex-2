package scaltex

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection
import com.github.pathikrit.dijon.`{}`

import Messages._

abstract class BaseActor(updater: ActorRef) extends Actor {

  val availableDocElems: Map[String, DocumentElement]

  var state = `{}`
  this.state._id = self.path.name
  this.state.documentElement = ""
  this.state.next = ""
  this.state.previous = ""
  this.state.contentSrc = ""
  this.state.contentRepr = ""
  this.state.contentEval = ""

  def receive = {
    case Change(to)   => this.state.documentElement = to
    case Next(id)     => this.state.next = id
    case Previous(id) => this.state.previous = id
    case m @ M(to, jsonMsg) =>
      if (to.contains(assignedDocElem))
        documentElement._processMsg(jsonMsg, refs)
      else
        if (refs.nextExisting) next ! m
    case State => updater ! CurrentState(currentState.toString)
    case Update => documentElement._gotUpdate(refs)
      val triggered = triggerRequestForCodeGen(findAllActorRefs(state.contentSrc.as[String].get))
      if (!triggered) updater ! CurrentState(currentState.toString)
    case Content(content) => this.state.contentSrc = content
    case RequestForCodeGen(requester, others) =>
      requester ! ReplyForCodeGen(genCode, others.size == 0)
      if (others.size > 0) {
        val head = context.actorSelection("../" + others.head)
        head ! RequestForCodeGen(requester, others.tail)
      }
    case ReplyForCodeGen(code, replyEnd) =>
      replyCodeBuffer += code
      if (replyEnd) triggerInterpreter
    case ReturnValue(repr) => this.state.contentRepr = repr.toString
      updater ! CurrentState(currentState.toString)
  }

  def assignedDocElem = this.state.documentElement.as[String].get

  def id = this.state._id.as[String].get

  def documentElement: DocumentElement = {
    if (availableDocElems.contains(assignedDocElem))
      availableDocElems(assignedDocElem)
    else
      new EmptyDocumentElement
  }

  def next: ActorSelection = {
    val next = this.state.next.as[String].get
    context.actorSelection(if (next == "") "" else "../" + next)
  }

  def previous: ActorSelection = {
    val prev = this.state.previous.as[String].get
    context.actorSelection(if (prev == "") "" else "../" + prev)
  }

  def refs: Refs = new Refs(next, updater, self)

  def currentState = documentElement.state ++ this.state

  def findAllActorRefs(str: String) = {
    val allIds = "id_[\\$a-zA-Z0-9]*_id".r.findAllIn(str)
    val withCuttedIds = allIds.map(x => x.slice(3, x.size - 3))
    withCuttedIds.toList
  }

  def triggerRequestForCodeGen(refs: List[String]): Boolean = {
    if (refs.size > 0) {
      val firstActorRef = context.actorSelection("../" + refs.head)
      val codeGenRequest = RequestForCodeGen(self, refs.tail)
      firstActorRef ! codeGenRequest
      true
    } else {
      this.state.contentRepr = this.state.contentSrc
      false
    }
  }

  def genCode = {
    val actorRefName = "id_" + this.id + "_id"
    val code = s"""
      | val $actorRefName = new ${documentElement.getClass.getName}
      | $actorRefName.state = json\"\"\" ${documentElement.state} \"\"\"
    """.stripMargin
    code
  }

  val replyCodeBuffer = scala.collection.mutable.Buffer[String]()

  def triggerInterpreter = {
    val references = replyCodeBuffer.toSet.mkString("\n")
    replyCodeBuffer.clear

    // Note: this.state.contentSrc delivers already quotes -> "..."
    val content = "s\"\"" + this.state.contentSrc + "\"\""
    val completeCode = s"""
      | import com.github.pathikrit.dijon.JsonStringContext
      | ${references}
      | val contentRepr = ${content}
      | contentRepr
    """.stripMargin

    val interpreterActor = context.actorSelection("../interpreter")
    interpreterActor ! Interpret(completeCode, "contentRepr")
  }

}
