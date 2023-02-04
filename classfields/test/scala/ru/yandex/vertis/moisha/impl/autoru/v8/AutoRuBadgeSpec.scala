package ru.yandex.vertis.moisha.impl.autoru.v8

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.model.Categories._
import ru.yandex.vertis.moisha.impl.autoru.model.Transports._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.util.GeoIds._
import ru.yandex.vertis.moisha.util.geo.AutoRuRegionsV2._

/**
  * Specs on AutoRu policy for [[Products.Badge]]
  */
trait AutoRuBadgeSpec extends SingleProductDailyPolicySpec {

  import AutoRuBadgeSpec._

  "Badge policy" should {
    "support product Badge" in {
      policy.products.contains(Products.Badge.toString) should be(true)
    }

    "return correct price for Moscow" in {
      checkPolicy(
        correctInterval,
        badge(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(Used),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        badge(75.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(New),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        badge(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(Used),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        badge(75.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(New),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        badge(15.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Commercial),
        withCategory(New),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        badge(15.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Special),
        withCategory(New),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        badge(15.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Moto),
        withCategory(New),
        inMoscow
      )
    }

    "return correct price for SPb" in {
      checkPolicy(
        correctInterval,
        badge(35.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        badge(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(New),
        inSPb
      )

      checkPolicy(
        correctInterval,
        badge(35.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        badge(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(New),
        inSPb
      )
    }

    "return correct price for Central" in {
      val CityLipetsk: RegionId = 9
      val CitySmolensk: RegionId = 12

      val citiesGen = Set(
        Some(CityLipetsk),
        Some(CitySmolensk),
        Some(CityNNovgorod),
        Some(CityKrasnodar),
        None
      )

      for {
        region <- RegCentralGroup
        city <- citiesGen
      } {
        checkPolicy(
          correctInterval,
          badge(15.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          inRegion(region),
          inCity(city)
        )
      }

      for {
        transport <- transportsWithoutCars
        region <- RegCentralGroup
        city <- citiesGen
      } {
        checkPolicy(
          correctInterval,
          badge(5.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region),
          inCity(city)
        )
      }
    }

    "return correct price for Ekb" in {
      for (region <- RegEkbGroup) {
        checkPolicy(
          correctInterval,
          badge(15.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          inRegion(region)
        )
      }

      for {
        transport <- transportsWithoutCars
        region <- RegEkbGroup
      } {
        checkPolicy(
          correctInterval,
          badge(5.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Midlands" in {
      for (region <- RegMidlandsGroup) {
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
