package scaltex.models

import akka.actor.Props
import akka.actor.ActorRef

import scaltex._

object AvailableModels { // should be generated

  import scaltex.models.report
  class Report(updater: ActorRef) extends BaseActor(updater) {
    val availableDocElems = Map[String, DocumentElement](
      "Paragraph" -> new report.Paragraph
    )
  }

  def configuredActors(updater: ActorRef) = Map[String, Props](
    "Report" -> Props(new Report(updater))
  )

}