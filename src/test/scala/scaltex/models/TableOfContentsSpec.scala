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

class TableOfContentsSpec
    extends TestKit(ActorSystem("TableOfContentsSpec", ConfigFactory.load(cfg.customConf)))
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

    allActorsLoaded

    val body_matter = system.actorSelection("/user/root/body_matter")
    body_matter ! Change("BodyMatter")
    val sec_b = system.actorSelection("/user/root/body_matter/sec_b")
    sec_b ! Change("Section")
    sec_b ! Content("First")
    val sec_c = system.actorSelection("/user/root/body_matter/sec_c")
    sec_c ! Change("Section")
    sec_c ! Content("Second")

    root ! Update
  }

  override def afterEach {
    HTTP.delete(url)
    root ! akka.actor.PoisonPill
  }

  override def afterAll {
    system.shutdown()
    HTTP.delete(url)
  }

  "The TableOfContents element" should {

    "ask every *Section within BodyMatter" in {
      val toc = TestActorRef(new AvailableModels.Report(updater.ref))
      toc ! Change("TableOfContents")
      toc ! Update

      val items = toc.underlyingActor.documentElement.state.items
      awaitAssert(items(0).numbering should be ("1"))
      awaitAssert(items(0).title should be ("First"))
      awaitAssert(items(1).numbering should be ("2"))
      awaitAssert(items(1).title should be ("Second"))
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

}
