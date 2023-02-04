package ru.yandex.realty.sitemap.testkit

import zio.test._
import zio.test.Assertion._

object CustomAssertions {

  def seqEquals[A](expected: Seq[A], loggedPrefix: Option[String], preSort: Option[Ordering[A]]): Assertion[Seq[A]] =
    Assertion.assertion("seqEquals")(Render.param(expected)) { actual =>
      def log(s: => String): Unit = loggedPrefix.map(p => s"[$p] $s").foreach(println(_))

      def logExpectedActual(title: String, expected: Any, actual: Any): Unit = {
        log(title)
        log(s"expected: $expected")
        log(s"actual:   $actual")
      }

      val a = preSort.map(actual.sorted(_)).getOrElse(actual)
      val e = preSort.map(expected.sorted(_)).getOrElse(expected)

      logExpectedActual("Comparing", e, a)

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
