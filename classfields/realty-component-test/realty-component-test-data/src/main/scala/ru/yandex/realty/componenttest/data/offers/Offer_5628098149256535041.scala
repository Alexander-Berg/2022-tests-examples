package ru.yandex.realty.componenttest.data.offers

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.{
  extractIdFromClassName,
  loadProtoFromJsonResource
}
import ru.yandex.realty.model.message.RealtySchema.OfferMessage
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.serialization.OfferProtoConverter

object Offer_5628098149256535041 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: OfferMessage = loadProtoFromJsonResource[OfferMessage](s"offers/offer_$Id.json")

  val Model: Offer = OfferProtoConverter.fromMessage(Proto)

}
