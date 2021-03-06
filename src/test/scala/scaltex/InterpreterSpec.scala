package scaltex

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
import com.github.pathikrit.dijon.parse

import scaltex.Messages._

class InterpreterSpec
    extends TestKit(ActorSystem("InterpreterSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  val interpreter = TestActorRef[InterpreterActor]

  override def afterAll {
    system.shutdown()
  }

  "A INTERPRETER" should {

    "evaluate a string which contains scala code" in {
      val code = "val x = 10"
      interpreter ! Interpret(code, Map[String, Tuple2[String, String]]())
      expectMsg(ReturnValue(10, Map[String, Tuple2[String, String]]()))
      interpreter ! Interpret("val x = 11", Map[String, Tuple2[String, String]]())
      expectMsg(ReturnValue(11, Map[String, Tuple2[String, String]]()))
    }

    "return None if evaluation fails" in {
      val code = "val 1x = 10"
      interpreter ! Interpret(code, Map[String, Tuple2[String, String]]())
      expectMsg(ReturnValue(None, Map[String, Tuple2[String, String]]()))
    }

  }

}
