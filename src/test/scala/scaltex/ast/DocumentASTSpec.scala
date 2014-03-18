// from: http://doc.akka.io/docs/akka/snapshot/scala/testkit-example.html
// see also: http://blog.matthieuguillermin.fr/2013/06/akka-testing-your-actors/

package de.fraunhofer.scai.scaltex.ast

import scala.util.Random
import scala.language.postfixOps
import scala.concurrent.duration._
 
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.Ignore
 
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

import com.github.pathikrit.dijon._

class DocumentASTSpec
  extends TestKit(ActorSystem("DocumentASTSpec"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val probe = TestProbe()

  override def afterAll {
    system.shutdown()
  }

  "Factory" should {

    "be able to init actors with unique id's and updater actor" in {
      Factory.system = system
      Factory.updater = probe.ref

      val e1 = Factory.makeEntityActor[SectionActor]
      e1 ! Msg.State
      val json = `{}`
      json.nr = 1
      json.heading = ""
      json.varname = ""
      json.from = 1
      expectMsg(Msg.StateAnswer("Section", json.toString, 1))

      Factory.makeEntityActor[TextActor]
      Factory.makeEntityActor[SectionActor]
      Factory.makeEntityActor[SectionActor]
    }

  }

  "SectionActor" should {

    "be able to set it's state" in {
      within(500 millis) {
        val varname = "intro"
        val content = "Introduction"
        val node = system.actorSelection("user/entity1")
        node ! Msg.Varname(varname)
        expectMsg(Ack.Varname(1))
        node ! Msg.Content(content)
        expectMsg(Ack.Content(1))
        node ! Msg.State
        val json = `{}`
        json.nr = 1
        json.heading = content
        json.varname = varname
        json.from = 1
        expectMsg(Msg.StateAnswer("Section", json.toString, 1))
      }
    }

    "be able to discover it's section number" in {
      within(500 millis) {
        val one = system.actorSelection("user/entity1")
        val two = system.actorSelection("user/entity3")
        val three = system.actorSelection("user/entity4")

        two ! Msg.Content("Experiment")
        expectMsg(Ack.Content(3))

        three ! Msg.Content("Summary")
        expectMsg(Ack.Content(4))

        // Start the discovery. Send a Update to the root element.
        one ! Msg.Update

        def mkJson(nr: Int, content: String, varname: String, from: Int) = {
          val json = `{}`
          json.nr = nr
          json.heading = content
          json.varname = varname
          json.from = from
          json
        }

        probe.fishForMessage(500 millis, "Heading 1"){
          case arg: Msg.StateAnswer =>
            val j = mkJson(1, "Introduction", "intro", 1)
            val msg = Msg.StateAnswer("Section", j.toString, 1)
            arg == msg
          case _ => false
        }
        probe.fishForMessage(500 millis, "Heading 2"){
          case arg: Msg.StateAnswer =>
            val j = mkJson(2, "Experiment", "", 3)
            arg == Msg.StateAnswer("Section", j.toString, 3)
          case _ => false
        }
        probe.fishForMessage(500 millis, "Heading 3"){
          case arg: Msg.StateAnswer =>
            val j = mkJson(3, "Summary", "", 4)
            arg == Msg.StateAnswer("Section", j.toString, 4)
          case _ => false
        }
      }
    }

    "have a companion class" in {
      val json = """{
        "nr": 1,
        "heading": "Introduction",
        "varname": "intro",
        "from": 1
      }""".toString
      val sec = new Section()
      sec.fromJson(json)
      sec.nr should be (1)
      sec.heading should be ("Introduction")
      sec.varname should be ("intro")
    }

  }

  "TextActor and SectionActor" should {

    "interact and evaluate code dynamically" in {
      within(3000 millis) {
        val node = system.actorSelection("user/entity2")
        node ! Msg.Content("The heading is ${entity1.heading} and ${entity3.heading}.")
        expectMsg(Ack.Content(2))
        node ! Msg.Update

        probe.fishForMessage(3000 millis, "Evaluated String"){
          case arg: Msg.StateAnswer =>
            val json = `{}`
            json.content = "The heading is Introduction and Experiment."
            json.contentUnresolved = "The heading is ${entity1.heading} and ${entity3.heading}."
            json.varname = ""
            json.from = 2
            arg == Msg.StateAnswer("Text", json.toString, 2)
          case _ => false
        }
      }
    }

    "have a companion class" in {
      val json = `{}`
      json.content = "Some Text"
      json.varname = "foo"
      json.from = 1
      val txt = new Text()
      txt.fromJson(json.toString)
      txt.content should be ("Some Text")
      txt.varname should be ("foo")
      txt.from should be (1)
    }
  }

}

