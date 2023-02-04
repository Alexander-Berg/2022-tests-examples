package ru.yandex.vertis.moisha.model

import org.scalatest.{Matchers, WordSpecLike}

/**
  * Specs on [[FundsConversions]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class FundsConversionsSpec extends Matchers with WordSpecLike {

  import ru.yandex.vertis.moisha.model.FundsConversions.FundsLong

  "FundsConversions" should {
    "make cents" in {
      25.cents should be(25)
      35L.cents should be(35)
      1.cent should be(1)
      1L.cent should be(1)
    }

    "make rubles" in {
      2.rubles should be(2 * 100)
      4L.rubles should be(4 * 100)
      1.ruble should be(1 * 100)
      1L.ruble should be(1 * 100)
    }

    "make thousands of rubles" in {
      3.thousands should be(3 * 1000 * 100)
      5L.thousands should be(5 * 1000 * 100)
      1.thousand should be(1 * 1000 * 100)
      1L.thousand should be(1 * 1000 * 100)
    }

    "react to overflow" in {
      intercept[ArithmeticException] {
        Long.MaxValue.thousands
      }
      intercept[ArithmeticException] {
        Long.MaxValue.rubles
      }
    }
  }

}
