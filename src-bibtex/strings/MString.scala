package io.bibimbap
package strings

import java.text.Normalizer
import Normalizer.Form.{NFC,NFD}

/** A wrapper around strings to support converting to and from different formats. */
case class MString private(latex: Option[String] = None, java: Option[String] = None, ascii: Option[String] = None) {
  val isEmpty : Boolean = latex.map(_.isEmpty).getOrElse(java.map(_.isEmpty).getOrElse(ascii.map(_.isEmpty).getOrElse(true)))

  lazy val toLaTeX: String = latex.orElse(java.map(MString.javaToLaTeX)).orElse(ascii.map(MString.asciiToLaTeX)).get
  lazy val toJava:  String = java.orElse(latex.map(MString.latexToJava)).orElse(ascii.map(MString.asciiToJava)).get
  lazy val toASCII: String = ascii.orElse(java.map(MString.javaToASCII)).orElse(latex.map(MString.latexToASCII)).get

  override def toString = { throw new Exception("Don't use MString.toString, use toJava/toLaTeX/toASCII.") }
  
  assert(!latex.isEmpty || !java.isEmpty || !ascii.isEmpty)

  def mergeWith(other : MString) : MString = {
    assert(false, "Currently unused. Please write tests if you plan on using.")
    val newLaTeX = latex orElse other.latex
    val newJava  = java  orElse other.java
    val newASCII = ascii orElse other.ascii
    MString(latex = newLaTeX, java = newJava, ascii = newASCII) 
  }

  /** Concatenation of MStrings is valid as long as the right-hand side defines at least as many
    * encodings as the left-hand side. The result is an MString with the same encodings as the
    * left-hand side. */
  def +(other : MString) : MString = {
    if(isEmpty) {
      other
    } else if(other.isEmpty) {
      this
    } else {
      if(latex.isDefined && !other.latex.isDefined) {
        throw new IllegalArgumentException("RHS of MString concatenation must define LaTeX encoding when LHS does.")
      }

      if(java.isDefined && !other.java.isDefined) {
        throw new IllegalArgumentException("RHS of MString concatenation must define Java (UTF) encoding when LHS does.")
      }

      if(ascii.isDefined && !other.ascii.isDefined) {
        throw new IllegalArgumentException("RHS of MString concatenation must define ASCII encoding when LHS does.")
      }

      MString(
        latex = latex.map(l => l + other.latex.get),
        java  = java.map(j => j + other.java.get),
        ascii = ascii.map(a => a + other.ascii.get)
      )
    }
  }

  /** This is a rather dubious operation, because it will throw away one of multiple stored representations. */
  def split(sep : MString) : Seq[MString] = {
    if(latex.isDefined && sep.latex.isDefined) {
      latex.get.split(sep.latex.get).map(MString.fromLaTeX)
    } else if(java.isDefined && sep.java.isDefined) {
      java.get.split(sep.java.get).map(MString.fromJava)
    } else if(ascii.isDefined && sep.ascii.isDefined) {
      ascii.get.split(sep.ascii.get).map(MString.fromASCII)
    } else {
      throw new IllegalArgumentException("String and separator must share at least one encoding.")
    }
  }
}

object MString {
  private lazy val latexParser = new latex.LaTeXParser with latex.LaTeXElements

  val empty : MString = MString(latex = Some(""), java = Some(""), ascii = Some(""))
  val and : MString = MString(latex = Some(" and "), java = Some(" and "), ascii = Some(" and "))

  def fromJava(str : String)  = new MString(java = Some(Normalizer.normalize(str, NFC)))
  def fromLaTeX(str : String) = new MString(latex = Some(str))
  def fromASCII(str : String) = new MString(ascii = Some(str))

  def conjoin(strs : Seq[MString], sep : MString = and) : MString = {
    val sz = strs.size
    if(sz == 0) {
      empty
    } else if(sz == 1) {
      strs.head
    } else {
      var result = strs(0)
      var i = 1
      while(i < sz) {
        result = result + sep + strs(i)
        i += 1
      }
      result
    }
  }

