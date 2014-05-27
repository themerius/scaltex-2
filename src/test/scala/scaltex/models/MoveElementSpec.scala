package scaltex.models

import scala.util.Random
import scala.language.postfixOps
import scala.concurrent.duration._

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

import com.m3.curly.HTTP

import scaltex.Messages._

class MoveElementSpec
    extends TestKit(ActorSystem("MoveElementSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll
    with GivenWhenThen {

  val updater = TestProbe()
  val props = AvailableModels.configuredActors(updater.ref)("Report")
  val root = TestActorRef(new scaltex.RootActor(updater.ref, props), "root")

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

  override def beforeAll {
    // Persist some topology
    HTTP.put(url, "".getBytes, "").getStatus
    HTTP.put(url + "/root", topologySrc.getBytes, "text/json").getStatus
    root ! DocumentHome(url)
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
      body_matter ! Move(onto="front_matter")

      val topo = root.underlyingActor.topology
      awaitAssert(topo("root")("firstChild") should be ("body_matter"))
      awaitAssert(topo("body_matter")("next") should be ("front_matter"))
      awaitAssert(topo("body_matter")("firstChild") should be ("sec_b"))
      awaitAssert(topo("front_matter")("next") should be ("back_matter"))
      awaitAssert(topo("front_matter")("firstChild") should be ("sec_a"))

      `actor exists?`("/user/root/body_matter")
      `actor exists?`("/user/root/body_matter/sec_b")
      `actor exists?`("/user/root/body_matter/par_b")
      `actor exists?`("/user/root/body_matter/sec_c")
      `actor exists?`("/user/root/body_matter/par_c")
    }

    "move the entire subtree onto the place of the selected element" in {
      pending
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

  def `actor exists?`(path: String) = {
    system.actorSelection(path) ! akka.actor.Identify("hello")
    expectMsgPF() {
      case akka.actor.ActorIdentity("hello", some) =>
        withClue(path + " didn't reply!") { some should not be (None) }
        val Some(actorRef) = some
        actorRef.path.toString should include(path)
    }
  }

}
