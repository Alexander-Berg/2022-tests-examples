package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Specs on [[FundsConversions]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class FundsConversionsSpec extends AnyWordSpec with Matchers {

  import ru.yandex.vertis.billing.model_core.FundsConversions._

  "FundsConversions" should {
    "make cents" in {
      25.cents should be(25)
      35L.cents should be(35)
      1.cent should be(1)
      1L.cent should be(1)
      -1L.cent should be(-1)
      1234567890.cent should be(1234567890)
      0.cent should be(0)
    }
    "make rubles" in {
      2.rubles should be(2 * 100)
      4L.rubles should be(4 * 100)
      1.ruble should be(1 * 100)
      1L.ruble should be(1 * 100)
      -1L.ruble should be(-100)
      1234567890.rubles should be(123456789000L)
      0.rubles should be(0)
    }
    "make thousands" in {
      3.thousands should be(3 * 1000 * 100)
      5L.thousands should be(5 * 1000 * 100)
      1.thousand should be(1 * 1000 * 100)
      1L.thousand should be(1 * 1000 * 100)
      -1L.thousand should be(-1000 * 100)
      1234567890.thousands should be(123456789000000L)
      0.thousands should be(0)
    }
    "make millions" in {
      3.millions should be(3 * 1000 * 1000 * 100)
      5L.millions should be(5 * 1000 * 1000 * 100)
      1.million should be(1 * 1000 * 1000 * 100)
      1L.million should be(1 * 1000 * 1000 * 100)
      -1L.million should be(-1000 * 1000 * 100)
      1234567890.millions should be(123456789000000L * 1000)
      0.millions should be(0)
    }
    "react to overflow" in {
      intercept[ArithmeticException] {
        Long.MaxValue.millions
      }
      intercept[ArithmeticException] {
        Long.MaxValue.rubles
      }
    }
  }

}
