package ru.yandex.realty.persistence.cassandra

import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.model.serialization.{MockOfferBuilder, MockRawOfferBuilder}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 18.07.16
  */
trait OfferSupport {

  def buildRawOffer(offerId: String): RawOfferImpl = {
    val raw = MockRawOfferBuilder.createMockRawOfferOld()
    raw.setId(offerId)
    raw
  }

  def buildOffer(offerId: String): Offer = {
    val offer = MockOfferBuilder.createMockOffer()
    offer.setId(offerId.toLong)
    offer
  }
}
