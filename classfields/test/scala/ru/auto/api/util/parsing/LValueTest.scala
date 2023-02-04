package ru.auto.api.util.parsing

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 25.02.17
  */
class LValueTest extends BaseSpec with ScalaCheckPropertyChecks {
  "LValue" should {
    "help in pattern-matching" in {
      "123" match {
        case LValue(v) =>
          v shouldEqual 123L
        case _ => fail()
      }

      "abc" match {
        case LValue(_) => fail()
        case _ => succeed
      }
    }
    "parse long value from string" in {
      LValue("123") shouldBe Some(123)
      LValue("-123") shouldBe Some(-123)
      LValue("-9223372036854775808") shouldBe Some(Long.MinValue)
      forAll(Gen.choose(Long.MinValue, Long.MaxValue))(value => LValue(value.toString) shouldBe Some(value))
    }
    "not parse non numeric strings" in {
      LValue("abc") shouldBe None
      forAll(Gen.alphaNumStr.suchThat(!_.matches("[0-9]+")))(value => LValue(value.toString) shouldBe None)
    }
  }
}
