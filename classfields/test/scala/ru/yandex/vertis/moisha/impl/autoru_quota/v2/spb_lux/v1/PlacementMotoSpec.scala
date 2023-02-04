package ru.yandex.vertis.moisha.impl.autoru_quota.v2.spb_lux.v1

import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model.Funds
import ru.yandex.vertis.moisha.util.GeoIds._

import ru.yandex.vertis.moisha.impl.autoru_quota.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru_quota.model._

trait PlacementMotoSpec extends SingleProductDailyPolicySpec {

  import PlacementMotoSpec._

  "PlacementMoto policy" should {
    "support product PlacementMoto" in {
      policy.products.contains(Products.PlacementMoto.toString) shouldBe true
    }

    "return correct price for Spb" in {
      checkPolicy(correctInterval, placement(48.rubles), withAmount(10), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(122.rubles), withAmount(25), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(243.rubles), withAmount(50), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(366.rubles), withAmount(75), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(487.rubles), withAmount(100), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(609.rubles), withAmount(125), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(731.rubles), withAmount(150), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(853.rubles), withAmount(175), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(975.rubles), withAmount(200), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(1462.rubles), withAmount(300), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(1950.rubles), withAmount(400), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(2437.rubles), withAmount(500), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(3656.rubles), withAmount(750), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(4875.rubles), withAmount(1000), inRegion(RegSPb))

      checkPolicy(correctInterval, placement(7312.rubles), withAmount(1500), inRegion(RegSPb))
    }
  }
}

object PlacementMotoSpec {

  def placement(price: Funds): AutoRuQuotaProduct =
    AutoRuQuotaProduct(
      Products.PlacementMoto,
      Set(
        AutoRuQuotaGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      )
    )
}
