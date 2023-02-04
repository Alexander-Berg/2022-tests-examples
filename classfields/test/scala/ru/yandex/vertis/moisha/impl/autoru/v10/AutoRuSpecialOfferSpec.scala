package ru.yandex.vertis.moisha.impl.autoru.v10

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model.Categories._
import ru.yandex.vertis.moisha.impl.autoru.model.Transports._
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.util.geo.AutoRuRegionsV3._

/**
  * Specs on AutoRu policy for [[Products.Special]]
  */
trait AutoRuSpecialOfferSpec extends SingleProductDailyPolicySpec {

  import AutoRuSpecialOfferSpec._

  "SpecialOffer" should {
    "support product SpecialOffer" in {
      policy.products.contains(Products.Special.toString) should be(true)
    }

    "return correct price for Moscow with cars used" in {
      checkPolicy(
        correctInterval,
        special(75.rubles),
        priceIn(0L, 500.thousands),
        withTransport(Cars),
        withCategory(Used),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        special(120.rubles),
        priceIn(500.thousands, 1500.thousands),
        withTransport(Cars),
        withCategory(Used),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        special(150.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        withTransport(Cars),
        withCategory(Used),
        inMoscow
      )
    }

    "return correct price for Moscow with lcv used" in {
      checkPolicy(
        correctInterval,
        special(75.rubles),
        priceIn(0L, 500.thousands),
        withTransport(LCV),
        withCategory(Used),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        special(120.rubles),
        priceIn(500.thousands, 1500.thousands),
        withTransport(LCV),
        withCategory(Used),
        inMoscow
      )

      checkPolicy(
        correctInterval,
        special(150.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        withTransport(LCV),
        withCategory(Used),
        inMoscow
      )
    }

    "return correct price for Moscow with lcv new" in {
      checkPolicy(
        correctInterval,
        special(120.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(New),
        inMoscow
      )
    }

    "return correct price for Moscow with cars new" in {
      checkPolicy(
        correctInterval,
        special(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(New),
        inMoscow
      )
    }

    "return correct price for Moscow with ots" in {
      for (transport <- transportsWithout(Set(Cars, LCV))) {
        checkPolicy(correctInterval, special(44.rubles), priceIn(0L, Long.MaxValue), withTransport(transport), inMoscow)
      }
    }

    "return correct price for Spb with cars used" in {
      checkPolicy(
        correctInterval,
        special(50.rubles),
        priceIn(0L, 500.thousands),
        withTransport(Cars),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        special(75.rubles),
        priceIn(500.thousands, 1500.thousands),
        withTransport(Cars),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        special(100.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        withTransport(Cars),
        withCategory(Used),
        inSPb
      )
    }

    "return correct price for Spb with lcv used" in {
      checkPolicy(
        correctInterval,
        special(50.rubles),
        priceIn(0L, 500.thousands),
        withTransport(LCV),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        special(75.rubles),
        priceIn(500.thousands, 1500.thousands),
        withTransport(LCV),
        withCategory(Used),
        inSPb
      )

      checkPolicy(
        correctInterval,
        special(100.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        withTransport(LCV),
        withCategory(Used),
        inSPb
      )
    }

    "return correct price for Spb with lcv new" in {
      checkPolicy(
        correctInterval,
        special(75.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(LCV),
        withCategory(New),
        inSPb
      )
    }

    "return correct price for Spb with cars new" in {
      checkPolicy(
        correctInterval,
        special(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        withCategory(New),
        inSPb
      )
    }

    "return correct price for Spb with commercial" in {
      checkPolicy(correctInterval, special(10.rubles), priceIn(0L, Long.MaxValue), withTransport(Commercial), inSPb)
    }

    "return correct price for Spb with sub commercial" in {
      for (transport <- Seq(Trailer, Bus, Trucks, Artic)) {
        checkPolicy(correctInterval, special(10.rubles), priceIn(0L, Long.MaxValue), withTransport(transport), inSPb)
      }
    }

    "return correct price for Spb with ots" in {
      for (transport <- transportsWithout(Set(Cars, LCV, Commercial, Special))) {
        checkPolicy(correctInterval, special(50.rubles), priceIn(0L, Long.MaxValue), withTransport(transport), inSPb)
      }
    }

    "return correct price for Ekb" in {
      for {
        region <- RegEkbGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          special(30.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Adygeya RG" in {
      for {
        region <- RegAdygeyaGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          special(30.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Belgorod RG" in {
      for {
        region <- RegBelgorodGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          special(30.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Tver RG" in {
      for {
        region <- RegTverGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          special(30.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for Voronezh RG" in {
      for {
        region <- RegVoronezhGroup
        transport <- Transports.values
      } {
        checkPolicy(
          correctInterval,
          special(30.rubles),
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
          special(10.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(region)
        )
      }
    }

    "return correct price for regions with cars" in {
      checkPolicy(
        correctInterval,
        special(10.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Cars),
        inRegion(0)
      )
    }

    "return correct price for regions with ots" in {
      for (transport <- transportsWithoutCars) {
        checkPolicy(
          correctInterval,
          special(6.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(transport),
          inRegion(0)
        )
      }
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Special,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuSpecialOfferSpec {

  def special(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Special,
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
