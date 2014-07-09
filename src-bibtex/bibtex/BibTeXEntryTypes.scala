package io.bibimbap
package bibtex

import io.bibimbap.strings._

object BibTeXEntryTypes extends Enumeration {
  type BibTeXEntryType = Value
  val Article =       Value("article")
  val Book =          Value("book")
  val Booklet =       Value("booklet")
  val InBook =        Value("inbook")
  val InCollection =  Value("incollection")
  val InProceedings = Value("inproceedings")
  val Manual =        Value("manual")
  val MastersThesis = Value("mastersthesis")
  val Misc =          Value("misc")
  val PhDThesis =     Value("phdthesis")
  val Proceedings =   Value("proceedings")
  val TechReport =    Value("techreport")
  val Unpublished =   Value("unpublished")

  def withNameOpt(name: Option[String]): Option[BibTeXEntryType] =
    name.map(withNameOpt(_)).flatten

  def withNameOpt(name: String): Option[BibTeXEntryType] = try {
    Some(withName(name))
  } catch {
    case e: Throwable =>
      None
  }

  case class OneOf(fs: String*) {
    val set = fs.toSet
    def satisfiedBy(fields: Set[String]): Boolean = !(set & fields).isEmpty

    def toFields = fs.toList
  }

  import language.implicitConversions
  implicit def strToOneOf(str: String) = OneOf(str)

  val requiredFieldsFor = Map[BibTeXEntryType, List[OneOf]](
    Article         -> List("title", "author", "journal", "year"),
    Book            -> List("title", OneOf("author", "editor"), "publisher", "year"),
    Booklet         -> List("title"),
    InBook          -> List("title", OneOf("author", "editor"), OneOf("chapter", "pages"), "publisher", "year"),
    InCollection    -> List("title", "author", "booktitle", "year"),
    InProceedings   -> List("title", "author", "booktitle", "year"),
    Manual          -> List("title"),
    MastersThesis   -> List("title", "author", "school", "year"),
    Misc            -> List(),
    PhDThesis       -> List("title", "author", "school", "year"),
    Proceedings     -> List("title", "year"),
    TechReport      -> List("title", "author", "institution", "year"),
    Unpublished     -> List("title", "author", "note")
  ).withDefaultValue(List())

  def requiredFieldsFor(otpe: Option[BibTeXEntryType]): List[OneOf] = otpe.map(requiredFieldsFor).getOrElse(List())

  val optionalFieldsFor = Map(
    Article         -> List("volume", "number", "pages", "month", "note", "key"),
    Book            -> List("volume", "series", "address", "edition", "month", "note", "key", "pages"),
    Booklet         -> List("author", "howpublished", "address", "month", "year", "note", "key"),
    InBook          -> List("volume", "series", "address", "edition", "month", "note", "key"),
    InCollection    -> List("editor", "volume", "number", "series", "type", "chapter", "pages", "address", "edition", "month", "note", "key"),
    InProceedings   -> List("editor", "volume", "number", "series", "pages", "address", "month", "organization", "publisher", "note", "key"),
    Manual          -> List("author", "organization", "edition", "address", "year", "month", "note", "key"),
    MastersThesis   -> List("address", "month", "note", "key"),
    Misc            -> List("author", "howpublished", "title", "month", "year", "note", "key"),
    PhDThesis       -> List("address", "month", "note", "key"),
    Proceedings     -> List("editor", "volume", "number", "series", "address", "month", "publisher", "organization", "note", "key"),
    TechReport      -> List("type", "number", "address", "month", "note", "key"),
    Unpublished     -> List("month", "year", "key")
  ).withDefaultValue(List())

  def optionalFieldsFor(otpe: Option[BibTeXEntryType]): List[String] = otpe.map(optionalFieldsFor).getOrElse(List())

  def relevantFieldsFor(otpe: Option[BibTeXEntryType]): List[String] =
    requiredFieldsFor(otpe).flatMap(_.toFields) ++ optionalFieldsFor(otpe)

  val allStdFields = Set("address", "abstract", "annote", "author",
      "booktitle", "chapter", "crossref", "edition", "editor", "eprint",
      "howpublished", "institution", "journal", "key", "month", "note", "number",
      "organization", "pages", "publisher", "school", "series", "title", "type",
      "url", "volume", "year")
}
