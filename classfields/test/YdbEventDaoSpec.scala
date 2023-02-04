package ru.yandex.vertis.billing.emon.storage.ydb

import billing.common_model.Project
import billing.emon.model.EventTypeNamespace.EventType
import billing.emon.model.Payer.PaymentMethod
import billing.emon.model.{Event, EventId, EventPayload, Payer}
import cats.data.NonEmptyList
import common.scalapb.ScalaProtobuf.instantToTimestamp
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.emon.storage.EventDao
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object YdbEventDaoSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("YdbEventDao")(
      testM("select upserted events") {
        val eventId1 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event1 = Event(
          Some(eventId1),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )
        val eventId2 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "2")
        val event2 = Event(
          Some(eventId2),
          Some(instantToTimestamp(Instant.ofEpochMilli(2))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(768, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )
        val eventId3 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "3")
        val event3 = Event(
          Some(eventId3),
          Some(instantToTimestamp(Instant.ofEpochMilli(3))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(1223423, Some(42))), 456L))),
          Some(EventPayload.defaultInstance)
        )

        for {
          _ <- runTx(EventDao.upsert(NonEmptyList.of(event1, event2, event3)))
          eventId1Select <- runTx(EventDao.select(NonEmptyList.one(eventId1)))
          eventId2Select <- runTx(EventDao.select(NonEmptyList.one(eventId2)))
          eventId3Select <- runTx(EventDao.select(NonEmptyList.one(eventId3)))
        } yield {
          assert(eventId1Select)(equalTo(Seq(event1))) &&
          assert(eventId2Select)(equalTo(Seq(event2))) &&
          assert(eventId3Select)(equalTo(Seq(event3)))
        }
      }
    ) @@ sequential @@ before(TestYdb.clean(YdbEventDao.TableName))
  }.provideCustomLayerShared {
    TestYdb.ydb >+> YdbEventDao.live ++ Ydb.txRunner
  }
}
