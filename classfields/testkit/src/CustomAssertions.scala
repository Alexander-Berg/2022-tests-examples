package ru.vertistraf.common.testkit

import zio.test._
import zio.test.Assertion._

object CustomAssertions {

  def seqEquals[A](expected: Seq[A], loggedPrefix: Option[String] = None): Assertion[Seq[A]] =
    Assertion.assertion("seqEquals")(Render.param(expected)) { actual =>
      def log(s: => String): Unit = loggedPrefix.map(p => s"[$p] $s").foreach(println(_))

      def logExpectedActual(title: String, expected: Any, actual: Any): Unit = {
        log(title)
        log(s"expected: $expected")
        log(s"actual:   $actual")
      }

      val a = actual
      val e = expected

      (0 until math.max(a.size, e.size)).foldLeft(true) {
        case (false, _) => false
        case (true, index) if index >= a.size || index >= e.size =>
          logExpectedActual("different sizes", e.size, a.size)
          false
        case (true, index) =>
          if (e(index) != a(index)) {
            logExpectedActual(s"Not equal at $index index", e(index), a(index))
            false
          } else {
            true
          }
      }

    }
}
