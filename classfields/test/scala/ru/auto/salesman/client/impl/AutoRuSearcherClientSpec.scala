package ru.auto.salesman.client.impl

import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.MotoModel.MotoCategory.ATV
import ru.auto.api.TrucksModel.TruckCategory.SWAP_BODY
import ru.auto.salesman.client.SearcherClient.OfferPositionRequest
import ru.auto.salesman.client.impl.AutoRuSearcherClient.resolver
import ru.auto.salesman.model.searcher.OfferSorts
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.model.gens.autoruOfferIdGen

class AutoRuSearcherClientSpec extends BaseSpec {

  private val offerId = 1043045004L

  "searcher client uri resolver" should {

    "resolve cars uri" in {
      forAll(offerGen(autoruOfferIdGen(offerId), CARS)) { offer =>
        val result = resolver.context(
          OfferPositionRequest(
            offer,
            withSuperGen = false,
            OfferSorts.AlphabetAsc
          )
        )
        result should startWith("offerPosition")
        result.contains("offer_id_for_position=autoru-1043045004") shouldBe true
        result.contains("moto_category") shouldBe false
        result.contains("trucks_category") shouldBe false
      }
    }

    "resolve moto uri" in {
      forAll(offerGen(autoruOfferIdGen(offerId), MOTO)) { baseOffer =>
        val builder = baseOffer.toBuilder()
        builder.getMotoInfoBuilder.setMotoCategory(ATV)
        val offer = builder.build()
        val result = resolver.context(
          OfferPositionRequest(
            offer,
            withSuperGen = false,
            OfferSorts.AlphabetAsc
          )
        )
        result should startWith("motoOfferPosition")
        result.contains("offer_id_for_position=autoru-1043045004") shouldBe true
        result.contains("moto_category=atv") shouldBe true
        result.contains("trucks_category") shouldBe false
      }
    }

    "resolve trucks uri" in {
      forAll(offerGen(autoruOfferIdGen(offerId), TRUCKS)) { baseOffer =>
        val builder = baseOffer.toBuilder()
        builder.getTruckInfoBuilder.setTruckCategory(SWAP_BODY)
        val offer = builder.build()
        val result = resolver.context(
          OfferPositionRequest(
            offer,
            withSuperGen = false,
            OfferSorts.AlphabetAsc
          )
        )
        result should startWith("truckOfferPosition")
        result.contains("offer_id_for_position=autoru-1043045004") shouldBe true
        result.contains("moto_category") shouldBe false
        result.contains("trucks_category=SWAP_BODY") shouldBe true
      }
    }
  }
}
