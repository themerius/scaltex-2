package io.bibimbap
package latex

import scala.collection.mutable.ListBuffer

trait LaTeXParser {
  self : LaTeXElements =>

  case class LaTeXParseError(message : String) extends Exception(message)

  def parseOpt(str : String) : Option[LaTeXString] = try {
    Some(parse(str))
  } catch {
    case LaTeXParseError(_) => None
  }

  /** This method is only designed to parse "small" LaTeX strings. The kind that would typically fit in one screen line. It will fail to
    * make sense of anything fancy, such as anything resulting from the use of \newcommand, \makeatletter, etc. In fact, properly parsing
    * LaTeX is next to impossible (short of invoking LaTeX itself), so we settle for a subset that is "good enough". */
  def parse(str : String) : LaTeXString = {
    var tokens = strToTokens(str)

    def eof : Boolean = tokens.isEmpty
    def currentToken : Token = tokens.head
    def readOne : Unit = if(!tokens.isEmpty) tokens = tokens.tail

    def eat(t : Token) {
      if(eof) {
        throw LaTeXParseError("Unexpected end of string.")
      } else if (currentToken != t) {
        throw LaTeXParseError("Unexpected token: " + currentToken)
      }
      readOne
    }

    def isElemFirst : Boolean = currentToken match {
      case RAW(_) | MACRO(_) | SPACE(_) | OGROUP | MATH => true
      case CGROUP => false
    }

    def elem(inMath : Boolean) : LaTeXElement = currentToken match {
      case RAW(_) =>
        val sb = new StringBuilder
        while(!eof && currentToken.isInstanceOf[RAW]) {
          sb.append(currentToken.asInstanceOf[RAW].value)
          readOne
        }
        RawString(sb.toString)

      case MACRO(n) =>
        readOne
        val lb = new ListBuffer[Group]
        while(!eof && currentToken == OGROUP) {
          lb.append(group(inMath))
        }
        Macro(n, lb.toList)
  
      case MATH if !inMath =>
        readOne
        val es = elems(inMath = true)
        eat(MATH)
        MathMode(es)

      case SPACE(v) =>
        readOne
        Whitespace(v)

      case OGROUP =>
        group(inMath)

      case other => throw LaTeXParseError("Unexpected token: " + other)
    }

    def group(inMath : Boolean) : Group = {
      eat(OGROUP)
      val es = elems(inMath)
      eat(CGROUP)
      Group(es)
    }

    def elems(inMath : Boolean) : LaTeXString = {
      val lb = new ListBuffer[LaTeXElement]
      while(!eof && isElemFirst && (!inMath || currentToken != MATH)) {
        lb.append(elem(inMath))
      }
      lb.toList
    }
  
    val r = elems(false)
    if(!eof) {
      throw new LaTeXParseError("Unexpected token: " + currentToken)
    }
    r
  }

  private sealed abstract class Token(repr : String) {
    override def toString = repr
  }
  private case class MACRO(name : String) extends Token("\\" + name)
  private case class RAW(value : String) extends Token(value)
  private case class SPACE(value : String) extends Token("'" + value + "'")
  private case object OGROUP extends Token("{")
  private case object CGROUP extends Token("}")
  private case object MATH extends Token("$")

  private def strToTokens(str : String) : Seq[Token] = {
    val l : Int = str.length
    val EOF : Char = Char.MinValue
    def charAt(j : Int) : Char = if(j >= 0 && j < l) str(j) else EOF

    var i : Int = 0 // position of "current" char
    
    var currentChar : Char = charAt(0)
    var nextChar    : Char = charAt(1)

    def upd {
      currentChar = charAt(i)
      nextChar    = charAt(i + 1)
    }

    def readOne : Unit = if(i < l) {
      i += 1
      upd
    }
    def read(c : Int) : Unit = {
      i += c
      if(i >= l) {
        i = l
      }
      upd
    }

    def isLetter(c : Char) : Boolean = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
    def isWhitespace(c : Char) : Boolean = c match {
      case ' ' | '\n' | '\r' | '\t' => true
      case _ => false
    }
    def isSpecial(c : Char) : Boolean = c match {
      case '\\' | '$' | '{' | '}' => true
      case _ if isWhitespace(c) => true
      case _ => false
    }

    def readWhile(p : Char=>Boolean) : String = {
      val here = i
      var c : Int = 0
      while(currentChar != EOF && p(currentChar)) {
        readOne
        c += 1
      }
      str.substring(here, here+c)
    }
    def readWhileLetters : String = readWhile(isLetter)
    def readWhileWhitespace : String = readWhile(isWhitespace)

    def nextToken : Token = currentChar match {
      case '$' => readOne; MATH
      case '{' => readOne; OGROUP
      case '}' => readOne; CGROUP

      case c if isWhitespace(c) => SPACE(readWhileWhitespace)

      case '\\' =>
        nextChar match {
          case '\\' | '{' | '}' | '_' | '$' | '&' | '%' | '#' =>
            val c = nextChar
            read(2)
            RAW(c.toString)

          case _    => 
            readOne
            val immediateNext = currentChar // This may or may not be a letter. We take one char anyway.
            if(isLetter(immediateNext)) {
              MACRO(readWhileLetters)
            } else {
              val oneFollowing = nextChar
              (immediateNext,oneFollowing) match {
                // This is where we recognize ugly macros for accents, such as \'e, etc.
                case (('\''|'`'|'^'|'"'), ('a'|'A'|'e'|'E'|'i'|'I'|'o'|'O'|'u'|'U'|'y'|'Y')) |
                     ('~', ('n'|'N')) =>
                  read(2)
                  MACRO(immediateNext.toString + oneFollowing.toString)
                case _ =>
                  readOne
                  MACRO(immediateNext.toString)
              }
            }
        }

      case c => RAW(readWhile(!isSpecial(_)))
    }

    val lb = new ListBuffer[Token]
    while(currentChar != EOF) {
      lb.append(nextToken)
    }

    lb.toList
  }
}
