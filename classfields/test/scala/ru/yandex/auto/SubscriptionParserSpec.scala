package ru.yandex.auto

import org.junit.Test
import ru.auto.api.ApiOfferModel
import ru.yandex.auto.converters.OfferToSubscriptionConverter
import ru.yandex.auto.core.model.CarAd
import ru.yandex.auto.utils.JsonFormat

import scala.io.Source.fromInputStream

class SubscriptionParserSpec extends SimpleAssertions {

  private lazy val offerToSubscriptionConverter =
    new OfferToSubscriptionConverter(null, null)(null, null)

  private def read(name: String) = {
    val s = getClass.getResourceAsStream("/offers/" + name)
    val string = fromInputStream(s).mkString
    val builder = ApiOfferModel.Offer.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(string, builder)
    builder.build()
  }

  @Test
  def test() {
    validate("offer1103394512.json")
    validate("offer_no_id.json")
  }

  private def validate(value: String) = {
    val apiOffer = read(value)
    val carad = CarAd.fromMessage(offerToSubscriptionConverter.convertToCarad(apiOffer))
    println(carad)
  }
}
