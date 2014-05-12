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

class ReportSpec
    extends TestKit(ActorSystem("ReportSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

	val updater = TestProbe()
	val props = AvailableModels.configuredActors(updater.ref)("Report")
	val node1 = system.actorOf(props)

  override def afterAll {
    system.shutdown()
  }

  "A REPORT document meta model element" should {
    
    "know it's unique id" in {
      val ref = TestActorRef(new AvailableModels.Report(updater.ref))
      val actor = ref.underlyingActor
      
      actor.id should be ("$$a")
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
      
      actorA.next.pathString should be ("/")
      actorB.next.pathString should be ("/")
      refA ! Next(id=refB.path.name)
      actorA.next.pathString should be ("/../" + refB.path.name)
      actorB.next.pathString should be ("/")
    }
    
    "send update to the next actors" in {
      val refA = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorA = refA.underlyingActor
      val refB = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorB = refB.underlyingActor
      val refC = TestActorRef(new AvailableModels.Report(updater.ref))
      val actorC = refB.underlyingActor
      
      refA ! Next(id=refB.path.name)
      refB ! Next(id=refC.path.name)
      
      refA ! Update
      updater.expectMsg(refA.path.name + " got update")
      updater.expectMsg(refB.path.name + " got update")
      updater.expectMsg(refC.path.name + " got update")
    }
  }

}
