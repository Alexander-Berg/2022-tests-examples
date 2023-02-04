package auto.dealers.multiposting.scheduler.test

import com.google.protobuf.timestamp.Timestamp
import auto.common.clients.vos.Vos.OwnerId.DealerId
import auto.common.clients.vos.testkit.VosTest
import auto.dealers.multiposting.scheduler.task.ExternalOfferStatisticToBrokerTask
import auto.dealers.multiposting.storage.testkit._
import auto.dealers.multiposting.clients.s3.testkit.S3MultipostingClientMock
import auto.dealers.multiposting.model._
import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.multiposting.scheduler.config.OfferStatisticToBrokerConfig
import auto.dealers.multiposting.scheduler.testkit.BrokerTypedMock
import auto.events.model.ClassifiedStatisticUpdateEvent
import cats.data.NonEmptySet
import common.zio.events_broker.Broker
import io.prometheus.client.CollectorRegistry
import ru.auto.api.response_model.OfferIdsByVinsResponse
import common.ops.prometheus.CollectorRegistryWrapper
import auto.dealers.multiposting.storage.testkit.ExternalStatisticCounterDaoMock
import zio.ZLayer
import zio.clock.Clock
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.mock.Expectation._
import zio.test.{assert, assertM, DefaultRunnableSpec, ZSpec}

object ExternalOfferStatisticToBrokerTaskSpec extends DefaultRunnableSpec {
  val task = new ExternalOfferStatisticToBrokerTask(new CollectorRegistryWrapper(CollectorRegistry.defaultRegistry))

  val cfg = ZLayer.succeed(OfferStatisticToBrokerConfig("bucket"))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ExternalOfferStatisticToBrokerTask")(
      testM("process file without saved counter state") {
        val offerId = OfferId("1-offerId1")
        val source = Source("src1")
        val clientId = ClientId(1)
        val vin = Vin("vin1")

        val expectedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          10,
          10,
          0
        )

        val expectedMessage = ClassifiedStatisticUpdateEvent()
          .withCardId(1)
          .withOfferId(offerId.value)
          .withCategory(Category.CARS)
          .withSection(Section.NEW)
          .withClientId(clientId.value)
          .withVin(vin.value)
          .withTimestamp(Timestamp(seconds = 1))
          .withSource(source.value)
          .withViewsDelta(10)
          .withPhoneViewsDelta(10)
          .withExternalOfferId("1")

