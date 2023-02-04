package ru.yandex.vertis.moisha.impl.autoru.v6

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.utils._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.util.GeoIds._
import ru.yandex.vertis.moisha.util.geo.AutoRuRegionsV1._

/**
  * Specs on AutoRu policy for [[Products.Badge]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  */
trait AutoRuBadgeSpec extends SingleProductDailyPolicySpec {

  import AutoRuBadgeSpec._

  "Badge policy" should {
    "support product Badge" in {
      policy.products.contains(Products.Badge.toString) should be(true)
    }

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, badge(15.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct price for SPb" in {
      checkPolicy(correctInterval, badge(10.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "return correct price for Kvnr" in {
      val CityLipetsk: RegionId = 9
      val CitySmolensk: RegionId = 12

      val citiesGen = CityKvnrGroup.map(Option.apply(_)) ++ Set(
        Some(CityLipetsk),
        Some(CitySmolensk),
        Some(CityNNovgorod),
        Some(CityKrasnodar),
        None
      )

      for {
        region <- RegKvnrGroup
        city <- citiesGen
      } {
        checkPolicy(correctInterval, badge(5.rubles), priceIn(0L, Long.MaxValue), inRegion(region), inCity(city))
      }
    }

    "return correct price for Ekb" in {
      for (region <- RegEkbGroup) {
        checkPolicy(correctInterval, badge(5.rubles), priceIn(0L, Long.MaxValue), inRegion(region))
      }
    }

    "return correct price for all regions" in {
      checkPolicy(correctInterval, badge(5.rubles), priceIn(0L, Long.MaxValue), inRegion(0))
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Badge,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuBadgeSpec {

  def badge(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Badge,
      Set(
        AutoRuGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      ),
      duration = DefaultDuration
    )

}
