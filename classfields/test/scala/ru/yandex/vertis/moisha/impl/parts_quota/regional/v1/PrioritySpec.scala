package ru.yandex.vertis.moisha.impl.parts_quota.regional.v1

import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model.Funds
import ru.yandex.vertis.moisha.util.GeoIds._

import ru.yandex.vertis.moisha.impl.parts_quota.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.parts_quota.model._
import ru.yandex.vertis.moisha.impl.parts_quota.utils.UnlimitedAmount

trait PrioritySpec extends SingleProductDailyPolicySpec {

  import PrioritySpec._

  "Priority policy" should {
    "support product Priority" in {
      policy.products.contains(Products.Priority.toString) shouldBe true
    }

    "return correct price for regions" in {
      val someRegions = List(RegKaluga, RegSmolensk, RegKrasnodar, RegKrasnoyarsk, RegNovosibirsk)

      for (region <- someRegions) {
        checkPolicy(correctInterval, priority(10.rubles), withAmount(1000), inRegion(region))

        checkPolicy(correctInterval, priority(19.rubles), withAmount(2000), inRegion(region))

        checkPolicy(correctInterval, priority(27.rubles), withAmount(3000), inRegion(region))

        checkPolicy(correctInterval, priority(36.rubles), withAmount(4000), inRegion(region))

        checkPolicy(correctInterval, priority(45.rubles), withAmount(5000), inRegion(region))

        checkPolicy(correctInterval, priority(80.rubles), withAmount(10000), inRegion(region))

        checkPolicy(correctInterval, priority(250.rubles), withAmount(50000), inRegion(region))

        checkPolicy(correctInterval, priority(450.rubles), withAmount(100000), inRegion(region))

        checkPolicy(correctInterval, priority(1300.rubles), withAmount(500000), inRegion(region))

        checkPolicy(correctInterval, priority(2200.rubles), withAmount(1000000), inRegion(region))

        checkPolicy(correctInterval, priority(3000.rubles), withAmount(UnlimitedAmount), inRegion(region))
      }
    }
  }
}

object PrioritySpec {

  def priority(price: Funds): PartsQuotaProduct =
    PartsQuotaProduct(
      Products.Priority,
      Set(
        PartsQuotaGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      )
    )
}
