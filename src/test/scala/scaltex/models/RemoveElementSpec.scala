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

class RemoveElementSpec
    extends TestKit(ActorSystem("RemoveElementSpec", ConfigFactory.load(cfg.customConf)))
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

  "Remove of a non-leaf" should {

    "repair the topology and put the removed subtree into the graveyard" in {
      allActorsLoaded
      // move body_matter onto the place of back_matter
      val front_matter = system.actorSelection("/user/root/front_matter")
      front_matter ! Remove

      val topo = root.underlyingActor.topology
      awaitAssert(topo("root")("firstChild") should be("body_matter"))
      awaitAssert(topo("body_matter")("next") should be("back_matter"))
      awaitAssert(topo("body_matter")("firstChild") should be("sec_b"))
      awaitAssert(topo("back_matter")("next") should be(""))
      awaitAssert(topo("back_matter")("firstChild") should be("sec_e"))

      topo should not contain allOf ("front_matter", "sec_a", "par_a")

      val addresses = root.underlyingActor.addresses
      addresses should not contain allOf ("front_matter", "sec_a", "par_a")

      val graveyard = root.underlyingActor.graveyard
      graveyard should contain allOf ("front_matter", "sec_a", "par_a")

      val messages = updater.receiveN(3).asInstanceOf[Seq[RemoveDelta]]
      val foundIds = messages.map(_.id) // remove requests to the front end
      foundIds should contain allOf ("front_matter", "sec_a", "par_a")

      `actor should exist:`("/user/root/body_matter")
      `actor should exist:`("/user/root/back_matter")

      `actor shouldn't exist:`("/user/root/front_matter")
      `actor shouldn't exist:`("/user/root/front_matter/sec_a")
      `actor shouldn't exist:`("/user/root/front_matter/par_a")

      system.actorSelection("/user/root/body_matter") ! "Next"
      expectMsg("back_matter")
      system.actorSelection("/user/root/body_matter") ! "FirstChild"
      expectMsg("sec_b")
      system.actorSelection("/user/root/back_matter") ! "Next"
      expectMsg("")
      system.actorSelection("/user/root/back_matter") ! "FirstChild"
      expectMsg("sec_e")
    }

  }

  "Remove of a leaf" should {

    "repair the topology and put the removed element into the graveyard" in {
      allActorsLoaded
      // move body_matter onto the place of back_matter
      val front_matter = system.actorSelection("/user/root/body_matter/par_b")
      front_matter ! Remove

      val topo = root.underlyingActor.topology
      awaitAssert(topo.contains("par_b") should be(false))
      awaitAssert(topo("body_matter")("firstChild") should be("sec_b"))
      awaitAssert(topo("body_matter")("next") should be("back_matter"))
      awaitAssert(topo("sec_b")("next") should be("sec_c"))

      val addresses = root.underlyingActor.addresses
      addresses.contains("par_b") should be(false)

      val graveyard = root.underlyingActor.graveyard
      graveyard should contain ("par_b")

      val messages = updater.receiveN(1).asInstanceOf[Seq[RemoveDelta]]
      val foundIds = messages.map(_.id) // remove requests to the front end
      foundIds should contain ("par_b")

      `actor should exist:`("/user/root/body_matter")
      `actor shouldn't exist:`("/user/root/body_matter/par_b")

      system.actorSelection("/user/root/body_matter/sec_b") ! "Next"
      expectMsg("sec_c")
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
