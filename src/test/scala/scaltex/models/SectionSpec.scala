package scaltex.models

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

import com.github.pathikrit.dijon.JsonStringContext

import scaltex.Messages._

class SectionSpec
    extends TestKit(ActorSystem("SectionSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  val updater = TestProbe()
    
  val `1` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `1.1` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `1.1.1` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `1.2` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `2` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `3` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `3.1` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `3.2` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `3.3` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `3.3.1` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `3.3.2` = TestActorRef(new AvailableModels.Report(updater.ref))
  val `3.3.3` = TestActorRef(new AvailableModels.Report(updater.ref))
  
  override def beforeAll {
    `1` ! Change("Section")
    `1.1` ! Change("SubSection")
    `1.1.1` ! Change("SubSubSection")
    `1.2` ! Change("SubSection")
    `2` ! Change("Section")
    `3` ! Change("Section")
    `3.1` ! Change("SubSection")
    `3.2` ! Change("SubSection")
    `3.3` ! Change("SubSection")
    `3.3.1` ! Change("SubSubSection")
    `3.3.2` ! Change("SubSubSection")
    `3.3.3` ! Change("SubSubSection")
    
    `1` ! Next(`1.1`.path.name)
    `1.1` ! Next(`1.1.1`.path.name)
    `1.1.1` ! Next(`1.2`.path.name)
    `1.2` ! Next(`2`.path.name)
    `2` ! Next(`3`.path.name)
    `3` ! Next(`3.1`.path.name)
    `3.1` ! Next(`3.2`.path.name)
    `3.2` ! Next(`3.3`.path.name)
    `3.3` ! Next(`3.3.1`.path.name)
    `3.3.1` ! Next(`3.3.2`.path.name)
    `3.3.2` ! Next(`3.3.3`.path.name)
  }
  
  override def afterAll {
    system.shutdown()
  }

  "The SECTION document elements" should {
    
    "have `title` and `numbering` properties" in {
      `1`.underlyingActor.documentElement.state.title should be ("Heading")
      `1`.underlyingActor.documentElement.state.numbering should be ("1")
    }
    
    "be able to discover it's (primary) section number" in {
      `1` ! Update
      `1`.underlyingActor.documentElement.state.numbering should be ("1")
      `2`.underlyingActor.documentElement.state.numbering should be ("2")
      `3`.underlyingActor.documentElement.state.numbering should be ("3")
    }
    
    "be able to discover it's (secundary) section number" in {
      `1` ! Update
      `1.1`.underlyingActor.documentElement.state.numbering should be ("1.1")
      `1.2`.underlyingActor.documentElement.state.numbering should be ("1.2")
      `3.1`.underlyingActor.documentElement.state.numbering should be ("3.1")
      `3.2`.underlyingActor.documentElement.state.numbering should be ("3.2")
      `3.3`.underlyingActor.documentElement.state.numbering should be ("3.3")
    }
    
    "be able to discover it's (tertiary) section number" in {
      `1` ! Update
      `1.1.1`.underlyingActor.documentElement.state.numbering should be ("1.1.1")
      `3.3.1`.underlyingActor.documentElement.state.numbering should be ("3.3.1")
      `3.3.2`.underlyingActor.documentElement.state.numbering should be ("3.3.2")
      `3.3.3`.underlyingActor.documentElement.state.numbering should be ("3.3.3")
    }
    
  }

}
