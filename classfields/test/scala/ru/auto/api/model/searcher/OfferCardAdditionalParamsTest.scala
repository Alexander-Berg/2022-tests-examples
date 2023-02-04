package ru.auto.api.model.searcher

import ru.auto.api.BaseSpec
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.SearcherQuery
import ru.auto.api.ui.UiModel.TristateTumblerGroup

class OfferCardAdditionalParamsTest extends BaseSpec {

  "should have empty params without query" in {
    val query = ""
    OfferCardAdditionalParams.from(query, TristateTumblerGroup.NONE) shouldEqual OfferCardAdditionalParams.empty
  }

  "should have filled params with valid query" in {
    val query = "geo_id=213&geo_radius=200&with_delivery=BOTH"

    OfferCardAdditionalParams.from(query, TristateTumblerGroup.NONE) shouldEqual OfferCardAdditionalParams(
      List(213),
      Some(200),
      TristateTumblerGroup.BOTH
    )
  }

  "should return valid params even for invalid query" in {
    val query1 = "geo_id=213&geo_radius=qrqw&rqere&with_delivery="
    val query2 = "geo_id=&geo_radius=24&rqere&with_delivery=555"
    val query3 = "arg1=e&arg2=34&&with_delivery"

    OfferCardAdditionalParams.from(query1, TristateTumblerGroup.NONE) shouldEqual OfferCardAdditionalParams(
      List(213),
      Option.empty
    )
    OfferCardAdditionalParams.from(query2, TristateTumblerGroup.NONE) shouldEqual OfferCardAdditionalParams(
      List.empty,
      Some(24)
    )
    OfferCardAdditionalParams.from(query3, TristateTumblerGroup.NONE) shouldEqual OfferCardAdditionalParams.empty
  }

  "should buildForListing" in {
    val searchRequest1 = SearcherRequest(Cars, SearcherQuery("geo_id=213&geo_radius=qrqw&rqere&with_delivery=ONLY"))
    val searchRequest2 = SearcherRequest(Cars, SearcherQuery("geo_id=&geo_radius=24&rqere&with_delivery=only"))
    val searchRequest3 = SearcherRequest(Cars, SearcherQuery("arg1=e&arg2=34&"))
    val searchRequest4 = SearcherRequest(Cars, SearcherQuery("geo_id=213&geo_radius=200&with_delivery=BOTH"))
    val searchRequest5 = SearcherRequest(Cars, SearcherQuery(""))

    OfferCardAdditionalParams.buildForListing(searchRequest1) shouldEqual OfferCardAdditionalParams(
      List(213),
      Option.empty,
      TristateTumblerGroup.ONLY
    )
    OfferCardAdditionalParams.buildForListing(searchRequest2) shouldEqual OfferCardAdditionalParams(
      List.empty,
      Some(24),
      TristateTumblerGroup.NONE
    )
    OfferCardAdditionalParams.buildForListing(searchRequest3) shouldEqual OfferCardAdditionalParams.empty
    OfferCardAdditionalParams.buildForListing(searchRequest4) shouldEqual OfferCardAdditionalParams(
      List(213),
      Some(200),
      TristateTumblerGroup.BOTH
    )
    OfferCardAdditionalParams.buildForListing(searchRequest5) shouldEqual OfferCardAdditionalParams.empty
  }

  "should buildForCard" in {
    val searchRequest1 = "geo_id=213&geo_radius=qrqw&rqere&with_delivery=ONLY"
    val searchRequest2 = "geo_id=&geo_radius=24&rqere&with_delivery=only"
    val searchRequest3 = "arg1=e&arg2=34&"
    val searchRequest4 = "geo_id=213&geo_radius=200&with_delivery=BOTH"
    val searchRequest5 = ""

    OfferCardAdditionalParams.buildForCard(searchRequest1) shouldEqual OfferCardAdditionalParams(
      List(213),
      Option.empty,
      TristateTumblerGroup.ONLY
    )
    OfferCardAdditionalParams.buildForCard(searchRequest2) shouldEqual OfferCardAdditionalParams(
      List.empty,
      Some(24),
      TristateTumblerGroup.BOTH
    )
    OfferCardAdditionalParams.buildForCard(searchRequest3) shouldEqual OfferCardAdditionalParams.empty.copy(
      withDelivery = TristateTumblerGroup.BOTH
    )
    OfferCardAdditionalParams.buildForCard(searchRequest4) shouldEqual OfferCardAdditionalParams(
      List(213),
      Some(200),
      TristateTumblerGroup.BOTH
    )
    OfferCardAdditionalParams.buildForCard(searchRequest5) shouldEqual OfferCardAdditionalParams.empty.copy(
      withDelivery = TristateTumblerGroup.BOTH
    )
  }
}
