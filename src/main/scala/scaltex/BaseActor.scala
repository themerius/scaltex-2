package scaltex

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection

import com.github.pathikrit.dijon.{ `{}`, `[]` }
import com.github.pathikrit.dijon.parse

import com.m3.curly.HTTP

import Messages._

object BaseActor {
  var count = 0
  var partialUpdate = false
  def countOne = this.synchronized { count = count + 1 }
}

abstract class BaseActor(updater: ActorRef, rootId: String) extends Actor with DiscoverReferences {

  val root = context.actorSelection(s"/user/$rootId") // TODO: inject from outside

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
    case Change(to) => BaseActor.countOne;
      `change current doc elem`(to)

    case ChangeName(to) => BaseActor.countOne;
      `change variable name`(to)

    case Next(id) => BaseActor.countOne;
      `change next ref`(id)

    case ShortName(sm) => BaseActor.countOne; this.state.shortName = sm

    case FirstChild(ref) => BaseActor.countOne;
      this.firstChild = ref

    case State => BaseActor.countOne; sendCurrentState

    case m: M => BaseActor.countOne;
      `let doc elem process the msg`(m)

    case Update => BaseActor.countOne;
      `let doc elem update, trigger code gen, send curr state`

    case Content(content) => BaseActor.countOne;
      `change content src`(content)

    case UpdateStateProperty(jsn) => BaseActor.countOne;
      val json = parse(jsn)
      this.state = this.state ++ json

    case RequestForCodeGen(requester, others) => BaseActor.countOne;
      `reply with code, pass request along`(requester, others)

    case ReplyForCodeGen(code, shortName, replyEnd) => BaseActor.countOne;
      `buffer code then trigger interpretation`(code, shortName, replyEnd)

    case ReturnValue(repr, names) => BaseActor.countOne;
      `change content repr, send curr state`(repr, names)

    case Setup(topology, docHome) => { BaseActor.countOne;
      self ! ReconstructState(docHome)
      this.setupActors(topology, docHome, true, true)
    }

    // FIX: MOVE, the subtree, hasn't got any next references?
    case SetupSubtree(topology, _, docHome, setFirstChild) => { BaseActor.countOne;
      this.setupActors(topology, docHome, withNexts = false, setFirstChild)
    }

    case SetupLeaf(id, nextId, docHome, setFirstChild) => { BaseActor.countOne;
      val reconstructed = context.actorOf(context.props, id)
      reconstructed ! Next(nextId)
      reconstructed ! ReconstructState(docHome)
      if (setFirstChild)
        this.firstChild = reconstructed
      root ! UpdateAddress(id, reconstructed)
    }

    case request @ InsertNextRequest(newId, msgs) => { BaseActor.countOne;
      context.parent ! InsertNextCreateChild(request)
    }

    case InsertNextCreateChild(request) => { BaseActor.countOne;
      val newChild = context.actorOf(context.props, request.newId)
      request.initMsgs.map(msg => newChild ! msg)
      newChild ! DocumentHome(this.documentHome)
      root ! InsertNext(newChild, after = sender)
    }

    case InsertFirstChildRequest(newId, msgs) => { BaseActor.countOne;
      val newChild = context.actorOf(context.props, newId)
      msgs.map(msg => newChild ! msg)
      newChild ! DocumentHome(this.documentHome)
      root ! InsertFirstChild(newChild, at = self)
    }

    case Move(onto) =>  BaseActor.countOne; root ! Move(onto)

    case Remove => root ! Remove(this.id)

    case ReconstructState(docHome) => { BaseActor.countOne;
      this.documentHome = docHome.url
      this.reconstructState
    }

    case DocumentHome(url) =>  BaseActor.countOne; this.documentHome = url

    case iao @ InitAutocompleteOnly(otherUpdater) => BaseActor.countOne;
      otherUpdater ! UpdateAutocompleteOnly(this.currentState.toString)
      if (refs.firstChildExisting) refs.firstChild ! iao
      if (refs.nextExisting) refs.next ! iao

    case "Debug"           => updater ! this.id
    case "Next"            => sender ! this.nextId
    case "FirstChild"      => sender ! this.firstChild.path.name
  }

  def id = this.state._id.as[String].get
  def refs: Refs = new Refs(next, updater, self, firstChild, root) // TODO: make static object?
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

    val changed = this.stateHash != currState.hashCode
    this.stateHash = currState.hashCode

    if (changed && BaseActor.partialUpdate)  // send only when state has changed
      updater ! CurrentState(currStateWithRev)

    if (!BaseActor.partialUpdate)  // send state every time
      updater ! CurrentState(currStateWithRev)

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
    if (m.to.contains(assignedDocElem)) {
      documentElement._processMsg(m, refs)
    } else {
      if (refs.nextExisting) next ! m
      if (refs.firstChildExisting) firstChild ! m
    }
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
