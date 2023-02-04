package ru.yandex.vertis.moisha.impl.autoru.v9

import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.AutoRuRequest
import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.gens._
import ru.yandex.vertis.moisha.impl.autoru.model.Categories._
import ru.yandex.vertis.moisha.impl.autoru.model.Transports._
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.model.gens.Producer
import ru.yandex.vertis.moisha.util.GeoIds._
import ru.yandex.vertis.moisha.util.geo.AutoRuRegionsV3._

/**
  * Specs on AutoRu policy for [[Products.Placement]]
  */
trait AutoRuPlacementSpec extends SingleProductDailyPolicySpec {

  import AutoRuPlacementSpec._

  def product: Products.Value = Products.Placement

  "Placement policy" should {
    "support Products.Placement" in {
      policy.products.contains(Products.Placement.toString) should be(true)
    }

    "fail if moto transport" in {
      checkPolicyFailure(
        correctInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(Moto),
        withCategory(Used)
      )
    }

    "fail if region is undefined" in {
      checkPolicyFailure(
        correctInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        inRegion(0),
        withTransport(Cars),
        withCategory(Used)
      )
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )
    }
  }

  it should passForCarsUsedInMoscow
  it should passForLcvUsedInMoscow
  it should passForLcvNewInMoscow
  it should passForCommercialUsedInMoscow
  it should passForCommercialNewInMoscow
  it should passForSubCommercialInMoscow
  it should passForSpecialInMoscow
  it should failOnOtherCategoriesInMoscow

  it should passForCarsUsedInSpb
  it should passForLcvUsedInSpb
  it should passForLcvNewInSpb
  it should passForCommercialUsedInSpb
  it should passForCommercialNewInSpb
  it should passForSubCommercialInSpb
  it should passForSpecialInSpb
  it should failOnOtherCategoriesInSpb

  it should passForCarsUsedInEkb
  it should failOnOtherCategoriesInEkb

  it should passForCarsUsedInAdygeya
  it should failOnOtherCategoriesInAdygeya

  it should passForCarsUsedInBelgorod
  it should failOnOtherCategoriesInBelgorod

  it should passForCarsUsedInTver
  it should failOnOtherCategoriesInTver

  it should passForCarsUsedInVoronezh
  it should failOnOtherCategoriesInVoronezh

  it should passForCarsUsedInMidlands
  it should failOnOtherCategoriesInMidlands

  it should failOnOtherCategoriesInRegions

  def passForCarsUsedInMoscow(): Unit = {
    "return correct price in first day for cars used in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(400.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )
    }

    "return correct prices in other days for cars used in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(40.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(85.rubles),
        priceIn(500.thousands, 800.thousands),
        oldOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(100.rubles),
        priceIn(800.thousands, 1500.thousands),
        oldOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(130.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )
    }
  }

  def passForLcvUsedInMoscow(): Unit = {
    "return correct price in first day for lcv used in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(400.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )
    }

    "return correct prices in other days for lcv used in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(40.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(85.rubles),
        priceIn(500.thousands, 800.thousands),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(100.rubles),
        priceIn(800.thousands, 1500.thousands),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(130.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )
    }
  }

  def passForLcvNewInMoscow(): Unit = {
    "return correct price in first day for lcv new in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(100.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(New)
      )
    }

    "return correct prices in other days for lcv new in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(20.rubles),
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(New)
      )
    }
  }

  def passForCommercialUsedInMoscow(): Unit = {
    "return correct price in first day for commercial used in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(350.rubles),
        priceIn(0L, 500.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(500.rubles),
        priceIn(500.thousands, 1000.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(800.rubles),
        priceIn(1000.thousands, 2000.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(1000.rubles),
        priceIn(2000.thousands, 4000.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(1200.rubles),
        priceIn(4000.thousands, Long.MaxValue),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(Used)
      )
    }

    "return correct prices in other days for commercial used in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(Used)
      )
    }
  }

  def passForCommercialNewInMoscow(): Unit = {
    "return correct price in first day for commercial new in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(350.rubles),
        priceIn(0L, 500.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(500.rubles),
        priceIn(500.thousands, 1000.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(800.rubles),
        priceIn(1000.thousands, 2000.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(1000.rubles),
        priceIn(2000.thousands, 4000.thousands),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(1200.rubles),
        priceIn(4000.thousands, Long.MaxValue),
        todaysOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(New)
      )
    }

    "return correct prices in other days for commercial new in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(Commercial),
        withCategory(New)
      )
    }
  }

  def passForSubCommercialInMoscow(): Unit = {
    val someCommercialCategories = Seq(Trailer, Trucks, Artic, Bus, Swapbody)
    val categories = Seq(New, Used)

    "return correct price in first day for sub commercial in Moscow" in {
      for {
        transport <- someCommercialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(350.rubles),
          priceIn(0L, 500.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(500.rubles),
          priceIn(500.thousands, 1000.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(800.rubles),
          priceIn(1000.thousands, 2000.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(1000.rubles),
          priceIn(2000.thousands, 4000.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(1200.rubles),
          priceIn(4000.thousands, Long.MaxValue),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )
      }
    }

    "return correct prices in other days for sub commercial in Moscow" in {
      for {
        transport <- someCommercialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(10.rubles),
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForSpecialInMoscow(): Unit = {
    val someSpecialCategories = Seq(Agricultural, Autoloader, Crane, Dredge)
    val categories = Seq(New, Used)

    "return correct price in first day for special in Moscow" in {
      for {
        transport <- someSpecialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(15.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )
      }
    }

    "return correct prices in other days for special new in Moscow" in {
      for {
        transport <- someSpecialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(15.rubles),
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def failOnOtherCategoriesInMoscow(): Unit = {
    "fail if cars new category in Moscow" in {
      for (transport <- Set(Cars)) {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inMoscow,
          withTransport(transport),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Moscow" in {
      for {
        transport <- transportsWithout(Set(Cars, LCV, Commercial, Special))
        category <- Set(Used, New)
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inMoscow,
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForCarsUsedInSpb(): Unit = {
    "return correct price in first day for cars used in Spb" in {
      checkPolicy(
        correctInterval,
        placement(300.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inSPb,
        withTransport(Cars),
        withCategory(Used)
      )
    }

    "return correct prices in other days for cars used in Spb" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inSPb,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(15.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inSPb,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(40.rubles),
        priceIn(500.thousands, 800.thousands),
        oldOffer,
        inSPb,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(800.thousands, 1500.thousands),
        oldOffer,
        inSPb,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(70.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inSPb,
        withTransport(Cars),
        withCategory(Used)
      )
    }
  }

  def passForLcvUsedInSpb(): Unit = {
    "return correct price in first day for lcv used in Spb" in {
      checkPolicy(
        correctInterval,
        placement(300.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
      )
    }

    "return correct prices in other days for lcv used in Spb" in {
      checkPolicy(
        correctInterval,
        placement(10.rubles),
        priceIn(0L, 300.thousands),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(15.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(40.rubles),
        priceIn(500.thousands, 800.thousands),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(800.thousands, 1500.thousands),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(70.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
      )
    }
  }

  def passForLcvNewInSpb(): Unit = {
    "return correct price in first day for lcv new in Spb" in {
      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(0L, Long.MaxValue),
        todaysOffer,
        inSPb,
        withTransport(LCV),
        withCategory(New)
      )
    }

    "return correct prices in other days for lcv new in Spb" in {
      checkPolicy(
        correctInterval,
        placement(15.rubles),
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(New)
      )
    }
  }

  def passForCommercialUsedInSpb(): Unit = {
    "return correct price in first day for commercial used in Spb" in {
      checkPolicy(
        correctInterval,
        placement(200.rubles),
        priceIn(0L, 500.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(250.rubles),
        priceIn(500.thousands, 1000.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(300.rubles),
        priceIn(1000.thousands, 2000.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(350.rubles),
        priceIn(2000.thousands, 4000.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(500.rubles),
        priceIn(4000.thousands, Long.MaxValue),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(Used)
      )
    }

    "return correct prices in other days for commercial used in Spb" in {
      checkPolicy(
        correctInterval,
        placement(5.rubles),
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(Used)
      )
    }
  }

  def passForCommercialNewInSpb(): Unit = {
    "return correct price in first day for commercial new in Spb" in {
      checkPolicy(
        correctInterval,
        placement(200.rubles),
        priceIn(0L, 500.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(250.rubles),
        priceIn(500.thousands, 1000.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(300.rubles),
        priceIn(1000.thousands, 2000.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(350.rubles),
        priceIn(2000.thousands, 4000.thousands),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(New)
      )

      checkPolicy(
        correctInterval,
        placement(500.rubles),
        priceIn(4000.thousands, Long.MaxValue),
        todaysOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(New)
      )
    }

    "return correct prices in other days for commercial new in Spb" in {
      checkPolicy(
        correctInterval,
        placement(5.rubles),
        priceIn(0L, Long.MaxValue),
        oldOffer,
        inSPb,
        withTransport(Commercial),
        withCategory(New)
      )
    }
  }

  def passForSubCommercialInSpb(): Unit = {
    val someCommercialCategories = Seq(Trailer, Trucks, Artic, Bus, Swapbody)
    val categories = Seq(New, Used)

    "return correct price in first day for sub commercial in Spb" in {
      for {
        transport <- someCommercialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(200.rubles),
          priceIn(0L, 500.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(250.rubles),
          priceIn(500.thousands, 1000.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(300.rubles),
          priceIn(1000.thousands, 2000.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(350.rubles),
          priceIn(2000.thousands, 4000.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )

        checkPolicy(
          correctInterval,
          placement(500.rubles),
          priceIn(4000.thousands, Long.MaxValue),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )
      }
    }

    "return correct prices in other days for sub commercial in Spb" in {
      for {
        transport <- someCommercialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(5.rubles),
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForSpecialInSpb(): Unit = {
    val someSpecialCategories = Seq(Agricultural, Autoloader, Crane, Dredge)
    val categories = Seq(New, Used)

    "return correct price in first day for special in Spb" in {
      for {
        transport <- someSpecialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(12.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )
      }
    }

    "return correct prices in other days for special new in Spb" in {
      for {
        transport <- someSpecialCategories
        category <- categories
      } {
        checkPolicy(
          correctInterval,
          placement(12.rubles),
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def failOnOtherCategoriesInSpb(): Unit = {
    "fail if new category in Spb" in {
      for (transport <- Set(Cars)) {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inSPb,
          withTransport(transport),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Spb" in {
      for {
        transport <- transportsWithout(Set(Cars, LCV, Commercial, Special))
        category <- Set(Used, New)
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inSPb,
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForCarsUsedInEkb(): Unit = {
    "return correct price in first day for cars used in Ekb" in {
      for (region <- RegEkbGroup) {
        checkPolicy(
          correctInterval,
          placement(100.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inRegion(region),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for cars used in Ekb" in {
      for (region <- RegEkbGroup) {
        checkPolicy(
          correctInterval,
          placement(1.ruble),
          priceIn(0L, 300.thousands),
          oldOffer,
          inRegion(region),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(3.rubles),
          priceIn(300.thousands, 500.thousands),
          oldOffer,
          inRegion(region),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(7.rubles),
          priceIn(500.thousands, 800.thousands),
          oldOffer,
          inRegion(region),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(12.rubles),
          priceIn(800.thousands, 1500.thousands),
          oldOffer,
          inRegion(region),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(15.rubles),
          priceIn(1500.thousands, Long.MaxValue),
          oldOffer,
          inRegion(region),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInEkb(): Unit = {
    "fail if new category in Ekb" in {
      for {
        transport <- Set(Cars, LCV)
        region <- RegEkbGroup
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(region),
          withTransport(transport),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Ekb" in {
      for {
        reg <- RegEkbGroup
        transport <- Transports.values.filterNot(Set(Cars))
        category <- Categories.values
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForCarsUsedInAdygeya(): Unit = {
    "return correct price in first day for cars used in Adygeya RG" in {
      for (reg <- RegAdygeyaGroup) {
        checkPolicy(
          correctInterval,
          placement(70.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for cars used in Adygeya RG" in {
      for (reg <- RegAdygeyaGroup) {
        checkPolicy(
          correctInterval,
          placement(1.rubles),
          priceIn(0L, 300.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(3.rubles),
          priceIn(300.thousands, 500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(6.rubles),
          priceIn(500.thousands, 800.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(10.rubles),
          priceIn(800.thousands, 1500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(12.rubles),
          priceIn(1500.thousands, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInAdygeya(): Unit = {
    "fail if cars new in Adygeya RG" in {
      for (reg <- RegAdygeyaGroup) {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Adygeya RG" in {
      for {
        reg <- RegAdygeyaGroup
        transport <- transportsWithoutCars
        category <- Categories.values
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForCarsUsedInBelgorod(): Unit = {
    "return correct price in first day for cars used in Belgorod RG" in {
      for (reg <- RegBelgorodGroup) {
        checkPolicy(
          correctInterval,
          placement(100.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for cars used in Belgorod RG" in {
      for (reg <- RegBelgorodGroup) {
        checkPolicy(
          correctInterval,
          placement(2.rubles),
          priceIn(0L, 300.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(5.rubles),
          priceIn(300.thousands, 500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(8.rubles),
          priceIn(500.thousands, 800.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(15.rubles),
          priceIn(800.thousands, 1500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(20.rubles),
          priceIn(1500.thousands, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInBelgorod(): Unit = {
    "fail if cars new in Belgorod RG" in {
      for (reg <- RegBelgorodGroup) {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Belgorod RG" in {
      for {
        reg <- RegBelgorodGroup
        transport <- transportsWithoutCars
        category <- Categories.values
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForCarsUsedInTver(): Unit = {
    "return correct price in first day for cars used in Tver RG" in {
      for (reg <- RegTverGroup) {
        checkPolicy(
          correctInterval,
          placement(150.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for cars used in Tver RG" in {
      for (reg <- RegTverGroup) {
        checkPolicy(
          correctInterval,
          placement(3.rubles),
          priceIn(0L, 300.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(6.rubles),
          priceIn(300.thousands, 500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(12.rubles),
          priceIn(500.thousands, 800.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(15.rubles),
          priceIn(800.thousands, 1500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(20.rubles),
          priceIn(1500.thousands, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInTver(): Unit = {
    "fail if cars new in Tver RG" in {
      for (reg <- RegTverGroup) {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Tver RG" in {
      for {
        reg <- RegTverGroup
        transport <- transportsWithoutCars
        category <- Categories.values
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForCarsUsedInVoronezh(): Unit = {
    "return correct price in first day for cars used in Voronezh RG" in {
      for (reg <- RegVoronezhGroup) {
        checkPolicy(
          correctInterval,
          placement(100.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for cars used in Voronezh RG" in {
      for (reg <- RegVoronezhGroup) {
        checkPolicy(
          correctInterval,
          placement(2.rubles),
          priceIn(0L, 300.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(5.rubles),
          priceIn(300.thousands, 500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(8.rubles),
          priceIn(500.thousands, 800.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(15.rubles),
          priceIn(800.thousands, 1500.thousands),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(20.rubles),
          priceIn(1500.thousands, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInVoronezh(): Unit = {
    "fail if cars new in Voronezh RG" in {
      for (reg <- RegVoronezhGroup) {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(Cars),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Voronezh RG" in {
      for {
        reg <- RegVoronezhGroup
        transport <- transportsWithoutCars
        category <- Categories.values
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def passForCarsUsedInMidlands(): Unit = {
    val CityKaliningrad: RegionId = 22
    val CityOmsk: RegionId = 66
    val CityObninsk: RegionId = 967

    val citiesGen = Set(
      Some(CityKaliningrad),
      Some(CityOmsk),
      Some(CityObninsk),
      None
    )

    "return correct price in first day for cars used in Midlands" in {
      for {
        reg <- RegMidlandsGroup
        city <- citiesGen
      } {
        checkPolicy(
          correctInterval,
          placement(50.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for cars used in Midlands" in {
      for {
        reg <- RegMidlandsGroup
        city <- citiesGen
      } {
        checkPolicy(
          correctInterval,
          placement(1.rubles),
          priceIn(0L, 300.thousands),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(2.rubles),
          priceIn(300.thousands, 500.thousands),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(3.rubles),
          priceIn(500.thousands, 1500.thousands),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(4.rubles),
          priceIn(1500.thousands, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInMidlands(): Unit = {
    val CityKaliningrad: RegionId = 22
    val CityOmsk: RegionId = 66
    val CityObninsk: RegionId = 967

    val citiesGen = Set(
      Some(CityKaliningrad),
      Some(CityOmsk),
      Some(CityObninsk),
      None
    )

    "fail if cars new in Midlands" in {
      for {
        reg <- RegMidlandsGroup
        city <- citiesGen
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(New)
        )
      }
    }

    "fail if other transport in Midlands" in {
      for {
        reg <- RegMidlandsGroup
        city <- citiesGen
        transport <- Transports.values.filterNot(Set(Cars))
        category <- Categories.values
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }

  def failOnOtherCategoriesInRegions(): Unit = {
    val CityKhabarovsk: RegionId = 76
    val CitySimferopol: RegionId = 146
    val CityChelyabinsk: RegionId = 56

    val RegArkhangelsk: RegionId = 10842
    val RegKemerovo: RegionId = 11282
    val RegKamchatka: RegionId = 11398

    val regionsGen = Set(RegArkhangelsk, RegKemerovo, RegKamchatka)
    val citiesGen = Set(Some(CityKhabarovsk), Some(CitySimferopol), Some(CityChelyabinsk), None)

    "fail if other transport in regions" in {
      for {
        reg <- regionsGen
        city <- citiesGen
        transport <- Transports.values
        category <- Categories.values
      } {
        checkPolicyFailure(
          correctInterval,
          Products.Placement,
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(transport),
          withCategory(category)
        )
      }
    }
  }
}

object AutoRuPlacementSpec {

  val TestIterations = 50

  def placement(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Placement,
      Set(
        AutoRuGood(Goods.Custom, Costs.PerIndexing, price)
      ),
      duration = DefaultDuration
    )

  import ru.yandex.vertis.moisha.environment._

  def oneDayPlacement: AutoRuRequest =
    RequestGen.next.copy(product = Products.Placement, interval = wholeDay(now()))

}
