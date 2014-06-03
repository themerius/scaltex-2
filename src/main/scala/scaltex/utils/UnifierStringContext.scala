package scaltex.utils

object StringContext {

  type Unify = Tuple3[String, List[String], List[String]]

  implicit class Unifier(val sc: StringContext) {
    def unify(args: Any*): Unify = {
      val exprResults = args.map(_.toString).toList
      val parts = sc.parts.toList
      (sc.s(args: _*), exprResults, parts)
    }
  }

}