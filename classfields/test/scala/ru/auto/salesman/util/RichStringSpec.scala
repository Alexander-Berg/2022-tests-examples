package ru.auto.salesman.util

import ru.auto.salesman.test.BaseSpec

class RichStringSpec extends BaseSpec {

  "isLower" should {

    "return true for full-lower-case string" in {
      "abcd".isLower shouldBe true
    }

    "return false for strings with upper-case letters" in {
      List("Abcd", "abC", "aBcde").foreach(_.isLower shouldBe false)
    }
  }

  "lowerToUpper" should {

    "return upper-cased for lower-case string" in {
      "abcd".lowerToUpper.value shouldBe "ABCD"
    }

    "return None for strings with upper-case letters" in {
      List("abcD", "Abc", "abcDe").foreach(_.lowerToUpper shouldBe None)
    }
  }
}
