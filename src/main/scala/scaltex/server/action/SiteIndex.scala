package scaltex.server.action

import xitrum.Action
import xitrum.annotation.GET

@GET("")
class Root extends DefaultLayout {
  def execute() {
    respondView()
  }
}

@GET("meta")
class Meta extends DefaultLayout {
  def execute() {
    respondView()
  }
}

@GET("latex")
class Latex extends DefaultLayout {
  def execute() {
    respondView()
  }
}

@GET("switch")
class Switch extends Action {
  def execute() {
    scaltex.BaseActor.partialUpdate = !scaltex.BaseActor.partialUpdate
    respondText(s"Partial update flag reverted. Now ${scaltex.BaseActor.partialUpdate}. Messages ${scaltex.BaseActor.count}.")
  }
}