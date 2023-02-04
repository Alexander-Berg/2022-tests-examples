package ru.yandex.vertis.moisha.impl.parts_quota.msk.v1

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

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, priority(33.rubles), withAmount(1000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(63.rubles), withAmount(2000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(88.rubles), withAmount(3000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(120.rubles), withAmount(4000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(150.rubles), withAmount(5000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(267.rubles), withAmount(10000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(833.rubles), withAmount(50000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(1500.rubles), withAmount(100000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(4333.rubles), withAmount(500000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(7333.rubles), withAmount(1000000), inRegion(RegMoscow))

      checkPolicy(correctInterval, priority(10000.rubles), withAmount(UnlimitedAmount), inRegion(RegMoscow))
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
