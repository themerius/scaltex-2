// from: http://doc.akka.io/docs/akka/snapshot/scala/testkit-example.html
// see also: http://blog.matthieuguillermin.fr/2013/06/akka-testing-your-actors/

package scai.scaltex.model

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


class ReferenceActorsSpec
  extends TestKit(ActorSystem("ReferenceActorsSpec"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val probe = TestProbe()

  override def afterAll {
    system.shutdown()
  }

  "DocumentAST" should {
    "have basic messages" in {
      // val vn = Msg.Varname("Lorem")
      // val cn = Msg.Content("Ipsum")
      // val nx = Msg.Next(id) // ActorRef, mocking?
      // val st = Msg.State
      // val sa = Msg.StateAnswer(cls, json, id)
      // val up = Msg.Update
    }
    "be able to init actors with it's Factory" in {
      Factory.system = system
      Factory.updater = probe.ref

      val e1 = Factory.makeEntityActor[SectionActor]
      e1 ! Msg.State
      expectMsg(SectionArgs(1, "", "", 1))

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
        expectMsg(SectionArgs(1, content, varname, 1))
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

        probe.fishForMessage(150 millis, "Heading 1"){
          case arg: SectionArgs =>
            arg == SectionArgs(1, "Introduction", "intro", 1)
          case _ => false
        }
        probe.fishForMessage(150 millis, "Heading 2"){
          case arg: SectionArgs =>
            arg == SectionArgs(2, "Experiment", "", 3)
          case _ => false
        }
        probe.fishForMessage(150 millis, "Heading 3"){
          case arg: SectionArgs =>
            arg == SectionArgs(3, "Summary", "", 4)
          case _ => false
        }
      }
    }
  }

  "TextActor and SectionActor" should {
    "interact and evaluate code dynamically" in {
      within(750 millis) {
        val node = system.actorSelection("user/entity2")
        node ! Msg.Content("The heading is ${entity1.heading}.")
        expectMsg(Ack.Content(2))
        node ! Msg.Update

        probe.fishForMessage(750 millis, "Heading 3"){
          case arg: TextArgs =>
            arg == TextArgs("The heading is Introduction.", "", 2)
          case _ => false
        }
      }
    }
  }

}

