package scaltex.models.report

import akka.actor.ActorSelection

import com.github.pathikrit.dijon.{`[]`, `{}`}
import com.github.pathikrit.dijon.Json

import org.clapper.markwrap.MarkWrap
import org.clapper.markwrap.MarkupType

import scaltex.DocumentElement
import scaltex.Refs
import scaltex.Messages.M

class List extends DocumentElement {

  this.state.items = `[]`
  this.state.style = `{}`
  this.state.style.unordered = true
  this.state.style.roman = false
  this.state.style.arabic = false

  override def _gotUpdate(actorState: Json[_], refs: Refs) = {
    val repr = actorState.contentRepr.as[String].get
    val parser = MarkWrap.parserFor(MarkupType.Markdown)
    val html = parser.parseToHTML(repr)

    try {
      val xml = scala.xml.XML.loadString(html)
      //val items = (xml \\ "ul" \\ "li").toList.map(_.text)
      val items = (xml \\ "ul" \\ "li").toList.map{
        case <li>{inner @ _*}</li> => inner.mkString("")  // preserve inner xml
        case _ => "there is no list item"
      }
      this.state.items = `[]`
      for ( (item, i) <- items.view.zipWithIndex)
        this.state.items(i) = item
    } catch {
      case e: org.xml.sax.SAXParseException => println("List: XML markup not valid.")
    }

    super._gotUpdate(actorState, refs)
  }

  def _processMsg(m: M, refs: Refs) = println(m)
}


class RomanList extends List {
  this.state.style.unordered = false
  this.state.style.roman = true
  this.state.style.arabic = false
}


class ArabicList extends List {
  this.state.style.unordered = false
  this.state.style.roman = false
  this.state.style.arabic = true
}
