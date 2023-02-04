package ru.auto.cm_sink.storage

import common.zio.logging.Logging
//import ru.auto.api.api_offer_model.Offer
//import ru.auto.api.diff_log_model.OfferChangeEvent
import ru.auto.cm_sink.storage.testkit.StringProducerMock
//import scalapb.json4s.JsonFormat.toJson
import zio.sqs.producer.ProducerEvent
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object SqsEventsUpdatesRepositorySpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("SqsEventsUpdatesRepository")(
      testM("send offer updates should complete") {
        val jsonWithEscapedDescription = """{"newOffer":{"description":"different&amp;#xa;line\\nfeeds"}}"""
        val expectedEvent = ProducerEvent(jsonWithEscapedDescription)
        val producer = StringProducerMock.Produce(
          equalTo(expectedEvent),
          value(expectedEvent)
        )

        assertM(CmUpdatesRepository.sendUpdates(jsonWithEscapedDescription))(isUnit)
          .provideCustomLayer(
            (producer.toLayer ++ Logging.live) >>> SqsEventsUpdatesRepository.live
          )
      }
    )
  }
}
