package scaltex

object Messages {

  case object Update
  case object State
  case object Delete

  // used within document meta model algorithm
  case class M(to: Seq[String], jsonMsg: String)

  // to trigger actor ref operations
  case class Previous(id: String)
  case class Next(id: String)
  case class FirstChild(id: String)
  case class Parent(id: String)
  case class InsertBetween(idx: String, idy: String)
  case class RemoveBetween(idx: String, idy: String)

  case class Change(to: String)

  case class AddStateProperty(json: String)
  case class RemoveStateProperty(key: String)

}