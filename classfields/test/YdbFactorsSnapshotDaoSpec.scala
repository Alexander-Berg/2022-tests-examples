package ru.yandex.vertis.billing.emon.storage.ydb

import billing.common_model.{Money, Project}
import billing.emon.internal.model.FactorsWithMeta
import billing.emon.model.EventTypeNamespace.EventType
import billing.emon.model.PriceResponse.Rule
import billing.emon.model.{EventId, Factors, PriceInfo, PriceResponse}
import cats.data.NonEmptyList
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.emon.model.FactorsSnapshot
import ru.yandex.vertis.billing.emon.storage.FactorsSnapshotDao
import zio.stream.ZSink
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object YdbFactorsSnapshotDaoSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("YdbFactorsSnapshotDao")(
      testM("select last upserted factors") {

        val eventId1 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val factors11 = FactorsSnapshot(
          eventId1,
          snapshotId = 1,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(1),
          None
        )

        val factors12 = FactorsSnapshot(
          eventId1,
          snapshotId = 2,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(2),
          None
        )

        val eventId2 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "2")
        val factors21 = FactorsSnapshot(
          eventId2,
          snapshotId = 1,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(2),
          None
        )

        for {
          _ <- runTx(FactorsSnapshotDao.upsert(NonEmptyList.of(factors11, factors12, factors21)))
          eventId1Select <- runTx(FactorsSnapshotDao.selectLast(NonEmptyList.one(eventId1)))
          eventId2Select <- runTx(FactorsSnapshotDao.selectLast(NonEmptyList.one(eventId2)))
          eventId1and2Select <- runTx(FactorsSnapshotDao.selectLast(NonEmptyList.of(eventId1, eventId2)))
        } yield {
          assert(eventId1Select)(equalTo(Seq(factors12))) &&
          assert(eventId2Select)(equalTo(Seq(factors21))) &&
          assert(eventId1and2Select)(equalTo(Seq(factors12, factors21)))
        }
      },
      testM("set price to factors") {

        val eventId1 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val factors11 = FactorsSnapshot(
          eventId1,
          snapshotId = 1,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(1),
          None
        )

        val factors12 = FactorsSnapshot(
          eventId1,
          snapshotId = 2,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(2),
          None
        )

        val eventId2 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "2")
        val factors21 = FactorsSnapshot(
          eventId2,
          snapshotId = 1,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(2),
          None
        )

        val pr11 = PriceInfo().withResponse(PriceResponse().withRule(Rule().withId("41rule").withPrice(Money(41L))))
        val pr12 = PriceInfo().withResponse(PriceResponse().withRule(Rule().withId("42rule").withPrice(Money(42L))))
        val pr21 = PriceInfo().withResponse(PriceResponse().withRule(Rule().withId("242rule").withPrice(Money(242L))))
        for {
          _ <- runTx(FactorsSnapshotDao.upsert(NonEmptyList.of(factors11, factors12, factors21)))
          _ <- runTx(FactorsSnapshotDao.setPrice(eventId1, 1, pr11))
          _ <- runTx(FactorsSnapshotDao.setPrice(eventId1, 2, pr12))
          _ <- runTx(FactorsSnapshotDao.setPrice(eventId2, 1, pr21))
          eventId1and2Select <- runTx(FactorsSnapshotDao.selectLast(NonEmptyList.of(eventId1, eventId2)))
        } yield {
          assert(eventId1and2Select)(
            equalTo(
              Seq(
                factors12.copy(price = Some(pr12)),
                factors21.copy(price = Some(pr21))
              )
            )
          )
        }
      },
      testM("select all upserted factors") {
        val eventId1 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val factors1List = (1 to 2000).map { i =>
          FactorsSnapshot(
            eventId1,
            snapshotId = i,
            Factors.defaultInstance,
            FactorsWithMeta.defaultInstance,
            Instant.ofEpochMilli(i),
            None
          )
        }.toList

        val eventId2 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "2")
        val factors2List = (1 to 2000).map { i =>
          FactorsSnapshot(
            eventId2,
            snapshotId = i,
            Factors.defaultInstance,
            FactorsWithMeta.defaultInstance,
            Instant.ofEpochMilli(i),
            None
          )
        }.toList

        for {
          _ <- runTx(FactorsSnapshotDao.upsert(NonEmptyList.fromListUnsafe(factors1List)))
          _ <- runTx(FactorsSnapshotDao.upsert(NonEmptyList.fromListUnsafe(factors2List)))
          eventId1Select <- runTx(FactorsSnapshotDao.selectAll(eventId1).run(ZSink.collectAll).map(_.toList))
          eventId2Select <- runTx(FactorsSnapshotDao.selectAll(eventId2).run(ZSink.collectAll).map(_.toList))
        } yield {
          assert(eventId1Select)(equalTo(factors1List)) &&
          assert(eventId2Select)(equalTo(factors2List))
        }
      },
      testM("select particular factors snapshot") {

        val eventId1 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val factors11 = FactorsSnapshot(
          eventId1,
          snapshotId = 1,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(1),
          None
        )

        val factors12 = FactorsSnapshot(
          eventId1,
          snapshotId = 2,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(2),
          None
        )

        val eventId2 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "2")
        val factors21 = FactorsSnapshot(
          eventId2,
          snapshotId = 1,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(1),
          None
        )
        val factors22 = FactorsSnapshot(
          eventId2,
          snapshotId = 2,
          Factors.defaultInstance,
          FactorsWithMeta.defaultInstance,
          Instant.ofEpochMilli(2),
          None
        )

        for {
          _ <- runTx(FactorsSnapshotDao.upsert(NonEmptyList.of(factors11, factors12, factors21, factors22)))
          eventId11Select <- runTx(FactorsSnapshotDao.select(eventId1, 1))
          eventId12Select <- runTx(FactorsSnapshotDao.select(eventId1, 2))
          eventId21Select <- runTx(FactorsSnapshotDao.select(eventId2, 1))
          eventId22Select <- runTx(FactorsSnapshotDao.select(eventId2, 2))
        } yield {
          assert(eventId11Select)(equalTo(Some(factors11))) &&
          assert(eventId12Select)(equalTo(Some(factors12))) &&
          assert(eventId21Select)(equalTo(Some(factors21))) &&
          assert(eventId22Select)(equalTo(Some(factors22)))
        }
      }
    ) @@ sequential @@ before(TestYdb.clean(YdbFactorsSnapshotDao.TableName))
  }.provideCustomLayerShared {
    TestYdb.ydb >+> YdbFactorsSnapshotDao.live ++ Ydb.txRunner
  }
}
