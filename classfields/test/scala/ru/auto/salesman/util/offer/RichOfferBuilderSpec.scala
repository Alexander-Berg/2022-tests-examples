package ru.auto.salesman.util.offer

import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.MotoModel.MotoCategory.ATV
import ru.auto.api.TrucksModel.TruckCategory.SWAP_BODY
import ru.auto.salesman.model.OfferCategories._
import ru.auto.salesman.model.OfferCategory
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.offer.RichOfferBuilderSpec._

class RichOfferBuilderSpec extends BaseSpec {

  "Offer.Builder.setCategory" should {

    "set cars category" in {
      forAll(offerGen()) { offer =>
        val result = offer.withOfferCategory(Cars)
        result.getCategory shouldBe CARS
        result.hasMotoInfo shouldBe false
        result.hasTruckInfo shouldBe false
      }
    }

    "set moto category" in {
      forAll(offerGen()) { offer =>
        val result = offer.withOfferCategory(Moto)
        result.getCategory shouldBe MOTO
        result.hasMotoInfo shouldBe false
        result.hasTruckInfo shouldBe false
      }
    }

    "set moto subcategory" in {
      forAll(offerGen()) { offer =>
        val result = offer.withOfferCategory(Atv)
        result.getCategory shouldBe MOTO
        result.getMotoInfo.getMotoCategory shouldBe ATV
        result.hasTruckInfo shouldBe false
      }
    }

    "set trucks category" in {
      forAll(offerGen()) { offer =>
        val result = offer.withOfferCategory(Commercial)
        result.getCategory shouldBe TRUCKS
        result.hasMotoInfo shouldBe false
        result.hasTruckInfo shouldBe false
      }
    }

    "set trucks subcategory" in {
      forAll(offerGen()) { offer =>
        val result = offer.withOfferCategory(Swapbody)
        result.getCategory shouldBe TRUCKS
        result.hasMotoInfo shouldBe false
        result.getTruckInfo.getTruckCategory shouldBe SWAP_BODY
      }
    }
  }
}

object RichOfferBuilderSpec {

  implicit class TestRichOffer(private val offer: Offer) extends AnyVal {

    def withOfferCategory(offerCategory: OfferCategory): Offer =
      offer
        .toBuilder()
        .clearMotoInfo()
        .clearTruckInfo()
        .setOfferCategory(offerCategory)
        .build()
  }
}