  val mapJavaToLatex = Map[Char, Seq[Char]](
    '#' -> """{\#}""",
    'ç' -> """\c{c}""",
    'Ç' -> """\c{C}""",
    'á' -> """\'{a}""",
    'Á' -> """\'{A}""",
    'é' -> """\'{e}""",
    'É' -> """\'{E}""",
    'í' -> """\'{\i}""",
    'Í' -> """\'{I}""",
    'ó' -> """\'{o}""",
    'Ó' -> """\'{O}""",
    'ú' -> """\'{u}""",
    'Ú' -> """\'{U}""",
    'ý' -> """\'{y}""",
    'Ý' -> """\'{Y}""",
    'à' -> """\`{a}""",
    'À' -> """\`{A}""",
    'è' -> """\`{e}""",
    'È' -> """\`{E}""",
    'ì' -> """\`{\i}""",
    'Ì' -> """\`{I}""",
    'ò' -> """\`{o}""",
    'Ò' -> """\`{O}""",
    'ù' -> """\`{u}""",
    'Ù' -> """\`{U}""",
    'ỳ' -> """\`{y}""",
    'Ỳ' -> """\`{Y}""",
    'â' -> """\^{a}""",
    'Â' -> """\^{A}""",
    'ê' -> """\^{e}""",
    'Ê' -> """\^{E}""",
    'î' -> """\^{\i}""",
    'Î' -> """\^{I}""",
    'ô' -> """\^{o}""",
    'Ô' -> """\^{O}""",
    'û' -> """\^{u}""",
    'Û' -> """\^{U}""",
    'ŷ' -> """\^{y}""",
    'Ŷ' -> """\^{Y}""",
    'æ' -> """{\ae}""",
    'Æ' -> """{\AE}""",
    'å' -> """{\aa}""",
    'Å' -> """{\AA}""",
    'œ' -> """{\oe}""",
    'Œ' -> """{\OE}""",
    'ø' -> """{\o}""",
    'Ø' -> """{\O}""",
    'ä' -> """\"{a}""",
    'Ä' -> """\"{A}""",
    'ë' -> """\"{e}""",
    'Ë' -> """\"{E}""",
    'ï' -> """\"{i}""",
    'Ï' -> """\"{I}""",
    'ö' -> """\"{o}""",
    'Ö' -> """\"{O}""",
    'ü' -> """\"{u}""",
    'Ü' -> """\"{U}""",
    'ÿ' -> """\"{y}""",
    'Ÿ' -> """\"{Y}""",
    'α' -> """$\alpha$""",
    'β' -> """$\beta$""",
    'γ' -> """$\gamma$""",
    'δ' -> """$\delta$""",
    'ε' -> """$\varepsilon$""",
    'ϵ' -> """$\epsilon$""",
    'ζ' -> """$\zeta$""",
    'η' -> """$\eta$""",
    'θ' -> """$\theta$""",
    'ι' -> """$\iota$""",
    'κ' -> """$\kappa$""",
    'λ' -> """$\lambda$""",
    'μ' -> """$\mu$""",
    'ν' -> """$\nu$""",
    'ξ' -> """$\xi$""",
    'ο' -> """$\omicron$""",
    'π' -> """$\pi$""",
    'ρ' -> """$\rho$""",
    'ς' -> """$\varsigma$""",
    'σ' -> """$\sigma$""",
    'τ' -> """$\tau$""",
    'υ' -> "$\\upsilon$",
    'φ' -> """$\phi$""",
    'χ' -> """$\chi$""",
    'ψ' -> """$\psi$""",
    'ω' -> """$\omega$"""
  )

  def javaToLaTeX(str: String): String = {
    // FIXME : use intermediate form (LaTeX trees) to do this properly.
    str.flatMap(mapJavaToLatex.orElse({ case x => Seq(x) }))
  }

  def javaToASCII(str : String) = {
    def transliterate(c : Char) : Seq[Char] = c match {
      case 'Ä' | 'Æ' => "AE"
      case 'Å'       => "AA"
      case 'Ö' | 'Ø' => "OE"
      case 'Ü'       => "UE"
      case 'Þ'       => "TH"
      case 'ß'       => "ss"
      case 'ä' | 'æ' => "ae"
      case 'å'       => "aa"
      case 'ö' | 'ø' => "oe"
      case 'ü'       => "ue"
      case 'þ'       => "th"
      case 'α'       => "alpha"
      case 'β'       => "beta"
      case 'γ'       => "gamma"
      case 'δ'       => "delta"
      case 'ε' | 'ϵ' => "epsilon"
      case 'ζ'       => "zeta"
      case 'η'       => "eta"
      case 'θ'       => "theta"
      case 'ι'       => "iota"
      case 'κ'       => "kappa"
      case 'λ'       => "lambda"
      case 'μ'       => "mu"
      case 'ν'       => "nu"
      case 'ξ'       => "xi"
      case 'ο'       => "omicron"
      case 'π'       => "pi"
      case 'ρ'       => "rho"
      case 'ς' | 'σ' => "sigma"
      case 'τ'       => "tau"
      case 'υ'       => "upsilon"
      case 'φ'       => "phi"
      case 'χ'       => "chi"
      case 'ψ'       => "psi"
      case 'ω'       => "omega"
      case _         => Seq(c)
    }

    def removeDiacritics(s : String) : String = {
      Normalizer.normalize(s, NFD).replaceAll("""\p{InCombiningDiacriticalMarks}+""", "")
    }

    def isASCII(c : Char) : Boolean = (c >= ' ' && c <= '~')  

    removeDiacritics(str.flatMap(transliterate)).filter(isASCII).trim
  }

  def latexToJava(str: String): String = {
    val result = latexParser.parseOpt(str) map { elems =>
      latexParser.renderLaTeX(elems)
    } getOrElse {
      str
    }

    //if(result != str) {
    //  val stripped = str.replaceAll("""\{|\}""", "")
    //  if(result != stripped) {
    //    println("LaTeX to Java : \n  [%s]\n  [%s]".format(str,result))
    //  }
    //}    

    result
  }

  def asciiToJava(str: String): String = str

  def latexToASCII(str: String): String = (latexToJava _ andThen javaToASCII)(str)

  def asciiToLaTeX(str: String): String = (asciiToJava _ andThen javaToLaTeX)(str)
}
