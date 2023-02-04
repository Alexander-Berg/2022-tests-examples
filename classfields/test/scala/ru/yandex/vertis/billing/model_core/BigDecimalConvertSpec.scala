package ru.yandex.vertis.billing.model_core

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/**
  * Specs on [[BigDecimal]]
  *
  * @author alesavin
  */
class BigDecimalConvertSpec extends AnyWordSpec with Matchers {

  "BigDecimal" should {
    "convert to long" in {
      (BigDecimal("16801.333333") * 100).toLong should be(1680133L)
    }
  }
}
