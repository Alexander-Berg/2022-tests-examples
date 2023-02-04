package ru.yandex.vertis.moisha.impl.autoru.v11

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model.Categories._
import ru.yandex.vertis.moisha.impl.autoru.model.Transports._
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.util.geo.AutoRuRegionsV3._

/**
  * Specs on AutoRu policy for [[Products.Boost]]
  */
trait AutoRuBoostSpec extends SingleProductDailyPolicySpec {

  import AutoRuBoostSpec._

  "Boost policy" should {
    "support product PremiumOffer" in {
      policy.products.contains(Products.Boost.toString) should be(true)
    }

    "return correct price for Moscow with cars" in {
      checkPolicy(
        correctInterval,
        boost(150.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(New),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        boost(300.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(Used),
        inMoscow
      )
    }

    "return correct price for Moscow with lcv" in {
      checkPolicy(correctInterval, boost(300.rubles), priceIn(0L, Long.MaxValue), withTransport(LCV), inMoscow)
    }

    "return correct price for Moscow with ots" in {
      for (transport <- transportsWithout(Set(Cars, LCV))) {
        checkPolicy(correctInterval, boost(50.rubles), priceIn(0L, Long.MaxValue), withTransport(transport), inMoscow)
      }
    }

    "return correct price for SPb with cars" in {
      checkPolicy(
        correctInterval,
        boost(250.rubles),
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
        boost(250.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        boost(250.rubles),
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

    "return correct price for Adygeya regions group" in {
      for {
        region <- RegAdygeyaGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          boost(50.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Belgorod regions group for cars" in {
      for (region <- RegBelgorodGroup) {
        checkPolicy(
          correctInterval,
          boost(80.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          withCategory(Used),
          inRegion(region)
        )

        checkPolicy(
          correctInterval,
          boost(50.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          withCategory(New),
          inRegion(region)
        )
      }
    }

    "return correct price for Belgorod regions group for ots" in {
      for {
        region <- RegBelgorodGroup
        transport <- transportsWithoutCars
      } {
        checkPolicy(
          correctInterval,
          boost(50.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Chelyabinsk regions group" in {
      for {
        region <- RegChelyabinskGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          boost(100.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Tver regions group for cars" in {
      for (region <- RegTverGroup) {
        checkPolicy(
          correctInterval,
          boost(80.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          withCategory(Used),
          inRegion(region)
        )

        checkPolicy(
          correctInterval,
          boost(50.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          withCategory(New),
          inRegion(region)
        )
      }
    }

    "return correct price for Tver regions group for ots" in {
      for {
        region <- RegTverGroup
        transport <- transportsWithoutCars
      } {
        checkPolicy(
          correctInterval,
          boost(50.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Ekb" in {
      for {
        region <- RegEkbGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          boost(100.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Voronezh regions group for cars" in {
      for (region <- RegVoronezhGroup) {
        checkPolicy(
          correctInterval,
          boost(80.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          withCategory(Used),
          inRegion(region)
        )

        checkPolicy(
          correctInterval,
          boost(50.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Cars),
          withCategory(New),
          inRegion(region)
        )
      }
    }

    "return correct price for Voronezh regions group for ots" in {
      for {
        region <- RegVoronezhGroup
        transport <- transportsWithoutCars
      } {
        checkPolicy(
          correctInterval,
          boost(50.rubles),
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
