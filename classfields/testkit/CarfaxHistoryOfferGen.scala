package ru.auto.comeback.model.testkit

import java.time.Instant

import ru.auto.api.api_offer_model.Category
import ru.auto.comeback.model.carfax.CarfaxHistoryOffer
import ru.auto.comeback.model.testkit.CommonGen.{anyInstant, anySection}
import ru.auto.comeback.model.testkit.OfferGen.anyOfferId
import ru.auto.comeback.model.testkit.UserRefGen.anyUserRef
import zio.random.Random
import zio.test.Gen.anyLong
import zio.test.{Gen, Sized}

object CarfaxHistoryOfferGen {

  def offer[R <: Random with Sized](
      userRef: Gen[R, String] = anyUserRef,
      clientId: Option[Gen[R, Long]] = Some(anyLong),
      category: Gen[R, Category] = Gen.const(Category.CARS),
      activated: Gen[R, Instant] = anyInstant,
      deactivated: Option[Gen[R, Instant]] = Some(anyInstant)): Gen[R, CarfaxHistoryOffer] =
    for {
      id <- anyOfferId
      userRef <- userRef
      clientId <- clientId.map(gen => gen.map(Some.apply)).getOrElse(Gen.const(None))
      category <- category
      section <- anySection
      activated <- activated
      deactivated <- deactivated.map(gen => gen.map(Some.apply)).getOrElse(Gen.const(None))
    } yield CarfaxHistoryOffer(
      id,
      userRef,
      clientId,
      category,
      section,
      activated,
      deactivated
    )
}
