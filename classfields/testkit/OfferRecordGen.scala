package ru.auto.comeback.model.testkit

import ru.auto.api.api_offer_model.Category
import ru.auto.api.vin.vin_report_model.OfferRecord
import ru.auto.comeback.model.testkit.OfferGen.anyCategory
import ru.auto.comeback.model.testkit.UserRefGen.anyUserRef
import zio.random.Random
import zio.test.{Gen, Sized}

object OfferRecordGen {

  def offer[R <: Random with Sized](
      userRef: Gen[R, String] = anyUserRef,
      category: Gen[R, Category] = anyCategory): Gen[R, OfferRecord] =
    for {
      userRef <- userRef
      category <- category
    } yield OfferRecord()
      .withUserRef(userRef)
      .withCategory(category)
}
