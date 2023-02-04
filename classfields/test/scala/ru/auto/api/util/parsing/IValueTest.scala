package ru.auto.api.util.parsing

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 25.02.17
  */
class IValueTest extends BaseSpec with ScalaCheckPropertyChecks {
  "IValue" should {
    "help in pattern-matching" in {
      "123" match {
        case IValue(v) =>
          v shouldEqual 123
        case _ => fail()
      }

      "abc" match {
        case IValue(_) => fail()
        case _ => succeed
      }
    }
    "parse integer value from string" in {
      IValue("123") shouldBe Some(123)
      IValue("-123") shouldBe Some(-123)
      forAll(Gen.choose(Int.MinValue, Int.MaxValue))(value => IValue(value.toString) shouldBe Some(value))
    }
    "not parse non numeric strings" in {
      IValue("abc") shouldBe None
      forAll(Gen.alphaNumStr.suchThat(!_.matches("[0-9]+")))(value => IValue(value.toString) shouldBe None)
    }
  }
}
