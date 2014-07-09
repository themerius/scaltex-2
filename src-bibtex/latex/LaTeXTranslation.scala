package io.bibimbap
package latex

trait LaTeXTranslation {
  self : LaTeXElements =>

  def translate(macroName : String, args : Seq[Group]) : Option[String] = {
    val argc = args.size

    val foundSoFar = if(argc == 0) {
      nullary.get(macroName)
    } else if(argc == 1) {
      args(0).elems match {
        case Seq() => nullary.get(macroName)
        case Seq(RawString(v)) => unary.get((macroName, v))
        case _ => None
      }
    } else if(argc == 2) {
      (args(0).elems, args(1).elems) match {
        case (Seq(RawString(v1)), Seq(RawString(v2))) => binary.get((macroName, v1, v2))
        case _ => None
      }
    } else {
      None
    }
    
    foundSoFar.orElse {
      defs.get(macroName, argc).flatMap(f => f(args))
    }
  }

  private val nullary : Map[String,String] = Map(
    "LaTeX"      -> "LaTeX",
    "TeX"        -> "TeX",
    " "          -> " ",
    "'a"         -> "á",
    "'A"         -> "Á",
    "'e"         -> "é",
    "'E"         -> "É",
    "'i"         -> "í", // this is technically speaking incorrect
    "'I"         -> "Í",
    "'o"         -> "ó",
    "'O"         -> "Ó",
    "'u"         -> "ú",
    "'U"         -> "Ú",
    "'y"         -> "ý",
    "'Y"         -> "Ý",
    "`a"         -> "à",
    "`A"         -> "À",
    "`e"         -> "è",
    "`E"         -> "È",
    "`i"         -> "ì", // see above
    "`I"         -> "Ì",
    "`o"         -> "ò",
    "`O"         -> "Ò",
    "`u"         -> "ù",
    "`U"         -> "Ù",
    "`y"         -> "ỳ",
    "`Y"         -> "Ỳ",
    "^a"         -> "â",
    "^A"         -> "Â",
    "^e"         -> "ê",
    "^E"         -> "Ê",
    "^i"         -> "î", // see above
    "^I"         -> "Î",
    "^o"         -> "ô",
    "^O"         -> "Ô",
    "^u"         -> "û",
    "^U"         -> "Û",
    "^y"         -> "ŷ",
    "^Y"         -> "Ŷ",
    "\"a"        -> "ä",
    "\"A"        -> "Ä",
    "\"e"        -> "ë",
    "\"E"        -> "Ë",
    "\"i"        -> "ï", // see above
    "\"I"        -> "Ï",
    "\"o"        -> "ö",
    "\"O"        -> "Ö",
    "\"u"        -> "ü",
    "\"U"        -> "Ü",
    "\"y"        -> "ÿ",
    "\"Y"        -> "Ÿ",
    "~a"         -> "ã",
    "~A"         -> "Ã",
    "~n"         -> "ñ",
    "~N"         -> "Ñ",
    "ae"         -> "æ",
    "AE"         -> "Æ",
    "aa"         -> "å",
    "AA"         -> "Å",
    "oe"         -> "œ",
    "OE"         -> "Œ",
    "o"          -> "ø",
    "O"          -> "Ø",
    "ss"         -> "ß",
    "i"          -> "i"
  ) ++ UnicodeSymbols.macroToSymbol

  private val unary : Map[(String,String),String] = Map(
    ("c", "c") -> "ç",
    ("c", "C") -> "Ç",
    ("'", "a") -> "á",
    ("'", "A") -> "Á",
    ("'", "e") -> "é",
    ("'", "E") -> "É",
    ("'", "i") -> "í",
    ("'", "I") -> "Í",
    ("'", "o") -> "ó",
    ("'", "O") -> "Ó",
    ("'", "u") -> "ú",
    ("'", "U") -> "Ú",
    ("'", "y") -> "ý",
    ("'", "Y") -> "Ý",
    ("'", "c") -> "ć",
    ("'", "C") -> "Ć",
    ("`", "a") -> "à",
    ("`", "A") -> "À",
    ("`", "e") -> "è",
    ("`", "E") -> "È",
    ("`", "i") -> "ì",
    ("`", "I") -> "Ì",
    ("`", "o") -> "ò",
    ("`", "O") -> "Ò",
    ("`", "u") -> "ù",
    ("`", "U") -> "Ù",
    ("`", "y") -> "ỳ",
    ("`", "Y") -> "Ỳ",
    ("^", "a") -> "â",
    ("^", "A") -> "Â",
    ("^", "e") -> "ê",
    ("^", "E") -> "Ê",
    ("^", "i") -> "î",
    ("^", "I") -> "Î",
    ("^", "o") -> "ô",
    ("^", "O") -> "Ô",
    ("^", "u") -> "û",
    ("^", "U") -> "Û",
    ("^", "y") -> "ŷ",
    ("^", "Y") -> "Ŷ",
    ("\"", "a") -> "ä",
    ("\"", "A") -> "Ä",
    ("\"", "e") -> "ë",
    ("\"", "E") -> "Ë",
    ("\"", "i") -> "ï",
    ("\"", "I") -> "Ï",
    ("\"", "o") -> "ö",
    ("\"", "O") -> "Ö",
    ("\"", "u") -> "ü",
    ("\"", "U") -> "Ü",
    ("\"", "y") -> "ÿ",
    ("\"", "Y") -> "Ÿ",
    ("~", "a") -> "ã",
    ("~", "A") -> "Ã",
    ("~", "n") -> "ñ",
    ("~", "N") -> "Ñ",
    ("u", "a") -> "ă",
    ("u", "A") -> "Ă",
    ("u", "e") -> "ĕ",
    ("u", "E") -> "Ĕ",
    ("u", "g") -> "ğ",
    ("u", "G") -> "Ğ",
    ("u", "i") -> "ĭ",
    ("u", "I") -> "Ĭ",
    ("u", "o") -> "ŏ",
    ("u", "O") -> "Ŏ",
    ("u", "u") -> "ŭ",  
    ("u", "U") -> "Ŭ",
    ("v", "a") -> "ǎ",
    ("v", "A") -> "Ǎ",
    ("v", "c") -> "č",
    ("v", "C") -> "Č",
    ("v", "e") -> "ě",
    ("v", "E") -> "Ě",
    ("v", "i") -> "ǐ",
    ("v", "I") -> "Ǐ",
    ("v", "o") -> "ǒ",
    ("v", "O") -> "Ǒ",
    ("v", "s") -> "š",
    ("v", "S") -> "Š",
    ("v", "u") -> "ǔ",
    ("v", "U") -> "Ǔ",
    ("v", "z") -> "ž",
    ("v", "Z") -> "Ž"
  )

