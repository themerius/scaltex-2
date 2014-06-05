package scaltex.models

import akka.actor.Props
import akka.actor.ActorRef

import scaltex._

object AvailableModels { // should be generated

  import scaltex.models.report
  class Report(updater: ActorRef) extends BaseActor(updater) {
    val availableDocElems = AvailableModels.availableDocElems("Report")
  }

  def configuredActors(updater: ActorRef) = Map[String, Props](
    "Report" -> Props(new Report(updater)))

  def availableDocElems = Map[String, Map[String, DocumentElement]](
    "Report" -> Map[String, DocumentElement](
      "Paragraph" -> new report.Paragraph,
      "Section" -> new report.Section,
      "SubSection" -> new report.SubSection,
      "SubSubSection" -> new report.SubSubSection,
      "FrontMatter" -> new report.FrontMatter,
      "BodyMatter" -> new report.BodyMatter,
      "BackMatter" -> new report.BackMatter)
  )
}