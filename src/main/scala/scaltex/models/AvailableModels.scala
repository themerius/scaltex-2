package scaltex.models

import akka.actor.Props
import akka.actor.ActorRef

import scaltex._

object AvailableModels { // should be generated

  import scaltex.models.report
  class Report(updater: ActorRef, rootId: String = "root") extends BaseActor(updater, rootId) {
    val availableDocElems = AvailableModels.availableDocElems("Report")
  }

  def configuredActors(updater: ActorRef, rootId: String = "root") = Map[String, Props](
    "Report" -> Props(new Report(updater, rootId)))

  def availableDocElems = Map[String, Map[String, DocumentElement]](
    "Report" -> Map[String, DocumentElement](
      "Paragraph" -> new report.Paragraph,
      "List" -> new report.List,
      "ArabicList" -> new report.ArabicList,
      "RomanList" -> new report.RomanList,
      "Figure" -> new report.Figure,
      "Chapter" -> new report.Chapter,
      "Section" -> new report.Section,
      "SubSection" -> new report.SubSection,
      "SubSubSection" -> new report.SubSubSection,
      "FrontMatter" -> new report.FrontMatter,
      "BodyMatter" -> new report.BodyMatter,
      "BackMatter" -> new report.BackMatter,
      "Python" -> new report.Python,
      "TableOfContents" -> new report.TableOfContents,
      "Chemistry" -> new report.Chemistry,
      "Spray" -> new report.Spray,
      "Annotation" -> new report.Annotation,
      "Reference" -> new report.Reference,
      "Footnote" -> new report.Footnote,
      "Quotation" -> new report.Quotation,
      "Code" -> new report.Code,
      "Math" -> new report.Math,
      "Math2" -> new report.Math2)
  )
}