package ru.yandex.realty.cost.plus.utils

import zio.test.Assertion
import zio.test.AssertionM.Render.param

object CustomAssertions {

  def hasSameElementsAndOrder[A](other: Seq[A]): Assertion[Seq[A]] =
    Assertion.assertion("hasSameElementsAndOrder")(param(other)) { actual =>
      if (actual.size != other.size) println(s"Expected size ${other.size}, but found ${actual.size}")

      other.size == actual.size && actual.zip(other).zipWithIndex.forall {
        case ((a, e), index) =>
          val res = a == e
          if (!res) println {
            s"""At position $index:
               |expected  $e,
               |but found $a""".stripMargin
          }
          res
      }
    }
}
