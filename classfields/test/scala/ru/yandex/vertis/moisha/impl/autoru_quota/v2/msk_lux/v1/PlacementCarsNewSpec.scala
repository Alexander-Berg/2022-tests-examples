package ru.yandex.vertis.moisha.impl.autoru_quota.v2.msk_lux.v1

import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model.Funds
import ru.yandex.vertis.moisha.util.GeoIds._

import ru.yandex.vertis.moisha.impl.autoru_quota.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru_quota.model._
import ru.yandex.vertis.moisha.impl.autoru_quota.utils.UnlimitedAmount

trait PlacementCarsNewSpec extends SingleProductDailyPolicySpec {

  import PlacementCarsNewSpec._

  "PlacementCarsNew policy" should {
    "support product PlacementCarsNew" in {
      policy.products.contains(Products.PlacementCarsNew.toString) shouldBe true
    }

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, placement(1700.rubles), withAmount(UnlimitedAmount), inRegion(RegMoscow))
    }
  }
}

object PlacementCarsNewSpec {

  def placement(price: Funds): AutoRuQuotaProduct =
    AutoRuQuotaProduct(
      Products.PlacementCarsNew,
      Set(
        AutoRuQuotaGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      )
    )
}
