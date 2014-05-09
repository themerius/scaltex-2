package scaltex.models

import akka.actor.Props

import scaltex._

object AvailableModels { // should be generated

  import scaltex.models.report
  class Report extends BaseActor {
    val availableDocElems = Map[String, DocumentElement](
      "Paragraph" -> new report.Paragraph
    )
  }

  val configuredActors = Map[String, Props](
    "Report" -> Props[Report]
  )

}