  private val binary : Map[(String,String,String),String] = Map(
    ("frac", "1", "2") -> "¼",
    ("frac", "1", "4") -> "½",
    ("frac", "3", "4") -> "¾",
    ("frac", "1", "3") -> "⅓",
    ("frac", "1", "5") -> "⅕",
    ("frac", "1", "6") -> "⅙",
    ("frac", "1", "7") -> "⅐",
    ("frac", "1", "8") -> "⅛",
    ("frac", "1", "9") -> "⅑",
    ("frac", "1", "10") -> "⅒",
    ("frac", "2", "3") -> "⅔",
    ("frac", "2", "5") -> "⅖",
    ("frac", "3", "5") -> "⅗",
    ("frac", "3", "8") -> "⅜",
    ("frac", "4", "5") -> "⅘",
    ("frac", "5", "6") -> "⅚",
    ("frac", "5", "8") -> "⅝",
    ("frac", "7", "8") -> "⅞"
  )

  private val defs : Map[(String,Int),Seq[Group]=>Option[String]] = Map(
    ("mathbb", 1) -> ((s : Seq[Group]) => intoFont(s, mathbb)),
    ("mathcal", 1) -> ((s : Seq[Group]) => intoFont(s, mathcal))
  )

  private def intoFont(ss : Seq[Group], font : Array[String]) : Option[String] = ss match {
    case Seq(Group(Seq(RawString(str)))) => Some(str.flatMap(intoFont(_, font)))
    case _ => None
  }

  private def intoFont(c : Char, font : Array[String]) : String = {
    if(c >= '0' && c <= '9') {
      font(c - '0')
    } else if(c >= 'A' && c <= 'Z') {
      font((c - 'A') + 10)
    } else if(c >= 'a' && c <= 'z') {
      font((c - 'a') + 36)
    } else {
      c.toString
    }
  }

  private val mathbb : Array[String] = Array("𝟘", "𝟙", "𝟚", "𝟛", "𝟜", "𝟝", "𝟞", "𝟟", "𝟠", "𝟡", "𝔸", "𝔹", "ℂ", "𝔻", "𝔼", "𝔽", "𝔾", "ℍ", "𝕀", "𝕁", "𝕂", "𝕃", "𝕄", "ℕ", "𝕆", "ℙ", "ℚ", "ℝ", "𝕊", "𝕋", "𝕌", "𝕍", "𝕎", "𝕏", "𝕐", "ℤ", "𝕒", "𝕓", "𝕔", "𝕕", "𝕖", "𝕗", "𝕘", "𝕙", "𝕚", "𝕛", "𝕜", "𝕝", "𝕞", "𝕟", "𝕠", "𝕡", "𝕢", "𝕣", "𝕤", "𝕥", "𝕦", "𝕧", "𝕨", "𝕩", "𝕪", "𝕫")

  private val mathcal : Array[String] = Array("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "𝓐", "𝓑", "𝓒", "𝓓", "𝓔", "𝓕", "𝓖", "𝓗", "𝓘", "𝓙", "𝓚", "𝓛", "𝓜", "𝓝", "𝓞", "𝓟", "𝓠", "𝓡", "𝓢", "𝓣", "𝓤", "𝓥", "𝓦", "𝓧", "𝓨", "𝓩", "𝓪", "𝓫", "𝓬", "𝓭", "𝓮", "𝓯", "𝓰", "𝓱", "𝓲", "𝓳", "𝓴", "𝓵", "𝓶", "𝓷", "𝓸", "𝓹", "𝓺", "𝓻", "𝓼", "𝓽", "𝓾", "𝓿", "𝔀", "𝔁", "𝔂", "𝔃")
}
