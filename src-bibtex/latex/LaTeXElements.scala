package io.bibimbap
package latex

trait LaTeXElements extends LaTeXTranslation {
  sealed trait LaTeXElement

  type LaTeXString = Seq[LaTeXElement]
  case class Group(elems : LaTeXString) extends LaTeXElement
  case class RawString(value : String) extends LaTeXElement
  case class Macro(name : String, args : Seq[Group] = Seq.empty) extends LaTeXElement
  case class Whitespace(value : String) extends LaTeXElement
  // Think of MathMode as a sort of "\ensuremath" environment.
  // It's fine to nest them (normalization removes the inner ones).
  // It's currently impossible to represent escaping from mathmode (as \text{...} does, for instance).
  case class MathMode(elems : LaTeXString) extends LaTeXElement

  // TODO: decide what to do with _ and ^...

  def normalize(elems : LaTeXString) : LaTeXString = {
    def norm(e : LaTeXElement, inMath : Boolean) : LaTeXString = e match {
      case Group(Seq(Group(es))) =>
        Seq(Group(rec(es, inMath)))

      case Group(es) =>
        Seq(Group(rec(es, inMath)))

      case MathMode(es) =>
        val r = rec(es, true)
        if(inMath) r else Seq(MathMode(r))

      case Macro(n, as) if(!as.isEmpty) =>
        Seq(Macro(n, as.map(a => norm(a, inMath).asInstanceOf[Group]))) // sad :'(

      case _ => Seq(e)
    }

    def rec(es : LaTeXString, inMath : Boolean) : LaTeXString = {
      val reverseNormalized = es.foldLeft[LaTeXString](Nil) { (s,e) =>
        if(s.isEmpty) {
          norm(e, inMath).reverse
        } else {
          val x = s.head
          val xs = s.tail
          (x, e) match {
            case (RawString(v1), RawString(v2)) => RawString(v1 + v2) +: xs
            case (Whitespace(v1), Whitespace(v2)) => Whitespace(v1 + v2) +: xs
            case (MathMode(es1), MathMode(es2)) => MathMode(es1 ++ rec(es2, true)) +: xs
            case _ => norm(e, inMath).reverse ++ (x +: xs)
          }
        }
      }
      reverseNormalized.reverse
    }

    rec(elems, false)
  }

  def renderLaTeX(elems : LaTeXString) : String = {
    val sb = new StringBuilder

    var fst = true
    var ws  = false
    
    def app(str : String) {
      if(ws && !fst) {
        sb.append(" ")
      }
      ws = false
      fst = false
      sb.append(str)
    }

    def rec(e : LaTeXElement) : Unit = e match {
      case Group(es) => es.foreach(rec)
      case RawString(v) => app(v)
      case Macro(n, as) =>
        translate(n, as) match {
          case Some(s) => app(s)
          case None => as.foreach(rec)
        }
      case Whitespace(_) => ws = true
      case MathMode(es) => es.foreach(rec)
    }

    elems.foreach(rec)
    sb.toString
  }
}
