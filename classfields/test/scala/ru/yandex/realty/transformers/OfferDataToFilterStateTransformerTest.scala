package ru.yandex.realty.transformers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import ru.yandex.realty.application.TransformerComponents
import ru.yandex.realty.features.FeaturesStubComponent

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 13.06.17
  */
@RunWith(classOf[JUnitRunner])
class OfferDataToFilterStateTransformerTest
  extends FlatSpec
  with Matchers
  with TransformerComponents
  with DataSource
  with FeaturesStubComponent {
  "OfferDataToFilterStateTransformer" should "work in transformer Offer VOS Data → Filters State SELL_APARTMENT" in {
    offerDataToFilterStateTransformer
      .transform(OfferDataVosSellApartment)
      .value should contain theSameElementsAs (FilterStateEditSellApartment.value)
  }

  "OfferDataToFilterStateTransformer" should "work in transformer Offer VOS Data → Filters State SELL_GARAGE" in {
    val jsObject = offerDataToFilterStateTransformer
      .transform(OfferDataSellGarage2)
    println(Json.prettyPrint(jsObject))
    jsObject.value should contain theSameElementsAs (FilterStateEditSellGarage.value)
  }
}
