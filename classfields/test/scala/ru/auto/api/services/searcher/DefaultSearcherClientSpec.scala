package ru.auto.api.services.searcher

import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.OfferResponse
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.MarkModelNameplateGeneration
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.web.MockedFeatureManager
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}

/**
  * @author pnaydenov
  */
class DefaultSearcherClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with OptionValues
  with MockedFeatureManager {

  private val searcherClient = new DefaultSearcherClient(http, featureManager)

  "SearcherClient" should {
    "throw OfferNotFoundException" in {
      val id = OfferIDGen.next
      http.respondWithProtoFrom[OfferResponse]("/searcher/offer_not_found.json")
      a[OfferNotFoundException] should be thrownBy searcherClient.getOffer(Cars, id).await
    }

    "get related for MMG" in {
      val mmg = MarkModelNameplateGeneration.parse("BMW#3ER")
      val rids = Seq[Integer](1, 2, 3)
      val yearFrom = Some(1984)
      val yearTo = Some(2045)

      http.expectUrl("/related?mark=BMW&model=3ER&rid=1&rid=2&rid=3&year_from=1984&year_to=2045")
      http.respondWithJsonFrom("/searcher/similar.json")

      val result = searcherClient.getSimilar(mmg, rids, yearFrom, yearTo).futureValue

      result.length shouldBe 2
      result.head.mark.get shouldBe "SUBARU"
      result.head.model.get shouldBe "XV"
      result.head.generation.get shouldBe 7892300
    }

    "get regions by prompt" in {
      http.expectUrl("/regionprompt?letters=text&not_only_with_offers=true&only_cities=true&with_villages=true")
      http.respondWithJsonFrom("/searcher/region_prompt.json")

      val result = searcherClient
        .regionPrompt(Some("text"), notOnlyWithOffers = true, onlyCities = true, withVillages = true)
        .futureValue

      result.toSet shouldEqual Set(213, 162734, 150747, 176800, 124606, 135703)
    }
  }
}
