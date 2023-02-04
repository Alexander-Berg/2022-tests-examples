package ru.yandex.auto.util

import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Assertion, FlatSpec}

class RGBUtilsSpec extends FlatSpec {

  "RGB" should "convert to int" in {

    assert(RGB.convert("000000") == 0)
    assert(RGB.convert("00000") == -1)
    assert(RGB.convert("") == -1)

    goodForString("FFFFFF")
    goodForString("AAAAAA")
    goodForString("000001")
    goodForString("001001")
    goodForString("101001")

  }

  "RGB" should "fail for invalid cases" in {
    assertThrows[TestFailedException](goodForString("ZZZZZZ"))
    assertThrows[TestFailedException](goodForString("FFFFFFF"))
    assertThrows[TestFailedException](goodForString("FFFFFG"))

  }

  private def goodForString(str: String): Assertion = {
    assert(RGB.convert(RGB.convert(str)).contains(str))
  }
}
