package de.fraunhofer.scai.scaltex.ast

import com.github.pathikrit.dijon._


class Section(var nr: Int, var heading: String) {
  def this() = this(0, "")

  var varname = ""
  var from = 0

  def fromJson(json: String) = {
    val sec = parse(json)
    nr = sec.nr.as[Double].get.toInt
    heading = sec.heading.as[String].getOrElse("")
    varname = sec.varname.as[String].getOrElse("")
    from = sec.from.as[Double].get.toInt
  }
}


class Text(var content: String) {
  def this() = this("")

  var varname = ""
  var from = 0

  def fromJson(json: String) = {
    val txt = parse(json)
    content = txt.content.as[String].getOrElse("")
    varname = txt.varname.as[String].getOrElse("")
    from = txt.from.as[Double].get.toInt
  }
}
