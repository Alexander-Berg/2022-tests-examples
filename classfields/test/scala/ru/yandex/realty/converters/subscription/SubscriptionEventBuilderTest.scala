package ru.yandex.realty.converters.subscription

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.Offer

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionEventBuilderTest extends FlatSpec with Matchers {

  "SubscriptionEventBuilder " should "set raw address if CombinedAddress is null " in {
    val offer = new Offer()
    val location = new Location()
    location.setRawAddress("Some raw address")
    offer.setLocation(location)

    val result = SubscriptionEventBuilder.buildOfferPriceChangedEvent(offer)

    result.getOfferPriceChanged.getOffer.getLocation.getAddress should be(offer.getLocation.getRawAddress)
  }

  "SubscriptionEventBuilder " should "set CombinedAddress if this address exists " in {
    val offer = new Offer()
    val location = new Location()
    location.setCombinedAddress("Some combined address")
    location.setRawAddress("Some raw address")
    offer.setLocation(location)

    val result = SubscriptionEventBuilder.buildOfferPriceChangedEvent(offer)

    result.getOfferPriceChanged.getOffer.getLocation.getAddress should be(offer.getLocation.getCombinedAddress)
  }
}
