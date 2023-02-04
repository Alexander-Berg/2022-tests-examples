package ru.yandex.realty

import org.junit.runner.RunWith
import org.scalatest.exceptions.{TestCanceledException, TestFailedException}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SpecBaseSpec extends Matchers with WordSpecLike {

  private val spec = new SpecBase {}

  "SpecBase" should {
    "do not call action twice in intercept" in {
      var counter = 0
      spec.intercept[RuntimeException] {
        counter += 1
        throw new RuntimeException
      }
      counter shouldBe 1
    }

    "do not intercept scalatest exceptions" in {
      val ex1 = intercept[TestFailedException] {
        val inner = spec.intercept[TestFailedException] {
          throw new TestFailedException("artificial", 0)
        }
        fail(s"SpecBase.intercept handled exception $inner")
      }
      ex1.getMessage().contains("artificial") shouldBe true

      val ex2 = intercept[TestCanceledException] {
        val inner = spec.intercept[TestCanceledException] {
          throw new TestCanceledException("artificial", 0)
        }
        fail(s"SpecBase.intercept handled exception $inner")
      }
      ex2.getMessage().contains("artificial") shouldBe true
    }

    "intercept non-scalatest exceptions" in {
      spec.intercept[RuntimeException] {
        throw new RuntimeException("artificial")
      }
    }
  }
}
