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

class CouchDbSpec
    extends TestKit(ActorSystem("CouchDbSpec"))
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
    // Persist some state
    val sec_a = """{"contentSrc": "some heading", "documentElement": "Section", "shortName": ""}"""
    HTTP.put(url + "/sec_a", sec_a.getBytes, "text/json").getStatus
  }

  override def afterAll {
    system.shutdown()
    HTTP.delete(url)
  }

  "Root" should {

    "be able to load a document from CouchDB persistance" in {
      root ! DocumentHome(url) // calls automatically Setup
      allActorsLoaded
    }

    "NOT integrate _id and _rev from CouchDB into the topology" in {
      root.underlyingActor.topology.contains("_id") should be(false)
      root.underlyingActor.topology.contains("_rev") should be(false)
    }

    "persist the topology on changes" in {
      root.underlyingActor.update("some_elem", "some_next", "some_fc")
      root.underlyingActor.persistTopology

      awaitAssert(repliedCorrectState)

      def repliedCorrectState = {
        val reply = HTTP.get(url + "/root")
        if (reply.getStatus == 200) {
          val json = parse(reply.getTextBody)
          json.some_elem.next should be("some_next")
          json.some_elem.firstChild should be("some_fc")
        } else {
          fail
        }
      }
    }

  }

  "Each such created element" should {

    "aquire it's state from persistance 'to life'" in {
      allActorsLoaded

      system.actorSelection("/user/root/front_matter/sec_a") ! State

      val messages = updater.receiveN(1).asInstanceOf[Seq[CurrentState]]
      val states = messages.map(x => parse(x.json))

      val sec_a = states.filter(x => x._id == "sec_a")(0)
      sec_a.contentSrc should be("some heading")
      sec_a.documentElement should be("Section")
    }

    "save state, if changed, back to persistance" in {
      allActorsLoaded
      val sec_a = system.actorSelection("/user/root/front_matter/sec_a")
      sec_a ! Content("a other heading")
      sec_a ! Update

      awaitCond(repliedCorrectState)

      def repliedCorrectState: Boolean = {
        val reply = HTTP.get(url + "/sec_a")
        if (reply.getStatus == 200) {
          val json = parse(reply.getTextBody)
          json._id should be("sec_a")
          json.contentSrc == "a other heading"
        } else {
          sec_a ! Update
          false
        }
      }
    }

    "save state if it's not persisted yet" in {
      allActorsLoaded

      val reply = HTTP.get(url + "/sec_e")
      reply.getStatus should be(404) // not existent

      val sec_e = system.actorSelection("/user/root/back_matter/sec_e")
      sec_e ! Content("test")
      sec_e ! Update

      awaitCond(repliedCorrectState)

      def repliedCorrectState: Boolean = {
        val reply = HTTP.get(url + "/sec_e")
        if (reply.getStatus == 200) {
          val json = parse(reply.getTextBody)
          json._id should be("sec_e")
          json.contentSrc == "test"
        } else {
          false
        }
      }
    }

  }

  def allActorsLoaded = {
    val adr = root.underlyingActor.addresses
    awaitCond(
      (
        adr.contains("front_matter") && adr.contains("sec_a") &&
        adr.contains("par_a") && adr.contains("body_matter") &&
        adr.contains("sec_b") && adr.contains("par_b") &&
        adr.contains("sec_c") && adr.contains("par_c") &&
        adr.contains("back_matter") && adr.contains("sec_e")))
  }

}
