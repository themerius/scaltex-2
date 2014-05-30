package scaltex.models

import scala.util.Random
import scala.language.postfixOps
import scala.concurrent.duration._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.GivenWhenThen
import org.scalatest.BeforeAndAfterEach

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

import com.m3.curly.HTTP

import scaltex.Messages._
import com.typesafe.config.ConfigFactory

object cfg {
  val customConf = ConfigFactory.parseString("""
    akka {
      log-dead-letters-during-shutdown = off
    }
  """)
}

class MoveElementSpec
    extends TestKit(ActorSystem("MoveElementSpec", ConfigFactory.load(cfg.customConf)))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll
    with GivenWhenThen with BeforeAndAfterEach {

  var updater: TestProbe = _
  var root: TestActorRef[scaltex.RootActor] = _

  val url = s"http://127.0.0.1:5984/${getClass.getSimpleName.toLowerCase}"

  val topologySrc = """
	{
	  "root": {
	    "next": "",
	    "firstChild": "front_matter"
	  },
	  "front_matter": {
	    "next": "body_matter",
	    "firstChild": "sec_a"
	  },
	  "sec_a": {
	    "next": "par_a",
	    "firstChild": ""
	  },
	  "par_a": {
	    "next": "",
	    "firstChild": ""
	  },
	  "body_matter": {
	    "next": "back_matter",
	    "firstChild": "sec_b"
	  },
	  "sec_b": {
	    "next": "par_b",
	    "firstChild": ""
	  },
	  "par_b": {
	    "next": "sec_c",
	    "firstChild": ""
	  },
	  "sec_c": {
	    "next": "par_c",
	    "firstChild": ""
	  },
	  "par_c": {
	    "next": "",
	    "firstChild": ""
	  },
	  "back_matter": {
	    "next": "",
	    "firstChild": "sec_e"
	  },
	  "sec_e": {
	    "next": "",
	    "firstChild": ""
	  }
	}
    """

  override def beforeEach {
    // Persist some topology
    HTTP.put(url, "".getBytes, "").getStatus
    HTTP.put(url + "/root", topologySrc.getBytes, "text/json").getStatus
    updater = TestProbe()
    val props = AvailableModels.configuredActors(updater.ref)("Report")
    root = TestActorRef(new scaltex.RootActor(updater.ref, props), "root")
    root ! DocumentHome(url)
  }

  override def afterEach {
    HTTP.delete(url)
    root ! akka.actor.PoisonPill
  }

  override def afterAll {
    system.shutdown()
    HTTP.delete(url)
  }

  "Move of a non-leaf" should {

    "change the topology (hang a entire subtree to a destination)" in {
      allActorsLoaded
      // move body_matter onto the place of front_matter
      val body_matter = system.actorSelection("/user/root/body_matter")
      body_matter ! Move(onto = "front_matter")

      val topo = root.underlyingActor.topology
      awaitAssert(topo("root")("firstChild") should be("body_matter"))
      awaitAssert(topo("body_matter")("next") should be("front_matter"))
      awaitAssert(topo("body_matter")("firstChild") should be("sec_b"))
      awaitAssert(topo("front_matter")("next") should be("back_matter"))
      awaitAssert(topo("front_matter")("firstChild") should be("sec_a"))

      `actor should exist:`("/user/root/front_matter")
      `actor should exist:`("/user/root/body_matter")
      system.actorSelection("/user/root/body_matter") ! "Next"
      expectMsg("front_matter")
      system.actorSelection("/user/root/body_matter") ! "FirstChild"
      expectMsg("sec_b")
      system.actorSelection("/user/root/front_matter") ! "Next"
      expectMsg("back_matter")
      system.actorSelection("/user/root/front_matter") ! "FirstChild"
      expectMsg("sec_a")
    }

    "change the topology (hang a entire subtree to a destination II)" in {
      allActorsLoaded
      // move body_matter onto the place of front_matter
      val front_matter = system.actorSelection("/user/root/front_matter")
      front_matter ! Move(onto = "back_matter")

      val topo = root.underlyingActor.topology
      awaitAssert(topo("root")("firstChild") should be("body_matter"))
      awaitAssert(topo("body_matter")("next") should be("front_matter"))
      awaitAssert(topo("body_matter")("firstChild") should be("sec_b"))
      awaitAssert(topo("front_matter")("next") should be("back_matter"))
      awaitAssert(topo("front_matter")("firstChild") should be("sec_a"))
      awaitAssert(topo("back_matter")("next") should be(""))
      awaitAssert(topo("back_matter")("firstChild") should be("sec_e"))

      `actor should exist:`("/user/root/body_matter")
      `actor should exist:`("/user/root/back_matter")
      `actor should exist:`("/user/root/front_matter")

      system.actorSelection("/user/root/body_matter") ! "Next"
      expectMsg("front_matter")
      system.actorSelection("/user/root/body_matter") ! "FirstChild"
      expectMsg("sec_b")
      system.actorSelection("/user/root/back_matter") ! "Next"
      expectMsg("")
      system.actorSelection("/user/root/back_matter") ! "FirstChild"
      expectMsg("sec_e")
      system.actorSelection("/user/root/front_matter") ! "Next"
      expectMsg("back_matter")
      system.actorSelection("/user/root/front_matter") ! "FirstChild"
      expectMsg("sec_a")
    }

    "move the entire subtree into another hierarchy level" in {
      allActorsLoaded

      `actor should exist:`("/user/root/body_matter")
      `actor should exist:`("/user/root/body_matter/sec_b")
      `actor should exist:`("/user/root/body_matter/par_b")
      `actor should exist:`("/user/root/body_matter/sec_c")
      `actor should exist:`("/user/root/body_matter/par_c")

      val body_matter = system.actorSelection("/user/root/body_matter")
      body_matter ! Move(onto = "sec_e")

      updater.expectMsg(
        Delta(
          List("body_matter", "sec_b", "par_b", "sec_c", "par_c"),
          "back_matter"))

      val messages = updater.receiveN(5).asInstanceOf[Seq[RemoveDelta]]
      val foundIds = messages.map(_.id)  // remove requests to the front end

      foundIds should contain("body_matter")
      foundIds should contain("sec_b")
      foundIds should contain("par_b")
      foundIds should contain("sec_c")
      foundIds should contain("par_c")

      `actor shouldn't exist:`("/user/root/body_matter")
      `actor shouldn't exist:`("/user/root/body_matter/sec_b")
      `actor shouldn't exist:`("/user/root/body_matter/par_b")
      `actor shouldn't exist:`("/user/root/body_matter/sec_c")
      `actor shouldn't exist:`("/user/root/body_matter/par_c")

      `actor should exist:`("/user/root/back_matter/body_matter")
      `actor should exist:`("/user/root/back_matter/body_matter/sec_b")
      `actor should exist:`("/user/root/back_matter/body_matter/par_b")
      `actor should exist:`("/user/root/back_matter/body_matter/sec_c")
      `actor should exist:`("/user/root/back_matter/body_matter/par_c")

      system.actorSelection("/user/root/back_matter/body_matter") ! "Next"
      expectMsg("sec_e")
      system.actorSelection("/user/root/back_matter/body_matter") ! "FirstChild"
      expectMsg("sec_b")
      system.actorSelection("/user/root/back_matter") ! "FirstChild"
      expectMsg("body_matter")
      system.actorSelection("/user/root/back_matter") ! "Next"
      expectMsg("")
    }

  }

  "Move of a leaf" should {

    "change the topology and recreate the actor at the desired position" in {
      allActorsLoaded

      `actor should exist:`("/user/root/front_matter/sec_a")

      val sec_a = system.actorSelection("/user/root/front_matter/sec_a")
      sec_a ! Move(onto = "par_b")

      val topo = root.underlyingActor.topology
      awaitAssert(topo("front_matter")("firstChild") should be("par_a"))
      awaitAssert(topo("sec_a")("next") should be("par_b"))
      awaitAssert(topo("sec_a")("firstChild") should be(""))
      awaitAssert(topo("sec_b")("next") should be("sec_a"))
      awaitAssert(topo("sec_b")("firstChild") should be(""))

      `actor shouldn't exist:`("/user/root/front_matter/sec_a")

      `actor should exist:`("/user/root/body_matter/sec_a")
    }

    "also work if moved to a position of a first child" in {
      allActorsLoaded

      `actor should exist:`("/user/root/front_matter/sec_a")

      val sec_a = system.actorSelection("/user/root/front_matter/sec_a")
      sec_a ! Move(onto = "sec_b")

      val topo = root.underlyingActor.topology
      awaitAssert(topo("front_matter")("firstChild") should be("par_a"))
      awaitAssert(topo("sec_a")("next") should be("sec_b"))
      awaitAssert(topo("sec_a")("firstChild") should be(""))
      awaitAssert(topo("sec_b")("next") should be("par_b"))
      awaitAssert(topo("sec_b")("firstChild") should be(""))
      awaitAssert(topo("body_matter")("firstChild") should be("sec_a"))

      `actor shouldn't exist:`("/user/root/front_matter/sec_a")

      `actor should exist:`("/user/root/body_matter/sec_a")

      system.actorSelection("/user/root/body_matter") ! "Next"
      expectMsg("back_matter")
      system.actorSelection("/user/root/body_matter") ! "FirstChild"
      expectMsg("sec_a")
      system.actorSelection("/user/root/front_matter") ! "Next"
      expectMsg("body_matter")
      system.actorSelection("/user/root/front_matter") ! "FirstChild"
      expectMsg("par_a")
    }

  }

  def allActorsLoaded = {
    val adr = root.underlyingActor.addresses
    awaitCond(
      adr.contains("front_matter") && adr.contains("sec_a") &&
        adr.contains("par_a") && adr.contains("body_matter") &&
        adr.contains("sec_b") && adr.contains("par_b") &&
        adr.contains("sec_c") && adr.contains("par_c") &&
        adr.contains("back_matter") && adr.contains("sec_e"))
  }

  def `actor should exist:`(path: String) = awaitAssert {
    system.actorSelection(path) ! akka.actor.Identify("hello")
    expectMsgPF() {
      case akka.actor.ActorIdentity("hello", some) =>
        withClue(path + " didn't reply!") { some should not be (None) }
        val Some(actorRef) = some
        actorRef.path.toString should include(path)
    }
  }

  def `actor shouldn't exist:`(path: String) = awaitAssert {
    system.actorSelection(path) ! akka.actor.Identify("hello")
    expectMsgPF() {
      case akka.actor.ActorIdentity("hello", some) => some should be(None)
    }
  }

}
