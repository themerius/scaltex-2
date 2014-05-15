package scaltex

import akka.actor.Actor

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain

import Messages._

class InterpreterActor extends Actor {

  val settings = new Settings
  settings.usejavacp.value = false
  settings.embeddedDefaults[InterpreterActor] // to make imain useable within sbt.

  val imain = new IMain(settings)

  // warm up the interpreter:
  imain.eval("val x = 1")
  imain.reset

  def receive = {
    case Interpret(code, returnId) => {
      var ret: Object = null
      try {
        this.imain.beSilentDuring {
          ret = this.imain.eval(code)
        }
      } catch {
        case e: javax.script.ScriptException => ret = code
      }
      this.imain.reset
      sender ! ReturnValue(ret)
    }
  }

}
