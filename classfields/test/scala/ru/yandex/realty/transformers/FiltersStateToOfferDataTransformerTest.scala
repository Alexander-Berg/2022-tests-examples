package ru.yandex.realty.transformers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.application.TransformerComponents
import ru.yandex.realty.features.{Features, FeaturesStubComponent}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 07.06.17
  */
@RunWith(classOf[JUnitRunner])
class FiltersStateToOfferDataTransformerTest
  extends FlatSpec
  with Matchers
  with TransformerComponents
  with DataSource
  with FeaturesStubComponent {

  " FiltersStateToOfferDataTransformer" should "work in Filters State → Offer Data SELL_APARTMENT" in {
    filtersStateToOfferDataTransformer
      .transform(FilterStateSellApartment)
      .value should contain theSameElementsAs (OfferDataSellApartment.value)
  }

  it should "work in Filters State → Offer Data SELL_ROOMS" in {
    filtersStateToOfferDataTransformer
      .transform(FilterStateSellRooms)
      .value should contain theSameElementsAs (OfferDataSellRooms.value)
  }

  it should "work in Filters State → Offer Data SELL_GARAGE" in {
    val jsObject = filtersStateToOfferDataTransformer
      .transform(FilterStateEditSellGarage2)
    println(jsObject)
    jsObject.value should contain theSameElementsAs (OfferDataSellGarage.value)
  }

  it should "work in Filters State → Offer Data SELL_HOUSE" in {
    filtersStateToOfferDataTransformer
      .transform(FilterStateSellHouse)
      .value should contain theSameElementsAs (OfferDataSellHouse.value)
  }

  " FiltersStateToOfferDataTransformer" should "not contain remoteReview block when its fields is missing" in {
    val apartment = FilterStateSellApartment - "onlineShowPossible" - "videoReviewUrl"
    val transformedValue = filtersStateToOfferDataTransformer
      .transform(apartment)
      .value
    transformedValue should contain theSameElementsAs (OfferDataSellApartment.value - "remoteReview")
  }

}
