package auto.dealers.multiposting.model.test

import auto.dealers.multiposting.model._
import auto.dealers.multiposting.model.ClassifiedStatisticUpdateEvent.Counters
import io.circe.parser._
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object ClassifiedStatisticUpdateEventParsingSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("ExternalOfferEvent")(
    test("parse json string") {
      val json =
        """{"timestamp":1,"autoru_client_id":"1","vin":"vin","offer_id":"offerId","source":"source","counters":{"date":"2020-10-14","views":1,"phones_views":2}}"""
      val expectedEvent =
        ClassifiedStatisticUpdateEvent(1, ClientId(1), Vin("vin"), OfferId("offerId"), Source("source"), Counters(1, 2))

      assert(decode[ClassifiedStatisticUpdateEvent](json))(isRight(equalTo(expectedEvent)))
    },
    test("parse json string with negative counters") {
      val json =
        """{"timestamp":1,"autoru_client_id":"1","vin":"vin","offer_id":"offerId","source":"source","counters":{"date":"2020-10-14","views":-1,"phones_views":-2}}"""
      val expectedEvent =
        ClassifiedStatisticUpdateEvent(1, ClientId(1), Vin("vin"), OfferId("offerId"), Source("source"), Counters(0, 0))

      assert(decode[ClassifiedStatisticUpdateEvent](json))(isRight(equalTo(expectedEvent)))
    }
  ) @@ sequential
}
