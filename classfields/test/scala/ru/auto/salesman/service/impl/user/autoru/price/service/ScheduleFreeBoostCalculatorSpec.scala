package ru.auto.salesman.service.impl.user.autoru.price.service

import ru.auto.salesman.model.ProductDuration
import ru.auto.salesman.model.ProductDuration.SevenDays
import ru.auto.salesman.model.UserSellerType.{Reseller, Usual}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.{Placement, Top}
import ru.auto.salesman.test.BaseSpec

class ScheduleFreeBoostCalculatorSpec extends BaseSpec {

  private val SixtyDays = ProductDuration.days(60)

  "ScheduleFreeBoostCalculator" should {
    "return true for 7-day reseller placement prolongation" in {
      ScheduleFreeBoostCalculator.shouldScheduleFreeBoost(
        Placement,
        SevenDays,
        userType = Reseller,
        useProlongationTariff = true
      ) shouldBe true
    }

    "return false for 60-day reseller placement prolongation" in {
      ScheduleFreeBoostCalculator.shouldScheduleFreeBoost(
        Placement,
        SixtyDays,
        userType = Reseller,
        useProlongationTariff = true
      ) shouldBe false
    }

    "return false for 7-day usual-user placement prolongation" in {
      ScheduleFreeBoostCalculator.shouldScheduleFreeBoost(
        Placement,
        SevenDays,
        userType = Usual,
        useProlongationTariff = true
      ) shouldBe false
    }

    "return false for 7-day reseller top prolongation" in {
      ScheduleFreeBoostCalculator.shouldScheduleFreeBoost(
        Top,
        SevenDays,
        userType = Reseller,
        useProlongationTariff = true
      ) shouldBe false
    }

    "return false when not using prolongation tariff" in {
      ScheduleFreeBoostCalculator.shouldScheduleFreeBoost(
        Placement,
        SevenDays,
        userType = Reseller,
        useProlongationTariff = false
      ) shouldBe false
    }
  }

}
