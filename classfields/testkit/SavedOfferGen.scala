package ru.yandex.vertis.general.favorites.model.testkit

import ru.yandex.vertis.general.favorites.model.SavedOffer
import zio.random.Random
import zio.test.{Gen, Sized}

object SavedOfferGen {

  val anyOfferId: Gen[Random with Sized, String] = {
    Gen.anyUUID.map(uuid => uuid.toString).noShrink
  }

  def anySavedOffer(offerId: Gen[Random with Sized, String] = anyOfferId): Gen[Random with Sized, SavedOffer] =
    for {
      offerId <- offerId
    } yield SavedOffer(offerId = offerId)

  val anySavedOffer: Gen[Random with Sized, SavedOffer] = anySavedOffer().noShrink

  def anySavedOffers(count: Int): Gen[Random with Sized, List[SavedOffer]] =
    Gen.listOfN(count)(anySavedOffer).noShrink
}
