package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class MathUtilSpec extends SpecBase {

  "ratioPercent" should {

    "correctly works for positive values" in {
      MathUtil.ratioPercent(50, 1000) shouldBe Success(5)
    }

    "correctly works if actual is negative" in {
      MathUtil.ratioPercent(-40, 1000) shouldBe Success(4)
    }

    "correctly works if expected is negative" in {
      MathUtil.ratioPercent(30, -1000) shouldBe Success(3)
    }

    "correctly works for zero expected" in {
      intercept[ArithmeticException] {
        MathUtil.ratioPercent(20, 0).get
      }
    }

    "correctly works for if zero actual and zero expected" in {
      intercept[ArithmeticException] {
        MathUtil.ratioPercent(0, 0).get
      }
    }

    "correctly rounds result down" in {
      MathUtil.ratioPercent(34, 1000) shouldBe Success(3)
    }

    "correctly rounds result up" in {
      MathUtil.ratioPercent(36, 1000) shouldBe Success(4)
    }
  }
}
