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

import scaltex.Messages._

class ReportSpec
    extends TestKit(ActorSystem("ReportSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll
    with GivenWhenThen {

  val updater = TestProbe()
  val props = AvailableModels.configuredActors(updater.ref)("Report")
  val root = TestActorRef(new scaltex.RootActor(updater.ref, props), "root")

  override def beforeAll {
    root ! InitTopology("""
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
    """)

    root ! Setup
  }

  override def afterAll {
    system.shutdown()
  }

  "A REPORT document meta model element" should {

    "save it's unique id into it's state" in {
      val ref = TestActorRef(new AvailableModels.Report(updater.ref))
      val actor = ref.underlyingActor

      actor.state._id should be(ref.path.name)
      actor.id should be(ref.path.name)
    }

    "be able to change it's current assigned document element" in {
      val ref = TestActorRef(new AvailableModels.Report(updater.ref))
      val actor = ref.underlyingActor

      actor.assignedDocElem should be("")
      ref ! Change(to = "Paragraph")
      actor.assignedDocElem should be("Paragraph")
      ref ! Change(to = "Section")
      actor.assignedDocElem should be("Section")
    }

    "be able to obtain a reference to the next actor" in {
      val refA = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorA = refA.underlyingActor
      val refB = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorB = refB.underlyingActor

      actorA.next.pathString should be("/")
      actorB.next.pathString should be("/")
      refA ! Next(id = refB.path.name)
      actorA.next.pathString should be("/../" + refB.path.name)
      actorB.next.pathString should be("/")
    }

    "send update to the next actors" in {
      val refA = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorA = refA.underlyingActor
      val refB = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorB = refB.underlyingActor
      val refC = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorC = refB.underlyingActor

      refA ! Next(id = refB.path.name)
      refB ! Next(id = refC.path.name)

      refA ! Update
      updater.expectMsgPF() {
        case CurrentState(json) => parse(json)._id should be(refC.path.name)
      }
      updater.expectMsgPF() {
        case CurrentState(json) => parse(json)._id should be(refB.path.name)
      }
      updater.expectMsgPF() {
        case CurrentState(json) => parse(json)._id should be(refA.path.name)
      }
    }

    "have a content (source, representation and result from evaluation)" in {
      val ref = TestActorRef(new AvailableModels.Report(updater.ref))
      val actor = ref.underlyingActor

      actor.state.contentSrc should be("")
      actor.state.contentRepr should be("")
      actor.state.contentEval should be("")
      ref ! Content("some content.")
      actor.state.contentSrc should be("some content.")
      actor.state.contentRepr should be("")
      actor.state.contentEval should be("")
    }

  "send deltas when the topology changes" in {
    When("a new element is inserted (after sec_a)")
    val sec_a = system.actorSelection("/user/root/front_matter/sec_a")
    val msgs = List(Content("my content"), Change("Paragraph"))
    sec_a ! InsertWithInitMsgs("new_elem", "sec_a", "", msgs)

    Then("updater should receive a delta (topology change set)")
    updater.expectMsg(Insert("new_elem", after = "sec_a"))  // for the semantic editor frontend

    And("the new-elem should register itself to root")
    root.underlyingActor.addresses("new_elem").path.name should be ("new_elem")
  }

  }

}
