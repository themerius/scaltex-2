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

    "be able to init actors with unique id's and a updater actor" in {
      Factory.system = system
      Factory.updater = probe.ref

      val e1 = Factory.makeEntityActor[EntityActor]                         // (1)
      e1 ! Msg.ClassDef("Section")
      e1 ! Msg.State
      val json = `{}`
      json.nr = 1
      json.content = ""
      json.heading = ""
      json.varname = ""
      json.from = 1
      json.classDef = "Section"
      expectMsg(Msg.StateAnswer(json.toString))

      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("Text")           // (2)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("Section")        // (3)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("Section")        // (4)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("Figure")         // (5)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSection")     // (6)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSection")     // (7)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSubSection")  // (8)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSubSection")  // (9)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSubSection")  // (10)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSection")     // (11)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("Section")        // (12)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSection")     // (13)
      Factory.makeEntityActor[EntityActor] ! Msg.ClassDef("SubSubSection")  // (14)
    }

  }

  "Section Actor" should {

    "be able to set it's state" in {
      within(1000 millis) {
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
        json.content = content
        json.heading = ""  // unresolved, it's triggered by discover references
        json.varname = varname
        json.from = 1
        json.classDef = "Section"
        expectMsg(Msg.StateAnswer(json.toString))
      }
    }

    "be able to discover it's section number" in {
      within(2000 millis) {
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
          json.content = content
          json.heading = content
          json.varname = varname
          json.from = from
          json.classDef = "Section"
          json
        }

        probe.fishForMessage(2000 millis, "Heading 1"){
          case arg: Msg.StateAnswer =>
            val j = mkJson(1, "Introduction", "intro", 1)
            val msg = Msg.StateAnswer(j.toString)
            arg == msg
          case _ => false
        }
        probe.fishForMessage(2000 millis, "Heading 2"){
          case arg: Msg.StateAnswer =>
            val j = mkJson(2, "Experiment", "", 3)
            arg == Msg.StateAnswer(j.toString)
          case _ => false
        }
        probe.fishForMessage(2000 millis, "Heading 3"){
          case arg: Msg.StateAnswer =>
            val j = mkJson(3, "Summary", "", 4)
            arg == Msg.StateAnswer(j.toString)
          case _ => false
        }
      }
    }

    "have a user class" in {
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

  "SubSection Actor" should {

    "be able to discover it's section number" in {
      within(2000 millis) {
        val s3_1 = system.actorSelection("user/entity6")
        val s3_2 = system.actorSelection("user/entity7")
        val s3_3 = system.actorSelection("user/entity11")
        val s4_1 = system.actorSelection("user/entity13")

        // Start the discovery. Send a Update to the root element.
        system.actorSelection("user/entity1") ! Msg.Update

        def mkJson(h1: Int, h2: Int, from: Int) = {
          val json = `{}`
          json.nr = s"$h1.$h2"
          json.h1 = h1
          json.h2 = h2
          json.content = ""
          json.heading = ""
          json.varname = ""
          json.from = from
          json.classDef = "SubSection"
          json.toString
        }

        probe.fishForMessage(2000 millis, "Heading 3.1"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(3, 1, from=6))
          case _ => false
        }

        probe.fishForMessage(2000 millis, "Heading 3.2"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(3, 2, from=7))
          case _ => false
        }

        probe.fishForMessage(2000 millis, "Heading 3.3"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(3, 3, from=11))
          case _ => false
        }

        probe.fishForMessage(2000 millis, "Heading 4.1"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(4, 1, from=13))
          case _ => false
        }
      }
    }

    "have a user class" in {
      val json = """{
        "nr": "1.1",
        "heading": "Some Heading",
        "varname": "some_heading",
        "from": 1
      }""".toString
      val sec = new SubSection()
      sec.fromJson(json)
      sec.nr should be ("1.1")
      sec.heading should be ("Some Heading")
      sec.varname should be ("some_heading")
    }

  }

  "SubSubSection Actor" should {

    "be able to discover it's section number" in {
      within(2000 millis) {
        val s3_2_1 = system.actorSelection("user/entity8")
        val s3_2_2 = system.actorSelection("user/entity9")
        val s3_2_3 = system.actorSelection("user/entity10")
        val s4_1_1 = system.actorSelection("user/entity14")

        // Start the discovery. Send a Update to the root element.
        system.actorSelection("user/entity1") ! Msg.Update

        def mkJson(h1: Int, h2: Int, h3: Int, from: Int) = {
          val json = `{}`
          json.nr = s"$h1.$h2.$h3"
          json.h1 = h1
          json.h2 = h2
          json.h3 = h3
          json.content = ""
          json.heading = ""
          json.varname = ""
          json.from = from
          json.classDef = "SubSubSection"
          json.toString
        }

        probe.fishForMessage(2000 millis, "Heading 3.2.1"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(3, 2, 1, from=8))
          case _ => false
        }

        probe.fishForMessage(2000 millis, "Heading 3.2.2"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(3, 2, 2, from=9))
          case _ => false
        }

        probe.fishForMessage(2000 millis, "Heading 3.2.3"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(3, 2, 3, from=10))
          case _ => false
        }

        probe.fishForMessage(2000 millis, "Heading 4.1.1"){
          case arg: Msg.StateAnswer =>
            arg == Msg.StateAnswer(mkJson(4, 1, 1, from=14))
          case _ => false
        }

      }
    }

    "have a user class" in {
      val json = """{
        "nr": "1.1.1",
        "heading": "Some Heading",
        "varname": "some_heading",
        "from": 1
      }""".toString
      val sec = new SubSubSection()
      sec.fromJson(json)
      sec.nr should be ("1.1.1")
      sec.heading should be ("Some Heading")
      sec.varname should be ("some_heading")
    }

  }

  "Text Actor" should {

    "be able to set it's state" in {
      within(1000 millis) {
        val varname = "mytext"
        val content = "Lorem Ipsum"
        val node = system.actorSelection("user/entity2")
        node ! Msg.Varname(varname)
        expectMsg(Ack.Varname(2))
        node ! Msg.Content(content)
        expectMsg(Ack.Content(2))
        node ! Msg.State
        val json = `{}`
        json.content = content
        json.text = ""  // unresolved, it's triggered by discover references
        json.varname = varname
        json.from = 2
        json.classDef = "Text"
        expectMsg(Msg.StateAnswer(json.toString))
      }
    }

    "have a user class" in {
      val json = `{}`
      json.text = "Some Text"
      json.varname = "foo"
      json.from = 1
      val txt = new Text()
      txt.fromJson(json.toString)
      txt.text should be ("Some Text")
      txt.varname should be ("foo")
      txt.from should be (1)
    }

  }

  "Figure Actor" should {

    "be able to parse the (user) given content" in {
      within(3000 millis) {
        val content = """ url = "http://url.tdl", desc = "Hello World." """

        val node = system.actorSelection("user/entity5")

        node ! Msg.Content(content)
        expectMsg(Ack.Content(5))

        node ! Msg.State

        val json = `{}`
        json.nr = 1
        json.content = content.replace("\"", "\\\"")
        json.url = "http://url.tdl"
        json.desc = "Hello World."
        json.varname = ""
        json.from = 5
        json.classDef = "Figure"

        expectMsg(Msg.StateAnswer(json.toString))
      }
    }

    "have a user class" in {
      val json = `{}`
      json.nr = 1
      json.url = "http://url.tdl"
      json.desc = "Hello World."
      json.varname = "foo"
      json.from = 1
      val fig = new Figure()
      fig.fromJson(json.toString)
      fig.nr should be (1)
      fig.url should be ("http://url.tdl")
      fig.desc should be ("Hello World.")
      fig.varname should be ("foo")
      fig.from should be (1)
    }

  }

  "Scala Interpreter" should {

    " be able to discover references to other nodes" in {
      within(3000 millis) {
        val node = system.actorSelection("user/entity2")
        node ! Msg.Content("The heading is ${entity1.heading} and ${entity3.heading}.")
        expectMsg(Ack.Content(2))
        node ! Msg.Update

        probe.fishForMessage(3000 millis, "Evaluated String"){
          case arg: Msg.StateAnswer =>
            val json = `{}`
            json.content = "The heading is ${entity1.heading} and ${entity3.heading}."
            json.text = "The heading is Introduction and Experiment."
            json.varname = "mytext"
            json.from = 2
            json.classDef = "Text"
            arg == Msg.StateAnswer(json.toString)
          case _ => false
        }
      }
    }

  }

}