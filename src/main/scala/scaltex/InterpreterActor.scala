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
    case Interpret(code, names) => {
      var ret: Object = null

      var escapedCode = code
      val regex = "\\\\[^btnfr\"]".r
      val matches = regex.findAllIn(escapedCode).toList
      val chars = matches.map(_.last)
      for ( (x,y) <- matches zip chars) {
        if (y == ' ')
          escapedCode = escapedCode.replace(x, " ")
        else
          escapedCode = escapedCode.replace(x, "\\\\" + y)
      }

      try {
        this.imain.beQuietDuring {
          ret = this.imain.eval(escapedCode)
        }
      } catch {
        case e: javax.script.ScriptException => ret = None
        case e: scala.StringContext.InvalidEscapeException =>
          println("Interpreter: Invalid Escape Exception", e)
          ret = None
      }
      this.imain.reset
      sender ! ReturnValue(ret, names)
    }
  }

}
