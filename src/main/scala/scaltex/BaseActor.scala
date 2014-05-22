package scaltex

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection

import com.github.pathikrit.dijon.`{}`

import Messages._

abstract class BaseActor(updater: ActorRef) extends Actor with DiscoverReferences {

  val root = context.actorSelection("/user/root")  // TODO: inject from outside

  val availableDocElems: Map[String, DocumentElement]

  // Topology Things:
  var firstChild: ActorRef = _

  // State (to be saved in DB):
  var state = `{}`
  this.state._id = self.path.name
  this.state.documentElement = ""
  this.state.next = ""
  this.state.previous = ""
  this.state.contentSrc = ""
  this.state.contentRepr = ""
  this.state.contentEval = ""

  def receive = {
    case Change(to) =>
      `change current doc elem`(to)

    case Next(id) =>
      `change next ref`(id)

    case Previous(id) =>
      `change previous ref`(id)

    case m: M =>
      `let doc elem process the msg`(m)

    case Update =>
      `let doc elem update, trigger code gen, send curr state`

    case Content(content) =>
      `change content src`(content)

    case RequestForCodeGen(requester, others) =>
      `reply with code, pass request along`(requester, others)

    case ReplyForCodeGen(code, replyEnd) =>
      `buffer code then trigger interpretation`(code, replyEnd)

    case ReturnValue(repr) =>
      `change content repr, send curr state`(repr)

    case Setup(topology) => {
      val firstChildRef = topology(this.id)("firstChild")
      this.state.next = topology(this.id)("next")
      if (firstChildRef.nonEmpty) {
        val firstChild = context.actorOf(context.props, firstChildRef)
        this.firstChild = firstChild
        root ! UpdateAddress(firstChildRef, firstChild)
        firstChild ! Setup(topology)
        val nexts = TopologyUtils.diggNext(firstChildRef, topology)
        for (next <- nexts) {
          val nextActor = context.actorOf(context.props, next)
          root ! UpdateAddress(next, nextActor)
          nextActor ! Setup(topology)
        }
      }
    }

    case "Debug" => updater ! this.id
  }

  def id = this.state._id.as[String].get
  def refs: Refs = new Refs(next, updater, self, firstChild) // TODO: make static object?
  def currentState = documentElement.state ++ this.state
  def assignedDocElem = this.state.documentElement.as[String].get

  def documentElement: DocumentElement =
    if (availableDocElems.contains(assignedDocElem))
      availableDocElems(assignedDocElem)
    else
      new EmptyDocumentElement

  def next: ActorSelection = {
    val next = this.state.next.as[String].get
    context.actorSelection(if (next == "") "" else "../" + next)
  }

  def previous: ActorSelection = {
    val prev = this.state.previous.as[String].get
    context.actorSelection(if (prev == "") "" else "../" + prev)
  }

  private def `let doc elem process the msg`(m: M): Unit = {
    if (m.to.contains(assignedDocElem))
      documentElement._processMsg(m.jsonMsg, refs)
    else if (refs.nextExisting) next ! m
  }

  private def `let doc elem update, trigger code gen, send curr state`: Unit = {
    val contentSrc = this.state.contentSrc.as[String].get
    val allRefs = findAllActorRefs(in = contentSrc)
    val triggered = triggerRequestForCodeGen(allRefs)
    if (!triggered) documentElement._gotUpdate(this.state, refs)
    if (!triggered) updater ! CurrentState(currentState.toString)
  }

  private def `reply with code, pass request along`(requester: ActorRef, others: List[String]): Unit = {
    requester ! ReplyForCodeGen(genCode, others.size == 0)
    if (others.size > 0) {
      val head = context.actorSelection("../" + others.head)
      head ! RequestForCodeGen(requester, others.tail)
    }
  }

  private def `buffer code then trigger interpretation`(code: String, replyEnd: Boolean): Unit = {
    replyCodeBuffer += code
    if (replyEnd) triggerInterpreter
  }

  private def `change content repr, send curr state`(repr: Any): Unit = {
    this.state.contentRepr = repr.toString
    documentElement._gotUpdate(this.state, refs)
    updater ! CurrentState(currentState.toString)
  }

  private def `change content src`(content: String): Unit = {
    this.state.contentSrc = content
  }

  private def `change previous ref`(id: String): Unit = {
    this.state.previous = id
  }

  private def `change next ref`(id: String): Unit = {
    this.state.next = id
  }

  private def `change current doc elem`(to: String): Unit = {
    this.state.documentElement = to
  }

}
