package ru.auto.cabinet.json

import org.scalatest.wordspec.{AnyWordSpec => WordSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.model.multiposting.ExternalOfferEvent
import ru.auto.cabinet.model.multiposting.ExternalOfferEvent.Counters
import spray.json._

class ExternalOfferEventProtocolSpec
    extends WordSpec
    with Matchers
    with ExternalOfferEventProtocol {
  "ExternalOfferEvent protocol" should {
    "parse json string" in {
      val json =
        """{"timestamp":1,"autoru_client_id":"clientId","vin":"vin","offer_id":"offerId","source":"source","counters":{"date":"2020-10-14","views":1,"phones_views":2}}"""
      val expectedEvent = ExternalOfferEvent(
        1,
        "clientId",
        "vin",
        "offerId",
        "source",
        Counters(1, 2))

      val parsedEvent = json.parseJson.convertTo[ExternalOfferEvent]

      parsedEvent shouldBe expectedEvent
    }

    "parse json string with negative counters" in {
      val json =
        """{"timestamp":1,"autoru_client_id":"clientId","vin":"vin","offer_id":"offerId","source":"source","counters":{"date":"2020-10-14","views":-1,"phones_views":-2}}"""
      val expectedEvent = ExternalOfferEvent(
        1,
        "clientId",
        "vin",
        "offerId",
        "source",
        Counters(0, 0))

      val parsedEvent = json.parseJson.convertTo[ExternalOfferEvent]

      parsedEvent shouldBe expectedEvent
    }
  }
}
