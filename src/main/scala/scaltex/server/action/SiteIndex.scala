package scaltex.server.action

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
