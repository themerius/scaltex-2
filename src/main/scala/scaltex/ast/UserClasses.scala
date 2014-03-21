package de.fraunhofer.scai.scaltex.ast

import com.github.pathikrit.dijon


class Section(var heading: String) {
  def this() = this("")

  var nr = 0

  var varname = ""
  var from = 0

  def fromJson(json: String) = {
    val sec = dijon.parse(json)
    nr = sec.nr.as[Double].get.toInt
    heading = sec.heading.as[String].getOrElse("")
    varname = sec.varname.as[String].getOrElse("")
    from = sec.from.as[Double].get.toInt
  }
}


class Text(var text: String) {
  def this() = this("")

  var varname = ""
  var from = 0

  def fromJson(json: String) = {
    val txt = dijon.parse(json)
    text = txt.text.as[String].getOrElse("")
    varname = txt.varname.as[String].getOrElse("")
    from = txt.from.as[Double].get.toInt
  }
}


class Figure(var url: String, var desc: String) {
  def this() = this("", "")

  var nr = 0

  var varname = ""
  var from = 0

  def fromJson(json: String) = {
    val fig = dijon.parse(json)
    nr = fig.nr.as[Double].get.toInt
    url = fig.url.as[String].getOrElse("")
    desc = fig.desc.as[String].getOrElse("")
    varname = fig.varname.as[String].getOrElse("")
    from = fig.from.as[Double].get.toInt
  }
}