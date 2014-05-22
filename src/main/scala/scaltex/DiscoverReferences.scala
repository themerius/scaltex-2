package scaltex
import Messages._
import akka.actor.ActorSelection.toScala

trait DiscoverReferences {
  this: BaseActor =>

  def findAllActorRefs(in: String) = {
    val allIds = "id_[\\$_a-zA-Z0-9]*_id".r.findAllIn(in)
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

    val interpreterActor = context.actorSelection("/user/interpreter")  // TODO: inject from outside?
    interpreterActor ! Interpret(completeCode, "contentRepr")
  }
}
