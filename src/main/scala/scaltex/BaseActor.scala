package scaltex

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection

import com.github.pathikrit.dijon.{ `{}`, `[]` }
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
  this.state.shortName = ""
  this.state.documentElement = ""
  this.state.contentSrc = ""
  this.state.contentRepr = ""
  this.state.contentEval = ""
  this.state.contentUnified = `[]`

  var rev = ""
  var stateHash = 0

  override def postStop {
    updater ! RemoveDelta(this.id)
  }

  // Messages
  def receive = {
    case Change(to) =>
      `change current doc elem`(to)

    case ChangeName(to) =>
      `change variable name`(to)

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

    case ReplyForCodeGen(code, shortName, replyEnd) =>
      `buffer code then trigger interpretation`(code, shortName, replyEnd)

    case ReturnValue(repr, names) =>
      `change content repr, send curr state`(repr, names)

    case Setup(topology, docHome) => {
      self ! ReconstructState(docHome)
      this.setupActors(topology, docHome, true, true)
    }

    // FIX: MOVE, the subtree, hasn't got any next references?
    case SetupSubtree(topology, _, docHome, setFirstChild) => {
      this.setupActors(topology, docHome, withNexts = false, setFirstChild)
    }

    case SetupLeaf(id, nextId, docHome, setFirstChild) => {
      val reconstructed = context.actorOf(context.props, id)
      reconstructed ! Next(nextId)
      reconstructed ! ReconstructState(docHome)
      if (setFirstChild)
        this.firstChild = reconstructed
      root ! UpdateAddress(id, reconstructed)
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

    case "Debug"           => updater ! this.id
    case "Next"            => sender ! this.nextId
    case "FirstChild"      => sender ! this.firstChild.path.name
  }

  def id = this.state._id.as[String].get
  def refs: Refs = new Refs(next, updater, self, firstChild) // TODO: make static object?
  def currentState = this.state ++ documentElement.state
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
    val currState = this.currentState.toString
    val rev = `{}`
    rev._rev = this.rev
    val currStateWithRev = (this.currentState ++ rev).toString

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
        json = json -- "_rev"
        for (key: String <- documentElement.state.toMap.keys)
          json = json -- key
        this.state = json
      }
    }
  }

  def setupActors(topology: Map[String, Map[String, String]], docHome: DocumentHome, withNexts: Boolean, setFirstChild: Boolean) = {
    val firstChildRef = topology(this.id)("firstChild")
    this.nextId = topology(this.id)("next")
    if (firstChildRef.nonEmpty) {
      val firstChild = context.actorOf(context.props, firstChildRef)
      if (setFirstChild)
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

  def `buffer code then trigger interpretation`(code: String, shortName: Tuple2[String, Tuple2[String, String]], replyEnd: Boolean): Unit = {
    replyCodeBuffer += code
    val uuid = shortName._1
    val name = shortName._2
    replyNameBuffer(uuid) = name
    if (replyEnd) triggerInterpreter
  }

  def `change content repr, send curr state`(repr: Any, shortNames: Map[String, Tuple2[String, String]]): Unit = {
    try {
      val reprTuple = repr.asInstanceOf[scaltex.utils.StringContext.Unify]
      this.state.contentRepr = reprTuple._1.toString
      documentElement._gotUpdate(this.state, refs)
      // TODO: refactor
      val results = reprTuple._2
      val staticParts = reprTuple._3

      val contentSrc = this.state.contentSrc.as[String].get
      val expressions = expRegex.findAllIn(contentSrc).map(_.toString).toList
      val splittedExpr = expressions.map(_.split(uuidRegex.toString)).toList
      val uuids = expressions.map(uuidRegex.findAllMatchIn(_).map(_.toString).toList)

      // equal size: expressions, splittedExpr, uuids->shortName, results
      // size + 1: static parts
      for (idx <- 0 until results.size) {
        this.state.contentUnified(idx) = `{}`
        this.state.contentUnified(idx).str = staticParts(idx)
        this.state.contentUnified(idx).result = results(idx)
        this.state.contentUnified(idx).expression = `[]`
        this.state.contentUnified(idx).expressionNonEmpty = false
        var currJsonIdx = 0
        for (jdx <- 0 until splittedExpr(idx).size) {
          this.state.contentUnified(idx).expressionNonEmpty = true
          this.state.contentUnified(idx).expression(currJsonIdx) = splittedExpr(idx)(jdx)
          if (uuids(idx).indices.contains(jdx)) {
            currJsonIdx += 1
            this.state.contentUnified(idx).expression(currJsonIdx) = `{}`
            this.state.contentUnified(idx).expression(currJsonIdx).uuid = uuids(idx)(jdx)
            this.state.contentUnified(idx).expression(currJsonIdx).shortName = shortNames(uuids(idx)(jdx))._1
            this.state.contentUnified(idx).expression(currJsonIdx).documentElement = shortNames(uuids(idx)(jdx))._2
          }
          currJsonIdx += 1
        }
      }

      this.state.contentUnified(staticParts.size - 1) = `{}`
      this.state.contentUnified(staticParts.size - 1).str = staticParts.last
    } catch {
      case e: java.lang.ClassCastException => {  // Error in Interpreter
        documentElement._gotUpdate(this.state, refs)
      }
    }

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

  def `change variable name`(to: String): Unit = {
    this.state.shortName = to
  }

}
