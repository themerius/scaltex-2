package scaltex

import Messages._
import akka.actor.ActorRef
import scala.collection.mutable.Buffer
import scala.collection.mutable.Map

trait DiscoverReferences {
  this: BaseActor =>

  val uuidRegex = "id_[\\$_a-zA-Z0-9]*_id".r
  val expRegex = """\$\{.*?\}""".r

  def findAllActorRefs(in: String) = {
    val allIds = uuidRegex.findAllIn(in)
    val withCuttedIds = allIds.map(x => x.slice(3, x.size - 3))
    withCuttedIds.toList
  }

  def triggerRequestForCodeGen(refs: List[String]): Boolean = {
    if (refs.size > 0) {
      val codeGenRequest = RequestForCodeGen(self, refs.tail)
      root ! Pass(refs.head, codeGenRequest)
      true
    } else {
      this.state.contentRepr = this.state.contentSrc
      false
    }
  }

  def `reply with code, pass request along`(requester: ActorRef, others: List[String]): Unit = {
    val shortName = this.state.shortName.as[String].get
    val uuid = "id_" + this.id + "_id"
    val docElem = this.state.documentElement.as[String].get
    requester ! ReplyForCodeGen(genCode, (uuid, (shortName, docElem)), others.size == 0)
    if (others.size > 0) {
      val codeGenRequest = RequestForCodeGen(requester, others.tail)
      root ! Pass(others.head, codeGenRequest)
    }
  }

  def genCode = {
    val actorRefName = "id_" + this.id + "_id"
    val code = s"""
      | val $actorRefName = new ${documentElement.getClass.getName}
      | $actorRefName.state = dijon.parse(\"\"\" ${documentElement.state} \"\"\")
    """.stripMargin
    code
  }

  val replyCodeBuffer = Buffer[String]()
  val replyNameBuffer = Map[String, Tuple2[String, String]]() // uuid -> shortName, documentElement

  def triggerInterpreter = {
    val references = replyCodeBuffer.toSet.mkString("\n")
    replyCodeBuffer.clear

    val names = replyNameBuffer.toMap
    replyNameBuffer.clear

    // Note: this.state.contentSrc delivers already quotes -> "..."
    val content = "unify\"\"" + this.state.contentSrc + "\"\""
    val completeCode = s"""
      | import com.github.pathikrit.dijon
      | import scaltex.utils.StringContext.Unifier
      | ${references}
      | val (contentRepr, exprResults, staticParts) = ${content}
      | (contentRepr, exprResults, staticParts)
    """.stripMargin

    val interpreterActor = context.actorSelection("/user/interpreter") // TODO: inject from outside?
    interpreterActor ! Interpret(completeCode, names)
  }
}
