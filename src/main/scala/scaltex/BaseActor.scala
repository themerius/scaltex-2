package scaltex

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection

import com.github.pathikrit.dijon.`{}`
import com.github.pathikrit.dijon.parse

import com.m3.curly.HTTP

import Messages._

abstract class BaseActor(updater: ActorRef) extends Actor with DiscoverReferences {

  val root = context.actorSelection("/user/root") // TODO: inject from outside

  val availableDocElems: Map[String, DocumentElement]

  // Topology Things:
  var firstChild: ActorRef = _
  var nextId = ""
  var documentHome = ""

  // State (to be saved in DB):
  var state = `{}`
  this.state._id = self.path.name
  this.state.documentElement = ""
  this.state.contentSrc = ""
  this.state.contentRepr = ""
  this.state.contentEval = ""

  var rev = ""
  var stateHash = 0

  override def postStop {
    updater ! RemoveDelta(this.id)
  }

  // Messages
  def receive = {
    case Change(to) =>
      `change current doc elem`(to)

    case Next(id) =>
      `change next ref`(id)

    case FirstChild(ref) =>
      this.firstChild = ref

    case State => updater ! CurrentState(currentState.toString)

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

    case Setup(topology, docHome) => {
      self ! ReconstructState(docHome)
      this.setupActors(topology, docHome)
    }

    case SetupSubtree(topology, docHome) => {
      this.setupActors(topology, docHome, withNexts = false)
    }

    case SetupLeaf(id, nextId, docHome) => {
      val reconstructed = context.actorOf(context.props, id)
      reconstructed ! Next(nextId)
      reconstructed ! ReconstructState(docHome)
    }

    case request @ InsertNextRequest(newId, msgs) => {
      context.parent ! InsertNextCreateChild(request)
    }

    case InsertNextCreateChild(request) => {
      val newChild = context.actorOf(context.props, request.newId)
      request.initMsgs.map(msg => newChild ! msg)
      newChild ! DocumentHome(this.documentHome)
      root ! InsertNext(newChild, after = sender)
    }

    case InsertFirstChildRequest(newId, msgs) => {
      val newChild = context.actorOf(context.props, newId)
      msgs.map(msg => newChild ! msg)
      newChild ! DocumentHome(this.documentHome)
      root ! InsertFirstChild(newChild, at = self)
    }

    case Move(onto) => root ! Move(onto)

    case ReconstructState(docHome) => {
      this.documentHome = docHome.url
      this.reconstructState
    }

    case DocumentHome(url) => this.documentHome = url

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

  def sendCurrentState = {
    val currState = currentState.toString
    val rev = `{}`
    rev._rev = this.rev
    val currStateWithRev = (currentState ++ rev).toString

    updater ! CurrentState(currStateWithRev)

    val changed = this.stateHash != currState.hashCode
    this.stateHash = currState.hashCode
    if (this.documentHome.nonEmpty && changed) { // then fetch from db
      val url = this.documentHome + "/" + this.id
      val state = if (this.rev.nonEmpty) currStateWithRev else currState
      val reply = HTTP.put(url, state.getBytes, "text/json")
      if (reply.getStatus == 201) {
        this.rev = parse(reply.getTextBody).rev.as[String].get
      }
    }
  }

  def reconstructState = {
    if (this.documentHome.nonEmpty) { // then fetch from db
      val reply = HTTP.get(this.documentHome + "/" + this.id)
      if (reply.getStatus == 200) {
        var json = parse(reply.getTextBody)
        this.rev = json._rev.as[String].get
        json -- "_rev"
        for (key: String <- documentElement.state.toMap.keys)
          json = json -- key
        this.state = json
      }
    }
  }

  def setupActors(topology: Map[String, Map[String, String]], docHome: DocumentHome, withNexts: Boolean = true) = {
    val firstChildRef = topology(this.id)("firstChild")
    this.nextId = topology(this.id)("next")
    if (firstChildRef.nonEmpty) {
      val firstChild = context.actorOf(context.props, firstChildRef)
      this.firstChild = firstChild
      root ! UpdateAddress(firstChildRef, firstChild)
      firstChild ! Setup(topology, docHome)
      if (withNexts) {
        val nexts = TopologyUtils.diggNext(firstChildRef, topology)
        for (next <- nexts) {
          val nextActor = context.actorOf(context.props, next)
          root ! UpdateAddress(next, nextActor)
          nextActor ! Setup(topology, docHome)
        }
      }
    }
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
    if (!triggered) sendCurrentState
  }

  def `buffer code then trigger interpretation`(code: String, replyEnd: Boolean): Unit = {
    replyCodeBuffer += code
    if (replyEnd) triggerInterpreter
  }

  def `change content repr, send curr state`(repr: Any): Unit = {
    this.state.contentRepr = repr.toString
    documentElement._gotUpdate(this.state, refs)
    sendCurrentState
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
