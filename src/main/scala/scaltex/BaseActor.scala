package scaltex

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection

import com.github.pathikrit.dijon.`{}`

import Messages._

abstract class BaseActor(updater: ActorRef) extends Actor with DiscoverReferences {

  val root = context.actorSelection("/user/root") // TODO: inject from outside

  val availableDocElems: Map[String, DocumentElement]

  // Topology Things:
  var firstChild: ActorRef = _
  var nextId: String = ""

  // State (to be saved in DB):
  var state = `{}`
  this.state._id = self.path.name
  this.state.documentElement = ""
  this.state.contentSrc = ""
  this.state.contentRepr = ""
  this.state.contentEval = ""

  def receive = {
    case Change(to) =>
      `change current doc elem`(to)

    case Next(id) =>
      `change next ref`(id)

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
      this.nextId = topology(this.id)("next")
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

    case iwim @ InsertWithInitMsgs(newId, afterId, next, msgs) => { // TODO: caution; does only work in the leaves
      if (afterId == this.id) { // i'll get a new sibling!
        context.parent ! InsertWithInitMsgs(newId, afterId, this.nextId, msgs)  // but parent needs the next infos...
        this.nextId = newId
      } else { // i'm the parent, so create this donkey
      val newElem = context.actorOf(context.props, newId)  // den muss der parent erstellen
      root ! UpdateAddress(newId, newElem)
      root ! Insert(newId, afterId)
      newElem ! Next(next)
      for (msg <- msgs)
        newElem ! msg
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
    context.actorSelection(if (this.nextId == "") "" else "../" + this.nextId)
  }

  def `let doc elem process the msg`(m: M): Unit = {
    if (m.to.contains(assignedDocElem))
      documentElement._processMsg(m.jsonMsg, refs)
    else if (refs.nextExisting) next ! m
  }

  def `let doc elem update, trigger code gen, send curr state`: Unit = {
    val contentSrc = this.state.contentSrc.as[String].get
    val allRefs = findAllActorRefs(in = contentSrc)
    val triggered = triggerRequestForCodeGen(allRefs)
    if (!triggered) documentElement._gotUpdate(this.state, refs)
    if (!triggered) updater ! CurrentState(currentState.toString)
  }

  def `buffer code then trigger interpretation`(code: String, replyEnd: Boolean): Unit = {
    replyCodeBuffer += code
    if (replyEnd) triggerInterpreter
  }

  def `change content repr, send curr state`(repr: Any): Unit = {
    this.state.contentRepr = repr.toString
    documentElement._gotUpdate(this.state, refs)
    updater ! CurrentState(currentState.toString)
  }

  def `change content src`(content: String): Unit = {
    this.state.contentSrc = content
  }

  def `change next ref`(id: String): Unit = {
    this.nextId = id
  }

  def `change current doc elem`(to: String): Unit = {
    this.state.documentElement = to
  }

}
