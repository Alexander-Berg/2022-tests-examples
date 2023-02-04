package ru.yandex.vertis.billing.emon.consumer

import billing.common_model.{Money, Project}
import billing.emon.internal.model.FactorsWithMeta
import billing.emon.model
import billing.emon.model.EventTypeNamespace.EventType
import billing.emon.model.{Event, EventId, EventPayload, EventPriceRequest, Factors, Payer, PriceInfo}
import billing.howmuch.price_service.{
  GetPricesRequest,
  GetPricesRequestEntry,
  GetPricesResponse,
  GetPricesResponseEntry
}
import cats.data.NonEmptyList
import common.scalapb.ScalaProtobuf.instantToTimestamp
import common.sraas.Sraas.SraasDescriptor
import common.sraas.TestSraas
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.emon.consumer.DefaultEventManager.HowmuchUnavailable
import ru.yandex.vertis.billing.emon.model.{FactorsSnapshot, Task}
import ru.yandex.vertis.billing.emon.storage.ydb.{YdbEventDao, YdbFactorsSnapshotDao, YdbTaskQueueDao}
import ru.yandex.vertis.billing.emon.storage.{EventDao, FactorsSnapshotDao, TaskQueueDao}
import zio.{Ref, ZIO}
import zio.clock.Clock
import zio.stream.ZSink
import zio.test.Assertion._
import zio.test.TestAspect.{before, _}
import zio.test._

import java.time.Instant

object DefaultEventManagerSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("DefaultEventManager")(
      testM("add new event to eventDao") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        for {
          _ <- EventManager.addEvent(event)
          eventId1Select <- runTx(EventDao.select(NonEmptyList.one(eventId)))
        } yield {
          assert(eventId1Select)(equalTo(Seq(event)))
        }
      },
      testM("do not add event with same event id to eventDao") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )
        val event2 = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(555, None)), 666L))),
          Some(EventPayload.defaultInstance)
        )

        for {
          _ <- EventManager.addEvent(event)
          _ <- EventManager.addEvent(event2)
          eventId1Select <- runTx(EventDao.select(NonEmptyList.one(eventId)))
        } yield {
          assert(eventId1Select)(equalTo(Seq(event)))
        }
      },
      testM("add default snapshot for event") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        for {
          _ <- EventManager.addEvent(event)
          factorsSelect <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
        } yield {
          assert(factorsSelect.map(_.snapshotId))(equalTo(Seq(0L))) &&
          assert(factorsSelect.map(_.eventId))(equalTo(Seq(eventId))) &&
          assert(factorsSelect.map(_.factors))(equalTo(Seq(Factors.defaultInstance)))
        }
      },
      testM("add task for last factors snapshot without event") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        val size = 1000
        val factors = (1 to size).map { i =>
          FactorsSnapshot(
            eventId,
            snapshotId = i,
            Factors.defaultInstance,
            FactorsWithMeta.defaultInstance,
            Instant.ofEpochMilli(i),
            None
          )
        }
        for {
          _ <- runTx(FactorsSnapshotDao.upsert(NonEmptyList.fromListUnsafe(factors.toList)))
          _ <- EventManager.addEvent(event)
          factorsSelect <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
          tasks <- ZIO
            .collectAll((0 until Task.ShardCount).map { shard =>
              runTx(TaskQueueDao.peek(shard, Task.TaskType.SendEventState, size))
            })
            .map(_.flatten)
        } yield {
          assert(factorsSelect.size)(equalTo(size + 1)) &&
          assert(factorsSelect.map(_.snapshotId).contains(0L))(isTrue) &&
          assert(tasks.map(_.taskId.id))(equalTo(Seq(s"${eventId.id}_$size")))
        }
      },
      testM("add factors to factorSnapshotDao without event") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val eventFactors = model
          .EventFactors()
          .withEventId(eventId)
          .withFactors(Factors.defaultInstance)
          .withTimestamp(instantToTimestamp(Instant.ofEpochMilli(1)))

        for {
          _ <- TestSraas.setJavaDescriptor(key =>
            zio.Task(SraasDescriptor(Factors.javaDescriptor, key.protoMessageName, key.version.toString))
          )
          _ <- EventManager.addFactors(eventFactors)
          factorsSelect <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
          tasks <- ZIO
            .collectAll((0 until Task.ShardCount).map { shard =>
              runTx(TaskQueueDao.peek(shard, Task.TaskType.SendEventState, 10))
            })
            .map(_.flatten)
        } yield {
          assert(factorsSelect.map(_.factors))(equalTo(Seq(eventFactors.getFactors))) &&
          assert(factorsSelect.map(_.snapshotId).contains(1L))(isTrue) &&
          assert(tasks.isEmpty)(isTrue)
        }
      },
      testM("ignore duplicate factors") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        val eventFactors = model
          .EventFactors()
          .withEventId(eventId)
          .withFactors(Factors.defaultInstance)
          .withTimestamp(instantToTimestamp(Instant.ofEpochMilli(1)))

        for {
          _ <- TestSraas.setJavaDescriptor(key =>
            zio.Task(SraasDescriptor(Factors.javaDescriptor, key.protoMessageName, key.version.toString))
          )
          _ <- EventManager.addEvent(event)
          _ <- EventManager.addFactors(eventFactors)
          factorsSelect <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
          tasks <- ZIO
            .collectAll((0 until Task.ShardCount).map { shard =>
              runTx(TaskQueueDao.peek(shard, Task.TaskType.SendEventState, 10))
            })
            .map(_.flatten)
        } yield {
          assert(factorsSelect.map(_.snapshotId).sorted)(equalTo(Seq(0L))) &&
          assert(tasks.map(_.taskId.id).sorted)(equalTo(Seq("1_0")))
        }
      },
      testM("add some new factors") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        val eventFactors1 = model
          .EventFactors()
          .withEventId(eventId)
          .withFactors(Factors().withHobo(ru.yandex.vertis.hobo.proto.model.Task(1).withComment("New task")))
          .withTimestamp(instantToTimestamp(Instant.ofEpochMilli(2)))

        val eventFactors2 = model
          .EventFactors()
          .withEventId(eventId)
          .withFactors(Factors().withHobo(ru.yandex.vertis.hobo.proto.model.Task(1).withComment("Ignore by ts")))
          .withTimestamp(instantToTimestamp(Instant.ofEpochMilli(1)))

        for {
          _ <- TestSraas.setJavaDescriptor(key =>
            zio.Task(SraasDescriptor(Factors.javaDescriptor, key.protoMessageName, key.version.toString))
          )
          _ <- EventManager.addEvent(event)
          _ <- EventManager.addFactors(eventFactors1)
          _ <- EventManager.addFactors(eventFactors2)
          factorsSelect <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
          tasks <- ZIO
            .collectAll((0 until Task.ShardCount).map { shard =>
              runTx(TaskQueueDao.peek(shard, Task.TaskType.SendEventState, 10))
            })
            .map(_.flatten)
        } yield {
          assert(factorsSelect.map(_.snapshotId).sorted)(equalTo(Seq(0L, 1L))) &&
          assert(factorsSelect.filter(_.snapshotId == 1L).map(_.factorsSnapshot.getFactors))(
            equalTo(Seq(eventFactors1.getFactors))
          ) &&
          assert(tasks.map(_.taskId.id).sorted)(equalTo(Seq("1_0", "1_1")))
        }
      },
      testM("add price for snapshot") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        val priceRequest = EventPriceRequest()
          .withEventId(eventId)
          .withSnapshotId(0)
          .withTimestamp(instantToTimestamp(Instant.ofEpochMilli(2)))
          .withPriceRequest(
            GetPricesRequest().withProject(eventId.project).withEntries(Seq(GetPricesRequestEntry.defaultInstance))
          )
        val priceEntry =
          GetPricesResponseEntry().withEntryId("12345").withRule(GetPricesResponseEntry.Rule("6789", Some(Money(42L))))
        val priceResponse = GetPricesResponse(Seq(priceEntry))

        for {
          _ <- EventManager.addEvent(event)
          _ <- TestPriceService.setPriceResponse(_ => ZIO.succeed(priceResponse))
          _ <- EventManager.addPrice(priceRequest)
          factorsSelect <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
          expectedResponse <- DefaultEventManager.buildPrice(priceResponse)
          expectedPrice = PriceInfo().withRequest(priceRequest.getPriceRequest).withResponse(expectedResponse)
        } yield {
          assert(factorsSelect.map(_.snapshotId))(equalTo(Seq(0L))) &&
          assert(factorsSelect.flatMap(_.price).headOption)(equalTo(Some(expectedPrice)))
        }
      },
      testM("ignore price request for snapshot with calculated price") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        val priceRequest = EventPriceRequest()
          .withEventId(eventId)
          .withSnapshotId(0)
          .withTimestamp(instantToTimestamp(Instant.ofEpochMilli(2)))
          .withPriceRequest(
            GetPricesRequest()
              .withProject(eventId.project)
              .withEntries(Seq(GetPricesRequestEntry.defaultInstance.withMatrixId("m1")))
          )
        val priceRequest2 = priceRequest.withPriceRequest(
          GetPricesRequest()
            .withProject(eventId.project)
            .withEntries(Seq(GetPricesRequestEntry.defaultInstance.withMatrixId("m2")))
        )

        val priceEntry =
          GetPricesResponseEntry().withEntryId("12345").withRule(GetPricesResponseEntry.Rule("6789", Some(Money(42L))))
        val priceResponse = GetPricesResponse(Seq(priceEntry))

        val priceEntry2 =
          GetPricesResponseEntry().withEntryId("123456").withRule(GetPricesResponseEntry.Rule("6789", Some(Money(43L))))
        val priceResponse2 = GetPricesResponse(Seq(priceEntry2))

        for {
          _ <- EventManager.addEvent(event)
          _ <- TestPriceService.setPriceResponse(r =>
            if (r == priceRequest.getPriceRequest) {
              ZIO.succeed(priceResponse)
            } else if (r == priceRequest2.getPriceRequest) {
              ZIO.succeed(priceResponse2)
            } else {
              ZIO.fail(new IllegalStateException("Unexpected price request"))
            }
          )
          _ <- EventManager.addPrice(priceRequest)
          _ <- EventManager.addPrice(priceRequest2)
          factorsSelect <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
          expectedResponse <- DefaultEventManager.buildPrice(priceResponse)
          expectedPrice = PriceInfo().withRequest(priceRequest.getPriceRequest).withResponse(expectedResponse)
        } yield {
          assert(factorsSelect.map(_.snapshotId))(equalTo(Seq(0L))) &&
          assert(factorsSelect.flatMap(_.price).headOption)(equalTo(Some(expectedPrice)))
        }
      },
      testM("unexpected howmuch error is retried, and then price is saved into db") {

        val eventId = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event = Event(
          Some(eventId),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )

        val priceRequest = EventPriceRequest()
          .withEventId(eventId)
          .withSnapshotId(0)
          .withTimestamp(instantToTimestamp(Instant.ofEpochMilli(2)))
          .withPriceRequest(
            GetPricesRequest()
              .withProject(eventId.project)
              .withEntries(Seq(GetPricesRequestEntry.defaultInstance.withMatrixId("m1")))
          )
        val priceEntry =
          GetPricesResponseEntry().withEntryId("123456").withRule(GetPricesResponseEntry.Rule("6789", Some(Money(43L))))
        val priceResponse = GetPricesResponse(Seq(priceEntry))
        for {
          expectedResponse <- DefaultEventManager.buildPrice(priceResponse)
          expectedPrice = PriceInfo().withRequest(priceRequest.getPriceRequest).withResponse(expectedResponse)
          _ <- setupTmpHowmuchUnavailabilityOnFirstRequest(priceResponse)

          _ <- EventManager.addEvent(event)
          // для простоты написания теста ретраим руками, а не честно дёргаем PriceConsumer,
          // который на самом деле ретраит addPrice
          _ <- EventManager
            .addPrice(priceRequest)
            .catchSome { case HowmuchUnavailable(_) => EventManager.addPrice(priceRequest) }
          factors <- runTx(FactorsSnapshotDao.selectAll(eventId).run(ZSink.collectAll).map(_.toList))
        } yield assertTrue(factors.flatMap(_.price) == List(expectedPrice))
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
    val priceService = TestPriceService.layer
    val sraas = TestSraas.layer
    val eventManager =
      (txRunner ++ priceService ++ sraas ++ eventDao ++ factorsDao ++ taskDao ++ Clock.live) >>> EventManager.live
    ydb ++ eventManager ++ priceService ++ sraas ++ eventDao ++ txRunner ++ factorsDao ++ taskDao ++ Clock.live
  }

  private def setupTmpHowmuchUnavailabilityOnFirstRequest(pricesResponse: GetPricesResponse) =
    zio.Ref.make(false).flatMap { howmuchAlreadyCalled =>
      TestPriceService.setPriceResponse(_ =>
        howmuchAlreadyCalled.get.flatMap {
          case false =>
            howmuchAlreadyCalled.set(true) *> ZIO.fail(HowmuchUnavailable("error happened"))
          case true =>
            ZIO.succeed(pricesResponse)
        }
      )
    }
}
