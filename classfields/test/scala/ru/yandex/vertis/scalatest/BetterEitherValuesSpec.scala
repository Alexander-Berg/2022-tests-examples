package ru.yandex.vertis.scalatest

import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.scalatest.BetterEitherValues._

class BetterEitherValuesSpec extends WordSpec with Matchers {

  "BetterEitherValues.value" should {

    "get right.right value" in {
      Right(42).right.value shouldBe 42
    }

    "get left.left value" in {
      Left(42).left.value shouldBe 42
    }

    "contain Right() value in Right().left.value exception message" in {
      val e = intercept[TestFailedException](Right(42).left.value)
      e.getMessage.contains("Right(42)") shouldBe true
    }

    "contain Left() value in Left().right.value exception message" in {
      val e = intercept[TestFailedException](Left(42).right.value)
      e.getMessage.contains("Left(42)") shouldBe true
    }
  }
}
