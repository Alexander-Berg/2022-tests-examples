package ru.yandex.vertis.billing.settings.discount

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.PercentDiscount

/**
  * Spec on [[RealtyCommercialDiscountPolicy]]
  *
  * @author ruslansd
  */
class RealtyCommercialDiscountPolicySpec extends AnyWordSpec with Matchers {

  private val Policy = RealtyCommercialDiscountPolicy

  "RealtyCommercialDiscountPolicy" should {

    "correctly provide discount" in {
      Policy.amount(0) shouldBe PercentDiscount(0)
      Policy.amount(1) shouldBe PercentDiscount(0)
      Policy.amount(999) shouldBe PercentDiscount(0)

      Policy.amount(1000) shouldBe PercentDiscount(15000)
      Policy.amount(2000) shouldBe PercentDiscount(15000)
      Policy.amount(2999) shouldBe PercentDiscount(15000)

      Policy.amount(3000) shouldBe PercentDiscount(30000)
      Policy.amount(5000) shouldBe PercentDiscount(30000)
      Policy.amount(5999) shouldBe PercentDiscount(30000)

      Policy.amount(6000) shouldBe PercentDiscount(50000)
      Policy.amount(6001) shouldBe PercentDiscount(50000)
      Policy.amount(10000) shouldBe PercentDiscount(50000)

    }
  }

}
