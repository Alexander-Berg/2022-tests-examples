package ru.yandex.vertis.moisha.impl.autoru.v7

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model.Categories._
import ru.yandex.vertis.moisha.impl.autoru.model.Transports._
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.util.GeoIds._
import ru.yandex.vertis.moisha.util.geo.AutoRuRegionsV1._

/**
  * Specs on AutoRu policy for [[Products.Boost]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuBoostSpec extends SingleProductDailyPolicySpec {

  import AutoRuBoostSpec._

  "Boost policy" should {
    "support product PremiumOffer" in {
      policy.products.contains(Products.Boost.toString) should be(true)
    }

    "return correct price for Moscow with cars" in {
      checkPolicy(correctInterval, boost(250.rubles), priceIn(0L, Long.MaxValue), withTransport(Cars), inMoscow)
    }

    "return correct price for Moscow with lcv" in {
      checkPolicy(correctInterval, boost(250.rubles), priceIn(0L, Long.MaxValue), withTransport(LCV), inMoscow)
    }

    "return correct price for Moscow with ots" in {
      for (transport <- transportsWithout(Set(Cars, LCV))) {
        checkPolicy(correctInterval, boost(50.rubles), priceIn(0L, Long.MaxValue), withTransport(transport), inMoscow)
      }
    }

    "return correct price for SPb with cars" in {
      checkPolicy(
        correctInterval,
        boost(200.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        boost(150.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(New),
        inSPb
      )
    }

    "return correct price for SPb with lcv" in {
      checkPolicy(
        correctInterval,
        boost(200.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        boost(150.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(New),
        inSPb
      )
    }

    "return correct price for SPb with ots" in {
      for (transport <- transportsWithout(Set(Cars, LCV))) {
        checkPolicy(correctInterval, boost(20.rubles), priceIn(0L, Long.MaxValue), withTransport(transport), inSPb)
      }
    }

    "return correct price for Ekb" in {
      for {
        region <- RegEkbGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          boost(20.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Kvnr" in {
      for {
        region <- RegKvnrGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          boost(30.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Midlands" in {
      for {
        region <- RegMidlandsGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          boost(20.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for regions with cars" in {
      checkPolicy(
        correctInterval,
        boost(20.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        inRegion(0)
      )
    }

    "return correct price for regions with ots" in {
      for (transport <- transportsWithoutCars) {
        checkPolicy(
          correctInterval,
          boost(10.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(0)
        )
      }
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Boost,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuBoostSpec {

  def boost(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Boost,
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
