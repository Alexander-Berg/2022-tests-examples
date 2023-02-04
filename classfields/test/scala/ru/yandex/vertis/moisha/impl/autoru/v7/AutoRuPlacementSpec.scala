package ru.yandex.vertis.moisha.impl.autoru.v7

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
import ru.yandex.vertis.moisha.util.geo.AutoRuRegionsV1._

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
  it should passForCommercialUsedInMoscow
  it should passForSubCommercialUsedInMoscow
  it should failOnOtherCategoriesInMoscow

  it should passForCarsUsedInSpb
  it should passForLcvUsedInSpb
  it should passForCommercialUsedInSpb
  it should passForSubCommercialUsedInSpb
  it should failOnOtherCategoriesInSpb

  it should failOnOtherCategoriesInEkb

  it should passForCarsUsedInKvnr
  it should failOnOtherCategoriesInKvnr

  it should passForCarsUsedInMidlands
  it should failOnOtherCategoriesInMidlands

  it should failOnOtherCategoriesInRegions

  def passForCarsUsedInMoscow(): Unit = {
    "return correct price in first day for cars used in Moscow" in {
      checkPolicy(
        correctInterval,
        placement(350.rubles),
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
        placement(30.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(65.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inMoscow,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(100.rubles),
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
        placement(350.rubles),
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
        placement(30.rubles),
        priceIn(300.thousands, 500.thousands),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(65.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(100.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inMoscow,
        withTransport(LCV),
        withCategory(Used)
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

  def passForSubCommercialUsedInMoscow(): Unit = {
    val someCommercialCategories = Seq(Trucks, Bus, Trailer, Swapbody)

    "return correct price in first day for sub commercial used in Moscow" in {
      for (transport <- someCommercialCategories) {
        checkPolicy(
          correctInterval,
          placement(350.rubles),
          priceIn(0L, 500.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(500.rubles),
          priceIn(500.thousands, 1000.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(800.rubles),
          priceIn(1000.thousands, 2000.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(1000.rubles),
          priceIn(2000.thousands, 4000.thousands),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(1200.rubles),
          priceIn(4000.thousands, Long.MaxValue),
          todaysOffer,
          inMoscow,
          withTransport(transport),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for sub commercial used in Moscow" in {
      for (transport <- someCommercialCategories) {
        checkPolicy(
          correctInterval,
          placement(10.rubles),
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inMoscow,
          withTransport(transport),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInMoscow(): Unit = {
    "fail if new category in Moscow" in {
      for (transport <- Set(Cars, LCV)) {
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
        transport <- transportsWithout(Set(Cars, LCV, Commercial))
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
        placement(250.rubles),
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
        placement(30.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inSPb,
        withTransport(Cars),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(50.rubles),
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
        placement(250.rubles),
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
        placement(30.rubles),
        priceIn(500.thousands, 1500.thousands),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
      )

      checkPolicy(
        correctInterval,
        placement(50.rubles),
        priceIn(1500.thousands, Long.MaxValue),
        oldOffer,
        inSPb,
        withTransport(LCV),
        withCategory(Used)
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

  def passForSubCommercialUsedInSpb(): Unit = {
    val someCommercialCategories = Seq(Bus, Artic, Trailer, Trucks, Swapbody)

    "return correct price in first day for sub commercial used in Spb" in {
      for (transport <- someCommercialCategories) {
        checkPolicy(
          correctInterval,
          placement(200.rubles),
          priceIn(0L, 500.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(250.rubles),
          priceIn(500.thousands, 1000.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(300.rubles),
          priceIn(1000.thousands, 2000.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(350.rubles),
          priceIn(2000.thousands, 4000.thousands),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(500.rubles),
          priceIn(4000.thousands, Long.MaxValue),
          todaysOffer,
          inSPb,
          withTransport(transport),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for sub commercial used in Spb" in {
      for (transport <- someCommercialCategories) {
        checkPolicy(
          correctInterval,
          placement(5.rubles),
          priceIn(0L, Long.MaxValue),
          oldOffer,
          inSPb,
          withTransport(transport),
          withCategory(Used)
        )
      }
    }
  }

  def failOnOtherCategoriesInSpb(): Unit = {
    "fail if new category in Spb" in {
      for (transport <- Set(Cars, LCV)) {
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
        transport <- transportsWithout(Set(Cars, LCV, Commercial))
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

  def failOnOtherCategoriesInEkb(): Unit = {
    val CityTagiiil: RegionId = 11168

    val citiesGen = CityEkbGroup.map(Option.apply(_)) ++ Set(
      Some(CityTagiiil),
      None
    )

    "fail if other transport in Ekb" in {
      for {
        reg <- RegEkbGroup
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

  def passForCarsUsedInKvnr(): Unit = {
    val CityLipetsk: RegionId = 9
    val CitySmolensk: RegionId = 12

    val citiesGen = CityKvnrGroup.map(Option.apply(_)) ++ Set(
      Some(CityLipetsk),
      Some(CitySmolensk),
      Some(CityNNovgorod),
      Some(CityKrasnodar),
      None
    )

    "return correct price in first day for cars used in Kvnr" in {
      for {
        reg <- RegKvnrGroup
        city <- citiesGen
      } {
        checkPolicy(
          correctInterval,
          placement(70.rubles),
          priceIn(0L, Long.MaxValue),
          todaysOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )
      }
    }

    "return correct prices in other days for cars used in Kvnr" in {
      for {
        reg <- RegKvnrGroup
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
          placement(3.rubles),
          priceIn(300.thousands, 500.thousands),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(6.rubles),
          priceIn(500.thousands, 1500.thousands),
          oldOffer,
          inRegion(reg),
          inCity(city),
          withTransport(Cars),
          withCategory(Used)
        )

        checkPolicy(
          correctInterval,
          placement(10.rubles),
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

  def failOnOtherCategoriesInKvnr(): Unit = {
    val CityLipetsk: RegionId = 9
    val CitySmolensk: RegionId = 12

    val citiesGen = CityKvnrGroup.map(Option.apply(_)) ++ Set(
      Some(CityLipetsk),
      Some(CitySmolensk),
      Some(CityNNovgorod),
      Some(CityKrasnodar),
      None
    )

    "fail if cars new in Kvnr" in {
      for {
        reg <- RegKvnrGroup
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

    "fail if other transport in Kvnr" in {
      for {
        reg <- RegKvnrGroup
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
