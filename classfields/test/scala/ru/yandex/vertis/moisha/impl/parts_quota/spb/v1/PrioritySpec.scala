package ru.yandex.vertis.moisha.impl.parts_quota.spb.v1

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

    "return correct price for Spb" in {
      checkPolicy(correctInterval, priority(30.rubles), withAmount(1000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(57.rubles), withAmount(2000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(80.rubles), withAmount(3000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(108.rubles), withAmount(4000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(135.rubles), withAmount(5000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(240.rubles), withAmount(10000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(750.rubles), withAmount(50000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(1350.rubles), withAmount(100000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(3900.rubles), withAmount(500000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(6600.rubles), withAmount(1000000), inRegion(RegSPb))

      checkPolicy(correctInterval, priority(9000.rubles), withAmount(UnlimitedAmount), inRegion(RegSPb))
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
