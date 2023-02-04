package ru.yandex.vos2.autoru.utils

import org.scalatest.funsuite.AnyFunSuite
import TraversableUtils._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TraversableUtilsTest extends AnyFunSuite {
  test("implode strings") {
    assert(Seq("one").implode(", ", " and ") == "one")
    assert(Seq("one", "two").implode(", ", " and ") == "one and two")
    assert(Seq("one", "two", "three").implode(", ", " and ") == "one, two and three")
  }
}