        val json =
          """{"timestamp":1,"autoru_client_id":"1","vin":"vin1","offer_id":"1","source":"src1","counters":{"date":"2020-10-14","views":10,"phones_views":10}}"""

        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(None)
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_1")), unit)
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(equalTo((offerId, source)), value(None)) ++
            ExternalStatisticCounterDaoMock.Upsert(equalTo(expectedState), unit)
        val s3 = S3MultipostingClientMock.ListNewObjects(equalTo(("bucket", "views", None)), value(List("views_1"))) ++
          S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_1")), value(ZStream.succeed(json)))
        val vos = VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId.value), NonEmptySet.one(vin.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId.value, Category.CARS, Section.NEW))
            )
          )
        )
        val broker = BrokerTypedMock.Send(equalTo((expectedMessage, None, None)), unit)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      },
      testM("update counter state even after broker client send retries") {
        val offerId = OfferId("1-offerId1")
        val source = Source("src1")
        val clientId = ClientId(1)
        val vin = Vin("vin1")

        val expectedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          10,
          10,
          0
        )

        val expectedMessage = ClassifiedStatisticUpdateEvent()
          .withCardId(1)
          .withOfferId(offerId.value)
          .withCategory(Category.TRUCKS)
          .withSection(Section.USED)
          .withClientId(clientId.value)
          .withVin(vin.value)
          .withTimestamp(Timestamp(seconds = 1))
          .withSource(source.value)
          .withViewsDelta(10)
          .withPhoneViewsDelta(10)
          .withExternalOfferId("1")

        val json =
          """{"timestamp":1,"autoru_client_id":"1","vin":"vin1","offer_id":"1","source":"src1","counters":{"date":"2020-10-14","views":10,"phones_views":10}}"""

        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(None)
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_1")), unit)
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(equalTo((offerId, source)), value(None)) ++
            ExternalStatisticCounterDaoMock.Upsert(equalTo(expectedState), unit)
        val s3 = S3MultipostingClientMock.ListNewObjects(equalTo(("bucket", "views", None)), value(List("views_1"))) ++
          S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_1")), value(ZStream.succeed(json)))
        val vos = VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId.value), NonEmptySet.one(vin.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId.value, Category.TRUCKS, Section.USED))
            )
          )
        )

        val broker = BrokerTypedMock.Send(
          equalTo((expectedMessage, None, None)),
          failure(Broker.TooManyRequests("Some error"))
        ) ++ BrokerTypedMock.Send(
          equalTo((expectedMessage, None, None)),
          unit
        )

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      },
      testM("do not update state if message is too old (to not lose deltas)") {
        val offerId = OfferId("1-offerId1")
        val source = Source("src1")
        val clientId = ClientId(1)
        val vin = Vin("vin1")

        val expectedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          10,
          10,
          0
        )

        val expectedMessage = ClassifiedStatisticUpdateEvent()
          .withCardId(1)
          .withOfferId(offerId.value)
          .withCategory(Category.MOTO)
          .withSection(Section.NEW)
          .withClientId(clientId.value)
          .withVin(vin.value)
          .withTimestamp(Timestamp(seconds = 1))
          .withSource(source.value)
          .withViewsDelta(10)
          .withPhoneViewsDelta(10)
          .withExternalOfferId("1")

        val json =
          """{"timestamp":1,"autoru_client_id":"1","vin":"vin1","offer_id":"1","source":"src1","counters":{"date":"2020-10-14","views":10,"phones_views":10}}"""

        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(None)
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_1")), unit)
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(equalTo((offerId, source)), value(None)) ++
            ExternalStatisticCounterDaoMock.Upsert(equalTo(expectedState), unit).atMost(0)
        val s3 = S3MultipostingClientMock.ListNewObjects(equalTo(("bucket", "views", None)), value(List("views_1"))) ++
          S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_1")), value(ZStream.succeed(json)))
        val vos = VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId.value), NonEmptySet.one(vin.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId.value, Category.MOTO, Section.NEW))
            )
          )
        )

        val broker = BrokerTypedMock.Send(
          equalTo((expectedMessage, None, None)),
          failure(Broker.MessageIsTooOld("Message is too old error"))
        )

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      },
      testM("do not update state if broker send was failed and do not save file as processed") {
        val offerId = OfferId("1-offerId1")
        val source = Source("src1")
        val clientId = ClientId(1)
        val vin = Vin("vin1")

        val expectedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          10,
          10,
          0
        )

        val expectedMessage = ClassifiedStatisticUpdateEvent()
          .withCardId(1)
          .withOfferId(offerId.value)
          .withCategory(Category.CARS)
          .withSection(Section.NEW)
          .withClientId(clientId.value)
          .withVin(vin.value)
          .withTimestamp(Timestamp(seconds = 1))
          .withSource(source.value)
          .withViewsDelta(10)
          .withPhoneViewsDelta(10)
          .withExternalOfferId("1")

        val json =
          """{"timestamp":1,"autoru_client_id":"1","vin":"vin1","offer_id":"1","source":"src1","counters":{"date":"2020-10-14","views":10,"phones_views":10}}"""

        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(None)
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_1")), unit).atMost(0)
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(equalTo((offerId, source)), value(None)) ++
            ExternalStatisticCounterDaoMock.Upsert(equalTo(expectedState), unit).atMost(0)
        val s3 = S3MultipostingClientMock.ListNewObjects(equalTo(("bucket", "views", None)), value(List("views_1"))) ++
          S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_1")), value(ZStream.succeed(json)))
        val vos = VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId.value), NonEmptySet.one(vin.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId.value, Category.CARS, Section.NEW))
            )
          )
        )

        val broker = BrokerTypedMock
          .Send(
            equalTo((expectedMessage, None, None)),
            failure(Broker.TooManyRequests("Some Error"))
          ) ++ BrokerTypedMock
          .Send(
            equalTo((expectedMessage, None, None)),
            failure(Broker.TooManyRequests("Some Error"))
          ) ++ BrokerTypedMock
          .Send(
            equalTo((expectedMessage, None, None)),
            failure(Broker.TooManyRequests("Some Error"))
          )

        val runnable = for {
          r <- task.program.run
        } yield {
          assert(r.toEither)(isLeft)
        }

        runnable
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      },
      testM("process file with saved counter state") {
        val offerId = OfferId("1-offerId1")
        val source = Source("src1")
        val clientId = ClientId(1)
        val vin = Vin("vin1")

        val savedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          5,
          5,
          0
        )

        val expectedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          10,
          10,
          0
        )

        val expectedMessage = ClassifiedStatisticUpdateEvent()
          .withCardId(1)
          .withOfferId(offerId.value)
          .withCategory(Category.CARS)
          .withSection(Section.NEW)
          .withClientId(clientId.value)
          .withVin(vin.value)
          .withTimestamp(Timestamp(seconds = 1))
          .withSource(source.value)
          .withViewsDelta(5) // saved: 5, new: 10
          .withPhoneViewsDelta(5) // saved: 5, new: 10
          .withExternalOfferId("1")

        val json =
          """{"timestamp":1,"autoru_client_id":"1","vin":"vin1","offer_id":"1","source":"src1","counters":{"date":"2020-10-14","views":10,"phones_views":10}}"""

        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(None)
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_1")), unit)
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(equalTo((offerId, source)), value(Some(savedState))) ++
            ExternalStatisticCounterDaoMock.Upsert(equalTo(expectedState), unit)
        val s3 = S3MultipostingClientMock.ListNewObjects(equalTo(("bucket", "views", None)), value(List("views_1"))) ++
          S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_1")), value(ZStream.succeed(json)))
        val vos = VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId.value), NonEmptySet.one(vin.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId.value, Category.CARS, Section.NEW))
            )
          )
        )
        val broker = BrokerTypedMock.Send(equalTo((expectedMessage, None, None)), unit)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      },
      testM("process file with saved counter state (negative delta)") {
        val offerId = OfferId("1-offerId1")
        val source = Source("src1")
        val clientId = ClientId(1)
        val vin = Vin("vin1")

        val savedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          100,
          100,
          0
        )

        val expectedState = ExternalStatisticCounterRecord(
          offerId,
          source,
          clientId,
          10,
          10,
          0
        )

        val expectedMessage = ClassifiedStatisticUpdateEvent()
          .withCardId(1)
          .withOfferId(offerId.value)
          .withCategory(Category.CARS)
          .withSection(Section.NEW)
          .withClientId(clientId.value)
          .withVin(vin.value)
          .withTimestamp(Timestamp(seconds = 1))
          .withSource(source.value)
          .withViewsDelta(-90) // saved: 100, new: 10
          .withPhoneViewsDelta(-90) // saved: 100, new: 10
          .withExternalOfferId("1")

        val json =
          """{"timestamp":1,"autoru_client_id":"1","vin":"vin1","offer_id":"1","source":"src1","counters":{"date":"2020-10-14","views":10,"phones_views":10}}"""

        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(None)
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_1")), unit)
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(equalTo((offerId, source)), value(Some(savedState))) ++
            ExternalStatisticCounterDaoMock.Upsert(equalTo(expectedState), unit)
        val s3 = S3MultipostingClientMock.ListNewObjects(equalTo(("bucket", "views", None)), value(List("views_1"))) ++
          S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_1")), value(ZStream.succeed(json)))
        val vos = VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId.value), NonEmptySet.one(vin.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId.value, Category.CARS, Section.NEW))
            )
          )
        )
        val broker = BrokerTypedMock.Send(equalTo((expectedMessage, None, None)), unit)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      },
      testM("process multiple files sequentially") {
        val offerId1 = OfferId("1-offerId1")
        val source1 = Source("src1")
        val clientId1 = ClientId(1)
        val vin1 = Vin("vin1")

        val offerId2 = OfferId("2-offerId2")
        val source2 = Source("src1")
        val clientId2 = ClientId(2)
        val vin2 = Vin("vin2")

        val json1 =
          """{"timestamp":2,"autoru_client_id":"1","vin":"vin1","offer_id":"1","source":"src1","counters":{"date":"2020-10-14","views":15,"phones_views":15}}"""
        val json2 =
          """{"timestamp":2,"autoru_client_id":"2","vin":"vin2","offer_id":"2","source":"src1","counters":{"date":"2020-10-14","views":15,"phones_views":15}}"""

        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(None)
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_1")), unit) ++
          ExternalOfferEventFileDaoMock.InsertNewFilename(equalTo((EventType.Statistics, "views_2")), unit)
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(equalTo((offerId1, source1)), value(None)) ++
            ExternalStatisticCounterDaoMock.Upsert(anything, unit) ++
            ExternalStatisticCounterDaoMock.Find(equalTo((offerId2, source2)), value(None)) ++
            ExternalStatisticCounterDaoMock.Upsert(anything, unit)
        val s3 =
          S3MultipostingClientMock.ListNewObjects(
            equalTo(("bucket", "views", None)),
            value(List("views_1", "views_2"))
          ) ++
            S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_1")), value(ZStream.succeed(json1))) ++
            S3MultipostingClientMock.ReadLines(equalTo(("bucket", "views_2")), value(ZStream.succeed(json2)))
        val vos = VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId1.value), NonEmptySet.one(vin1.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin1.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId1.value, Category.CARS, Section.NEW))
            )
          )
        ) ++ VosTest.GetDetailedOfferIds(
          equalTo((DealerId(clientId2.value), NonEmptySet.one(vin2.value), false)),
          value(
            OfferIdsByVinsResponse(
              Map(vin2.value -> OfferIdsByVinsResponse.OfferIdByVin(offerId2.value, Category.CARS, Section.NEW))
            )
          )
        )
        val broker = BrokerTypedMock.Send(anything, unit) ++ BrokerTypedMock.Send(anything, unit)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      },
      testM("filter processed files") {
        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.Statistics),
          value(Some("views_2"))
        )
        val countersDao =
          ExternalStatisticCounterDaoMock.Find(anything, value(None)).atMost(0)
        val s3 =
          S3MultipostingClientMock.ListNewObjects(equalTo(("bucket", "views", Some("views_2"))), value(List.empty))
        val vos = VosTest.GetDetailedOfferIds(anything, value(OfferIdsByVinsResponse.defaultInstance)).atMost(0)

        val broker = BrokerTypedMock.Send(anything, unit).atMost(0)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ countersDao ++ s3 ++ vos ++ broker ++ cfg)
      }
    ) @@ sequential
}
