package ru.yandex.vertis.billing.emon.scheduler

import billing.common_model.{Money, Project}
import billing.emon.internal.model.FactorsWithMeta
import billing.emon.model.EventTypeNamespace.EventType
import billing.emon.model.{Event, EventId, EventPayload, Factors, Payer, PriceInfo, PriceResponse}
import cats.data.NonEmptyList
import common.scalapb.ScalaProtobuf.instantToTimestamp
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.realty.developer.chat.event.event.RealtyDevchatBillingEvent
import ru.yandex.vertis.billing.emon.model.FactorsSnapshot
import ru.yandex.vertis.billing.emon.storage.ydb.{YdbEventDao, YdbFactorsSnapshotDao, YdbTaskQueueDao}
import ru.yandex.vertis.billing.emon.storage.{EventDao, FactorsSnapshotDao}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{before, _}
import zio.test._

import java.time.Instant

object EventStateBuilderSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("EventStateBuilder")(
      testM("build valid EventState") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance.withRealtyDevchat(RealtyDevchatBillingEvent.defaultInstance))
        )
        val factors = Factors().withHobo(ru.yandex.vertis.hobo.proto.model.Task(1).withComment("New task"))
        val factorsWithMeta = Factors().withHobo(ru.yandex.vertis.hobo.proto.model.Task(2).withComment("New task"))
        val factorsSnapshot = FactorsSnapshot(
          eventId,
          snapshotId = 1,
          factors,
          FactorsWithMeta(Some(factorsWithMeta), Map.empty),
          Instant.ofEpochMilli(2),
          Some(
            PriceInfo().withResponse(
              PriceResponse().withRule(PriceResponse.Rule().withId("some123").withPrice(Money(42L)))
            )
          )
        )

        for {
          _ <- runTx(EventDao.upsert(NonEmptyList.of(event)))
          _ <- runTx(FactorsSnapshotDao.upsert(NonEmptyList.of(factorsSnapshot)))
          eventState0 <- runTx(EventStateBuilder.buildEventState(eventId, 0L))
          eventState1 <- runTx(EventStateBuilder.buildEventState(eventId, 1L))
        } yield {
          assert(eventState0)(equalTo(None)) &&
          assert(eventState1.map(_.getEvent))(equalTo(Some(event))) &&
          assert(eventState1.map(_.getPrice))(equalTo(factorsSnapshot.price)) &&
          assert(eventState1.map(_.getFactors))(equalTo(factorsSnapshot.factorsSnapshot.factors)) &&
          assert(eventState1.map(_.snapshotId))(equalTo(Some(1L))) &&
          assert(eventState1.map(_.getTimestamp))(equalTo(Some(instantToTimestamp(factorsSnapshot.createTime))))
        }
      }
    ) @@ sequential @@ before(
      TestYdb.clean(YdbTaskQueueDao.TableName) *>
        TestYdb.clean(YdbFactorsSnapshotDao.TableName) *>
        TestYdb.clean(YdbEventDao.TableName)
    )
  }.provideCustomLayerShared {
    val ydb = TestYdb.ydb
    val txRunner = ydb >>> Ydb.txRunner
    val eventDao = ydb >>> YdbEventDao.live
    val factorsDao = ydb >>> YdbFactorsSnapshotDao.live
    val taskDao = ydb >>> YdbTaskQueueDao.live
    val eventStateBuilder = eventDao ++ factorsDao >>> DefaultEventStateBuilder.live
    ydb ++ eventDao ++ txRunner ++ factorsDao ++ taskDao ++ eventStateBuilder ++ Clock.live
  }
}
