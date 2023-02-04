package ru.auto.salesman.util.offer

import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.MotoModel.MotoCategory.ATV
import ru.auto.api.TrucksModel.TruckCategory.SWAP_BODY
import ru.auto.salesman.model.OfferCategories._
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.BaseSpec

class RichOfferSpec extends BaseSpec {

  "RichOffer.offerCategory" should {

    "return cars" in {
      forAll(offerGen(offerCategoryGen = CARS)) { offer =>
        offer.offerCategory shouldBe Cars
      }
    }

    "return moto" in {
      forAll(offerGen(offerCategoryGen = MOTO)) { baseOffer =>
        val offer = baseOffer.toBuilder().clearMotoInfo().build()
        offer.offerCategory shouldBe Moto
      }
    }

    "return moto subcategory" in {
      forAll(offerGen(offerCategoryGen = MOTO)) { baseOffer =>
        val builder = baseOffer.toBuilder()
        builder.getMotoInfoBuilder.setMotoCategory(ATV)
        val offer = builder.build()
        offer.offerCategory shouldBe Atv
      }
    }

    "return commercial" in {
      forAll(offerGen(offerCategoryGen = TRUCKS)) { baseOffer =>
        val offer = baseOffer.toBuilder().clearTruckInfo().build()
        offer.offerCategory shouldBe Commercial
      }
    }

    "return commercial subcategory" in {
      forAll(offerGen(offerCategoryGen = TRUCKS)) { baseOffer =>
        val builder = baseOffer.toBuilder()
        builder.getTruckInfoBuilder.setTruckCategory(SWAP_BODY)
        val offer = builder.build()
        offer.offerCategory shouldBe Swapbody
      }
    }

    "throw exception for empty category" in {
      forAll(offerGen()) { baseOffer =>
        val offer = baseOffer.toBuilder().clearCategory().build()
        intercept[InvalidOfferCategoryException](offer.offerCategory)
      }
    }
  }
}
