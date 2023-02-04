package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

/**
  * Created by andrey on 1/12/17.
  */
@RunWith(classOf[JUnitRunner])
class BadgeUtilsTest extends AnyFunSuite {

  test("normalize") {
    val badges = List("стикер", "  стикер   ", " сТиКеР", "второй стикер", "  стикер   ")
    val stickersDistinct = badges.distinct
    assert(stickersDistinct.length == 4, "normalized badges length must be 2")
    assert(stickersDistinct.contains("стикер"))
    assert(stickersDistinct.contains("  стикер   "))
    assert(stickersDistinct.contains(" сТиКеР"))
    assert(stickersDistinct.contains("второй стикер"))
  }
}
