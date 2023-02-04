package ru.auto.api.util

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
class ProtobufTest extends BaseSpec with ScalaCheckPropertyChecks {

  "Protobuf" should {
    "round-trip to json" in {
      forAll(ModelGenerators.OfferGen)(offer => Protobuf.fromJson[Offer](Protobuf.toJson(offer)) shouldBe offer)
    }
  }
}
