package ru.yandex.vertis.moisha.impl.autoru_quota.v2.msk_lux.v1

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

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, placement(65.rubles), withAmount(10), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(163.rubles), withAmount(25), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(325.rubles), withAmount(50), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(488.rubles), withAmount(75), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(650.rubles), withAmount(100), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(813.rubles), withAmount(125), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(975.rubles), withAmount(150), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(1138.rubles), withAmount(175), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(1300.rubles), withAmount(200), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(1950.rubles), withAmount(300), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(2600.rubles), withAmount(400), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(3250.rubles), withAmount(500), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(4875.rubles), withAmount(750), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(6500.rubles), withAmount(1000), inRegion(RegMoscow))

      checkPolicy(correctInterval, placement(9750.rubles), withAmount(1500), inRegion(RegMoscow))
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
