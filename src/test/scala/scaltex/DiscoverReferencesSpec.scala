package scaltex

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
import akka.testkit.TestProbe
import akka.testkit.TestActorRef

import com.github.pathikrit.dijon.JsonStringContext
import com.github.pathikrit.dijon.parse

import scaltex.Messages._
import scaltex.models.AvailableModels.Report

class DiscoverReferencesSpec
    extends TestKit(ActorSystem("DiscoverReferencesSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  val updater = TestProbe()
  val interpreter = TestActorRef(new InterpreterActor, "interpreter")
  val props = models.AvailableModels.configuredActors(updater.ref)("Report")
  val root = TestActorRef(new RootActor(updater.ref, props), "root")

  val `1` = TestActorRef(new Report(updater.ref))
  val `2` = TestActorRef(new Report(updater.ref))
  val `3` = TestActorRef(new Report(updater.ref))

  root.underlyingActor.addresses(`1`.path.name) = `1`
  root.underlyingActor.addresses(`2`.path.name) = `2`
  root.underlyingActor.addresses(`3`.path.name) = `3`

  val contentFor1 = "Has no references"
  val contentFor2 = "Has one \\C reference: ${id_" + `1`.path.name + "_id.getClass.getSimpleName}!"
  val contentFor3 = "Has two references: ${id_" + `1`.path.name + "_id.getClass.getSimpleName}" +
    " and ${id_" + `2`.path.name + "_id.getClass.getSimpleName}!"

  override def beforeAll {
    `1` ! Change("Section")
    `2` ! Change("Paragraph")
    `3` ! Change("Paragraph")

    `1` ! Next(`2`.path.name)
    `2` ! Next(`3`.path.name)

    `1` ! Content(contentFor1)
    `2` ! Content(contentFor2)
    `3` ! Content(contentFor3)
  }

  override def afterAll {
    system.shutdown()
  }

  "The References Discovery" should {

    "find all ActorRefs within a Sting" in {
      `1`.underlyingActor.findAllActorRefs(contentFor1) should have size 0
      `2`.underlyingActor.findAllActorRefs(contentFor2) should be(
        List(`1`.path.name))
      `3`.underlyingActor.findAllActorRefs(contentFor3) should be(
        List(`1`.path.name, `2`.path.name))
    }

    "generate code of the classes the user may use" in {
      `1`.underlyingActor.genCode should include(
        s"""val id_${`1`.path.name}_id = new scaltex.models.report.Section""")
      `1`.underlyingActor.genCode should include(
        s"""id_${`1`.path.name}_id.state = dijon.parse(\"\"\"""")

      `2`.underlyingActor.genCode should include(
        s"""val id_${`2`.path.name}_id = new scaltex.models.report.Paragraph""")
      `2`.underlyingActor.genCode should include(
        s"""id_${`2`.path.name}_id.state = dijon.parse(\"\"\"""")
    }

    "collect the complete code and put the evaluated repr into contentRepr" in {
      `1` ! Update
      updater.expectMsgPF() {
        case CurrentState(json) =>
          val state = parse(json)
          state._id should be(`1`.path.name)
          state.contentRepr should be("Has no references")
      }
      updater.expectMsgPF() {
        case CurrentState(json) =>
          val state = parse(json)
          state._id should be(`2`.path.name)
          state.contentRepr should be("Has one \\C reference: Section!")
      }
      updater.expectMsgPF() {
        case CurrentState(json) =>
          val state = parse(json)
          state._id should be(`3`.path.name)
          state.contentRepr should be("Has two references: Section and Paragraph!")
      }
    }

  }

  "Unified content" should {

    "be an array with the expression details" in {
      `1` ! ChangeName("nameOfX")
      `2` ! Update

      updater.expectMsgPF() {
        case CurrentState(json) =>
          val state = parse(json)
          state._id should be(`2`.path.name)
          state.contentRepr should be("Has one \\C reference: Section!")
          state.contentUnified(0).str should be ("Has one \\C reference: ")
          state.contentUnified(0).result should be ("Section")
          state.contentUnified(0).expression(0) should be ("${")
          state.contentUnified(0).expression(1).uuid should be ("id_" + `1`.path.name + "_id")
          state.contentUnified(0).expression(1).shortName should be ("nameOfX")
          state.contentUnified(0).expression(1).documentElement should be ("Section")
          state.contentUnified(0).expression(2) should be (".getClass.getSimpleName}")
          state.contentUnified(0).expressionNonEmpty.as[Boolean].get should be (true)
      }
    }
  }

}
