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
  * Specs on AutoRu policy for [[Products.Premium]]
  */
trait AutoRuPremiumSpec extends SingleProductDailyPolicySpec {

  import AutoRuPremiumSpec._

  "Premium" should {
    "support product Premium" in {
      policy.products.contains(Products.Premium.toString) should be(true)
    }

    "return correct price for Moscow with cars" in {
      checkPolicy(correctInterval, premium(350.rubles), priceIn(0L, Long.MaxValue), withTransport(Cars), inMoscow)
    }

    "return correct price for Moscow with lcv" in {
      checkPolicy(correctInterval, premium(350.rubles), priceIn(0L, Long.MaxValue), withTransport(LCV), inMoscow)
    }

    "return correct price for Moscow with ots" in {
      for (transport <- transportsWithout(Set(Cars, LCV))) {
        checkPolicy(
          correctInterval,
          premium(125.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inMoscow
        )
      }
    }

    "return correct price for SPb with cars" in {
      checkPolicy(
        correctInterval,
        premium(250.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        premium(200.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(New),
        inSPb
      )
    }

    "return correct price for SPb with lcv" in {
      checkPolicy(
        correctInterval,
        premium(250.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        premium(200.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(New),
        inSPb
      )
    }

    "return correct price for SPb with commercial" in {
      checkPolicy(correctInterval, premium(50.rubles), priceIn(0L, Long.MaxValue), withTransport(Commercial), inSPb)
    }

    "return correct price for SPb with ots" in {
      for (transport <- transportsWithout(Set(Cars, LCV, Commercial))) {
        checkPolicy(correctInterval, premium(10.rubles), priceIn(0L, Long.MaxValue), withTransport(transport), inSPb)
      }
    }

    "return correct price for Ekb" in {
      for {
        region <- RegEkbGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          premium(50.rubles),
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
          premium(100.rubles),
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
          premium(50.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for regions with cars" in {
      checkPolicy(
        correctInterval,
        premium(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        inRegion(0)
      )
    }

    "return correct price for regions with ots" in {
      for (transport <- transportsWithoutCars) {
        checkPolicy(
          correctInterval,
          premium(30.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(0)
        )
      }
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Premium,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuPremiumSpec {

  def premium(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Premium,
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
