package scaltex.utils

import scaltex.RootActor
import akka.actor.ActorRef

class ParentPreviousHelper(val ref: ActorRef, topology: Map[String, Map[String, String]]) {

  val id = ref.path.name

  def next(id: String) = if (topology.contains(id)) topology(id)("next") else ""
  def firstChild(id: String) = if (topology.contains(id)) topology(id)("firstChild") else ""
  def next: String = next(id)
  def firstChild: String = firstChild(id)

  def parent = ref.path.parent.name
  def previous = {
    val elemPrev = topology.filter(_._2("next") == id)
    if (elemPrev.nonEmpty) elemPrev.keys.head else ""
  }

  def previousNext = next(previous)
  def previousFirstChild = firstChild(previous)

  def parentNext = next(parent)
  def parentFirstChild = firstChild(parent)

}