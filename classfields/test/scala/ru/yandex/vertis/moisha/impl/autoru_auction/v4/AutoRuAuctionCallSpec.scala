package ru.yandex.vertis.moisha.impl.autoru_auction.v4

import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._

import ru.yandex.vertis.moisha.util.GeoIds.{RegChelyabinsk, RegSverdlovsk, RegVoronezh}

import ru.yandex.vertis.moisha.impl.autoru_auction.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru_auction.model._
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Categories._
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Sections._

/**
  * Specs on  for [[Products.Call]]
  */
trait AutoRuAuctionCallSpec extends SingleProductDailyPolicySpec {

  val premiumMarks_Group1 = Seq(
    "ASTON_MARTIN",
    "AUDI",
    "BENTLEY",
    "BMW",
    "BUGATTI",
    "CADILLAC",
    "FERRARI",
    "GENESIS",
    "INFINITI",
    "JAGUAR",
    "JEEP",
    "LAMBORGHINI",
    "LAND_ROVER",
    "LEXUS",
    "MASERATI",
    "MERCEDES",
    "MINI",
    "PORSCHE",
    "ROLLS_ROYCE",
    "VOLVO"
  )

  val premiumMarks_Group2 = Seq(
    "CHRYSLER",
    "DODGE",
    "MAZDA",
    "MITSUBISHI",
    "SKODA",
    "SUBARU",
    "SUZUKI",
    "TOYOTA",
    "VOLKSWAGEN"
  )

  val premiumMarks_Group3 = Seq(
    "AUDI",
    "BMW",
    "GENESIS",
    "JAGUAR",
    "JEEP",
    "LAND_ROVER",
    "LEXUS",
    "MERCEDES",
    "PORSCHE",
    "VOLVO"
  )

  val regularMarks_Group1 = Seq(
    "AC",
    "FORD",
    "KIA",
    "NISSAN",
    "OPEL"
  )

  val regularMarks_Group2 = Seq(
    "AC",
    "DODGE",
    "FORD",
    "KIA",
    "MAZDA",
    "NISSAN",
    "OPEL",
    "SKODA",
    "TOYOTA"
  )

  import AutoRuAuctionCallSpec._

  "Call policy" should {
    "support product Call" in {
      policy.products.contains(Products.Call.toString) shouldBe true
    }

    "return correct price for premium marks [group 1] in Moscow" in {
      for (mark <- premiumMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(2500.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inMoscow
        )
      }
    }

    "return correct price for premium marks [group 2] in Moscow" in {
      for (mark <- premiumMarks_Group2) {
        checkPolicy(
          correctInterval,
          call(1600.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inMoscow
        )
      }
    }

    "return correct price for regular marks in Moscow" in {
      for (mark <- regularMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(1200.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inMoscow
        )
      }
    }

    "return correct price for premium marks [group 1] in Spb" in {
      for (mark <- premiumMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(2500.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inSPb
        )
      }
    }

    "return correct price for premium marks [group 2] in Spb" in {
      for (mark <- premiumMarks_Group2) {
        checkPolicy(
          correctInterval,
          call(1600.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inMoscow
        )
      }
    }

    "return correct price for regular marks in Spb" in {
      for (mark <- regularMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(1200.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inSPb
        )
      }
    }

    "return correct price for premium marks in Ekb group" in {
      for (mark <- premiumMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(800.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inRegion(RegSverdlovsk)
        )
      }
    }

    "return correct price for regular marks in Ekb group" in {
      for (mark <- regularMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(600.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inRegion(RegSverdlovsk)
        )
      }
    }

    "return correct price for premium marks in Chelyabinsk group" in {
      for (mark <- premiumMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(800.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inRegion(RegChelyabinsk)
        )
      }
    }

    "return correct price for regular marks in Chelyabinsk group" in {
      for (mark <- regularMarks_Group1) {
        checkPolicy(
          correctInterval,
          call(600.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inRegion(RegChelyabinsk)
        )
      }
    }

    "return correct price for premium marks in Voronezh group" in {
      for (mark <- premiumMarks_Group3) {
        checkPolicy(
          correctInterval,
          call(800.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inRegion(RegVoronezh)
        )
      }
    }

    "return correct price for regular marks in Voronezh group" in {
      for (mark <- regularMarks_Group2) {
        checkPolicy(
          correctInterval,
          call(600.rubles),
          withCategory(Cars),
          withSection(New),
          withMarks(List(mark)),
          inRegion(RegVoronezh)
        )
      }
    }

    "fail on other regions" in {
      checkPolicyFailure(correctInterval, Products.Call, withCategory(Cars), withSection(New), inRegion(0))

      checkPolicyFailure(correctInterval, Products.Call, inRegion(0))
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Call,
        inMoscow
      )
    }

    "fail on other transport categories" in {
      checkPolicyFailure(
        correctInterval,
        Products.Call,
        withCategory(Cars),
        withSection(Used)
      )

      for (category <- categoriesWithoutCars) {
        checkPolicyFailure(
          correctInterval,
          Products.Call,
          withCategory(category)
        )
      }
    }
  }
}

object AutoRuAuctionCallSpec {

  def call(price: Funds): AutoRuAuctionProduct =
    AutoRuAuctionProduct(
      Products.Call,
      Set(
        AutoRuAuctionGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      )
    )

}
