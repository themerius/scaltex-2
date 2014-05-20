package scaltex

import scala.util.Random
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.collection.mutable.Stack

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

class RootSpec
    extends TestKit(ActorSystem("RootSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  val updater = TestProbe()
  //val interpreter = TestActorRef(new InterpreterActor, "interpreter")

  val root = TestActorRef(new RootActor(updater.ref), "root")
  root ! "Create"

//  val `front matter` = TestActorRef(new Report(updater.ref), "front matter")
//  val `sec a` = TestActorRef(new Report(updater.ref), "sec a")
//  val `par a` = TestActorRef(new Report(updater.ref), "par a")
//
//  val `body matter` = TestActorRef(new Report(updater.ref), "body matter")
//  val `intro` = TestActorRef(new Report(updater.ref), "intro")
//  val `sec b` = TestActorRef(new Report(updater.ref), "sec b")
//  val `par b` = TestActorRef(new Report(updater.ref), "par b")
//  val `concl` = TestActorRef(new Report(updater.ref), "concl")
//  val `sec c` = TestActorRef(new Report(updater.ref), "sec c")
//  val `par c` = TestActorRef(new Report(updater.ref), "par c")
//  val `par d` = TestActorRef(new Report(updater.ref), "par d")
//
//  val `back matter` = TestActorRef(new Report(updater.ref), "back matter")
//  val `sec e` = TestActorRef(new Report(updater.ref), "sec e")

  override def beforeAll {
    // id, next, firstChild
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

    "be able to insert a (new) element after another and remove elements" in {
      val topo = root.underlyingActor.topology
      topo("sec-a")("next") should be ("par-a")
      topo.contains("new-elem") should be (false)
      topo("par-a")("next") should be ("")

      root ! Insert("new-elem", after="sec-a")

      topo("sec-a")("next") should be ("new-elem")
      topo("new-elem")("next") should be ("par-a")
      topo("par-a")("next") should be ("")

      root ! Remove("new-elem")

      topo("sec-a")("next") should be ("par-a")
      topo.contains("new-elem") should be (false)
      topo("par-a")("next") should be ("")
    }

    "provide next references" in {
      root ! AskForNext("sec-a")
      expectMsg(NextIs("par-a"))
    }

    "be able to initiate the Update through the document" in {
      root ! Update
      updater.expectMsgPF() {
        case CurrentState(json) =>
          parse(json)._id should be("front-matter")
      }
    }

    "setup the document actors with a given topology" in {
      ;
    }

  }

}