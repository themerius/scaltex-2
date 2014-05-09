package scaltex.models

import scala.util.Random
import scala.language.postfixOps
import scala.concurrent.duration._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers

import com.typesafe.config.ConfigFactory

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestActorRef

import com.github.pathikrit.dijon.JsonStringContext

import scaltex.Messages._

class ReportSpec
    extends TestKit(ActorSystem("ReportSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  val props = AvailableModels.configuredActors("Report")
  val node1 = system.actorOf(props)

  override def afterAll {
    system.shutdown()
  }

  "A Report" should {
    "be able to change it's current assigned document element" in {
      val ref = TestActorRef[AvailableModels.Report]
      val actor = ref.underlyingActor
      actor.assignedDocElem should be("")
      ref ! Change(to = "Paragraph")
      actor.assignedDocElem should be("Paragraph")
      ref ! Change(to = "Section")
      actor.assignedDocElem should be("Section")
    }
  }

}
