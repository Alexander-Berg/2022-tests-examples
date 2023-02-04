package ru.yandex.vertis.billing.emon.scheduler

import billing.common_model.{Money, Project}
import billing.emon.internal.model.SendEventStateTask
import billing.emon.model.EventTypeNamespace.EventType
import billing.emon.model.{Event, EventId, EventPayload, EventState, Factors, Payer, PriceInfo, PriceResponse}
import cats.data.NonEmptyList
import common.scalapb.ScalaProtobuf.instantToTimestamp
import common.sraas.Sraas.SraasDescriptor
import common.sraas.TestSraas
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.realty.developer.chat.event.event.RealtyDevchatBillingEvent
import ru.yandex.vertis.billing.emon.model.Task
import ru.yandex.vertis.billing.emon.model.Task.{TaskId, TaskType}
import ru.yandex.vertis.billing.emon.scheduler.EventStateSender.Postpone
import ru.yandex.vertis.billing.emon.storage.TaskQueueDao
import ru.yandex.vertis.billing.emon.storage.ydb.YdbTaskQueueDao
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.test.mock.Expectation.unit
import zio.test.mock._

import java.time.Instant

object DefaultEventStateSenderSpec extends DefaultRunnableSpec {

  private val baseLayer = {
    val ydb = TestYdb.ydb
    val txRunner = ydb >>> Ydb.txRunner
    val taskDao = ydb >>> YdbTaskQueueDao.live
    val sraas = TestSraas.layer
    ydb ++ txRunner ++ taskDao ++ Clock.live ++ sraas
  }

  override def spec = {
    suite("DefaultEventStateSender")(
      testM("send EventState from task") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance.withRealtyDevchat(RealtyDevchatBillingEvent.defaultInstance))
        )
        val factors = Factors().withHobo(ru.yandex.vertis.hobo.proto.model.Task(1).withComment("New task"))

        val eventState = EventState(
          snapshotId = 1,
          event = Some(event),
          factors = Some(factors),
          price = Some(
            PriceInfo().withResponse(
              PriceResponse().withRule(PriceResponse.Rule().withId("some123").withPrice(Money(42L)))
            )
          ),
          timestamp = Some(instantToTimestamp(Instant.ofEpochMilli(2)))
        )

        val task = Task(
          TaskId(1, TaskType.SendEventState, Instant.ofEpochMilli(1), "1"),
          SendEventStateTask().withSnapshotId(eventState.snapshotId).withEventId(eventId).toByteArray
        )

        val schemaVersion = "v0.0.42"
        val broker = EventStateBrokerMock.Send(equalTo((eventState, None, Some(schemaVersion))), unit)
        val eventBuilder =
          EventStateBuilderMock.BuildEventState(
            equalTo((eventId, eventState.snapshotId)),
            Expectation.value(Some(eventState))
          )

        val effect = for {
          _ <- TestSraas.setJavaDescriptor(key =>
            zio.Task(SraasDescriptor(Factors.javaDescriptor, key.protoMessageName, schemaVersion))
          )
          _ <- runTx(TaskQueueDao.add(NonEmptyList.of(task)))
          sendResult <- EventStateSender.sendEventState(Set(task.taskId.shardId))
          stats <- runTx(TaskQueueDao.stats(TaskType.SendEventState))
        } yield {
          assert(stats.size)(equalTo(0)) &&
          assert(sendResult)(equalTo(Postpone))
        }

        effect.provideCustomLayer {
          baseLayer ++ broker ++ eventBuilder >+> DefaultEventStateSender.live
        }
      }
    ) @@ sequential
  }
}
