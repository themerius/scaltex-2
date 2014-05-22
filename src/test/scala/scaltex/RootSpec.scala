package scaltex

import scala.util.Random
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.collection.mutable.Stack

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.GivenWhenThen

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

class RootSpec
    extends TestKit(ActorSystem("RootSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  val updater = TestProbe()
  //val interpreter = TestActorRef(new InterpreterActor, "interpreter")
  val props = models.AvailableModels.configuredActors(updater.ref)("Report")

  val root = TestActorRef(new RootActor(updater.ref, props), "root")

  def setupNonEmptyTopology = { // id, next, firstChild
    root.underlyingActor.update("root", "", "front-matter")

    root.underlyingActor.update("front-matter", "body-matter", "sec-a")
    root.underlyingActor.update("sec-a", "par-a", "")
    root.underlyingActor.update("par-a", "", "")

    root.underlyingActor.update("body-matter", "back-matter", "intro")
    root.underlyingActor.update("intro", "concl", "sec-b")
    root.underlyingActor.update("sec-b", "par-b", "")
    root.underlyingActor.update("par-b", "", "")
    root.underlyingActor.update("concl", "", "sec-c")
    root.underlyingActor.update("sec-c", "par-c", "")
    root.underlyingActor.update("par-c", "par-d", "")
    root.underlyingActor.update("par-d", "", "")

    root.underlyingActor.update("back-matter", "", "sec-e")
    root.underlyingActor.update("sec-e", "", "")
  }

  override def beforeAll {
    setupNonEmptyTopology
  }

  override def afterAll {
    system.shutdown()
  }

  "The Root Actor" should {

    "hold topological informations about the document" in {
      root.underlyingActor.topology should be(Map(
        "root" -> Map("next" -> "", "firstChild" -> "front-matter"),
        "concl" -> Map("next" -> "", "firstChild" -> "sec-c"),
        "par-b" -> Map("next" -> "", "firstChild" -> ""),
        "back-matter" -> Map("next" -> "", "firstChild" -> "sec-e"),
        "sec-a" -> Map("next" -> "par-a", "firstChild" -> ""),
        "par-a" -> Map("next" -> "", "firstChild" -> ""),
        "par-d" -> Map("next" -> "", "firstChild" -> ""),
        "body-matter" -> Map("next" -> "back-matter", "firstChild" -> "intro"),
        "sec-c" -> Map("next" -> "par-c", "firstChild" -> ""),
        "intro" -> Map("next" -> "concl", "firstChild" -> "sec-b"),
        "par-c" -> Map("next" -> "par-d", "firstChild" -> ""),
        "front-matter" -> Map("next" -> "body-matter", "firstChild" -> "sec-a"),
        "sec-b" -> Map("next" -> "par-b", "firstChild" -> ""),
        "sec-e" -> Map("next" -> "", "firstChild" -> "")))
    }

    "digg for the firstChilds" in {
      root.underlyingActor.diggFirstChilds("root") should be(
        "sec-a" :: "front-matter" :: Nil)
    }

    "digg for the next references" in {
      root.underlyingActor.diggNext("front-matter") should be(
        "body-matter" :: "back-matter" :: Nil)
    }

    "be able to claculate the correct order of the document elements" in {
      val order = root.underlyingActor.order
      order(0) should be("root")
      order(1) should be("front-matter")
      order(2) should be("sec-a")
      order(3) should be("par-a")
      order(4) should be("body-matter")
      order(5) should be("intro")
      order(6) should be("sec-b")
      order(7) should be("par-b")
      order(8) should be("concl")
      order(9) should be("sec-c")
      order(10) should be("par-c")
      order(11) should be("par-d")
      order(12) should be("back-matter")
      order(13) should be("sec-e")
    }

    "be able to insert (new) elements and remove elements from topology" in {
      val topo = root.underlyingActor.topology
      topo("sec-a")("next") should be("par-a")
      topo.contains("new-elem") should be(false)
      topo("par-a")("next") should be("")

      root ! Insert("new-elem", after = "sec-a")
      updater.expectMsg(Insert("new-elem", after = "sec-a"))

      topo("sec-a")("next") should be("new-elem")
      topo("new-elem")("next") should be("par-a")
      topo("par-a")("next") should be("")

      root ! Remove("new-elem")

      topo("sec-a")("next") should be("par-a")
      topo.contains("new-elem") should be(false)
      topo("par-a")("next") should be("")
    }

    "setup the document actors with a given topology" in {
      root ! Setup

      `actor exists?`("user/root/front-matter")
      `actor exists?`("user/root/body-matter")
      `actor exists?`("user/root/back-matter")

      `actor exists?`("user/root/front-matter/sec-a")
      `actor exists?`("user/root/front-matter/par-a")

      `actor exists?`("user/root/body-matter/intro")
      `actor exists?`("user/root/body-matter/intro/sec-b")
      `actor exists?`("user/root/body-matter/intro/par-b")

      `actor exists?`("user/root/body-matter/concl")
      `actor exists?`("user/root/body-matter/concl/sec-c")
      `actor exists?`("user/root/body-matter/concl/par-c")
      `actor exists?`("user/root/body-matter/concl/par-d")

      `actor exists?`("user/root/back-matter/sec-e")
    }

    "be able to initiate the Update through the entire document" in {
      root ! Update

      val messages = updater.receiveN(13).asInstanceOf[Seq[CurrentState]]
      val foundIds = messages.map(x => parse(x.json)._id)

      foundIds should contain("front-matter")
      foundIds should contain("sec-a")
      foundIds should contain("par-a")
      foundIds should contain("body-matter")
      foundIds should contain("intro")
      foundIds should contain("sec-b")
      foundIds should contain("par-b")
      foundIds should contain("concl")
      foundIds should contain("sec-c")
      foundIds should contain("par-c")
      foundIds should contain("par-d")
      foundIds should contain("back-matter")
      foundIds should contain("sec-e")

    }

    "pass messages to the selected id" in {
      root ! Pass(to = "sec-a", msg = "Debug")
      updater.expectMsg("sec-a")
      root ! Pass(to = "notExistent", msg = "Debug")
      updater.expectNoMsg
    }

    def `actor exists?`(path: String) = {
      system.actorSelection(path) ! akka.actor.Identify("hello")
      expectMsgPF() {
        case akka.actor.ActorIdentity("hello", some) =>
          withClue(path + " didn't reply!") { some should not be (None) }
          val Some(actorRef) = some
          path should include(actorRef.path.name)
      }
    }

  }

}