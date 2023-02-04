package ru.yandex.vertis.moisha.impl.autoru_quota.v2.msk_lux.v1

import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model.Funds
import ru.yandex.vertis.moisha.util.GeoIds._

import ru.yandex.vertis.moisha.impl.autoru_quota.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru_quota.model._
import ru.yandex.vertis.moisha.impl.autoru_quota.utils.UnlimitedAmount

trait PlacementCarsNewPremiumSpec extends SingleProductDailyPolicySpec {

  import PlacementCarsNewPremiumSpec._

  "PlacementCarsNewPremium policy" should {
    "support product PlacementCarsNewPremium" in {
      policy.products.contains(Products.PlacementCarsNewPremium.toString) shouldBe true
    }

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, placement(3400.rubles), withAmount(UnlimitedAmount), inRegion(RegMoscow))
    }
  }
}

object PlacementCarsNewPremiumSpec {

  def placement(price: Funds): AutoRuQuotaProduct =
    AutoRuQuotaProduct(
      Products.PlacementCarsNewPremium,
      Set(
        AutoRuQuotaGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      )
    )
}
