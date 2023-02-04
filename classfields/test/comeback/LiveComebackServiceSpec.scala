package ru.auto.comeback.storage.comeback

import common.zio.doobie.testkit.TestPostgresql
import ru.auto.comeback.model.testkit.ComebackGen
import ru.auto.comeback.model.testkit.OfferGen.anyOfferRef
import ru.auto.comeback.model.{IntTokenDistribution, Status}
import ru.auto.comeback.storage.Schema
import ru.auto.comeback.storage.comeback_update_event.{ComebackUpdateEventDao, ComebackUpdateEventService}
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object LiveComebackServiceSpec extends DefaultRunnableSpec {

  private val singleInstanceDistribution = IntTokenDistribution(Set(0), 1)

  def spec = {
    suite("LiveComebackService")(
      testM("select by ref from empty db") {
        checkM(anyOfferRef) { ref =>
          for {
            service <- ZIO.service[ComebackService.Service]
            res <- service.findCreatedBy(ref)
          } yield assert(res)(isEmpty)
        }
      },
      testM("select should return inserted comeback") {
        checkM(ComebackGen.anyNewComeback) { cb =>
          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackService.Service]
            inserted <- service.insert(List(cb), scheduleUpdateEvent = true)
            res <- service.getByIds(inserted.map(_.id))
          } yield assert(res)(equalTo(inserted))
        }
      },
      testM("update should do nothing if comeback not exists") {
        checkM(ComebackGen.anyComeback) { cb =>
          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackService.Service]
            updateEventsService <- ZIO.service[ComebackUpdateEventService.Service]
            _ <- service.update(List(cb), scheduleUpdateEvent = true)
            res <- service.getByIds(List(cb.id))
            event <- updateEventsService.getFirst(singleInstanceDistribution)
          } yield assert(res)(isEmpty) && assert(event)(isNone)
        }
      },
      testM("insert comeback should also insert update event") {
        checkM(ComebackGen.anyNewComeback) { cb =>
          val activeCb = cb.copy(status = Status.Active)

          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackService.Service]
            updateEventsService <- ZIO.service[ComebackUpdateEventService.Service]
            res <- service.insert(List(activeCb), scheduleUpdateEvent = true)
            event <- updateEventsService.getFirst(singleInstanceDistribution)
          } yield assert(res.head)(equalTo(activeCb.withId(res.head.id))) &&
            assert(event.get.prevState)(isNone) &&
            assert(event.get.currentState)(equalTo(res.head))
        }
      },
      testM("update inserted data should also insert update event") {
        checkM(ComebackGen.anyNewComeback, ComebackGen.anyNewComeback) { (cb1, cb2) =>
          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackService.Service]
            updateEventsService <- ZIO.service[ComebackUpdateEventService.Service]
            inserted <- service.insert(List(cb1.copy(status = Status.Active)), scheduleUpdateEvent = true)
            eventByInsert <- updateEventsService.getFirst(singleInstanceDistribution)
            _ <- updateEventsService.delete(eventByInsert.get.id)
            toUpdate = cb2.copy(id = inserted.head.id, status = Status.Active)
            _ <- service.update(List(toUpdate), scheduleUpdateEvent = true)
            eventByUpdate <- updateEventsService.getFirst(singleInstanceDistribution)
            res <- service.getByIds(inserted.map(_.id))
          } yield assert(res.head)(equalTo(toUpdate)) &&
            assert(eventByInsert.get.prevState)(isNone) &&
            assert(eventByInsert.get.currentState)(equalTo(inserted.head)) &&
            assert(eventByUpdate.get.prevState)(isSome(equalTo(inserted.head))) &&
            assert(eventByUpdate.get.currentState)(equalTo(toUpdate))
        }
      },
      testM("update event should not be inserted if comeback was deleted") {
        checkM(ComebackGen.anyNewComeback) { cb =>
          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackService.Service]
            updateEventsService <- ZIO.service[ComebackUpdateEventService.Service]
            inserted <- service.insert(List(cb.copy(status = Status.Active)), scheduleUpdateEvent = true)
            eventByInsert <- updateEventsService.getFirst(singleInstanceDistribution)
            _ <- updateEventsService.delete(eventByInsert.get.id)
            toUpdate = inserted.head.copy(status = Status.Hidden)
            _ <- service.update(List(toUpdate), scheduleUpdateEvent = true)
            selected <- service.getByIds(inserted.map(_.id))
            eventByUpdate <- updateEventsService.getFirst(singleInstanceDistribution)
          } yield assert(selected.head)(equalTo(toUpdate)) &&
            assert(eventByInsert.get.prevState)(isNone) &&
            assert(eventByInsert.get.currentState)(equalTo(inserted.head)) &&
            assert(eventByUpdate)(isNone)
        }
      },
      testM("select comebacks by offerRef") {
        checkM(Gen.listOf(ComebackGen.anyNewComeback)) { cbs =>
          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackService.Service]
            inserted <- service.insert(cbs, scheduleUpdateEvent = true)
            refs = inserted.map(_.offer.ref).distinct
            res <- ZIO.foreach(refs)(ref => service.findCreatedBy(ref).map(ref -> _))
          } yield assert(res.toMap)(equalTo(inserted.groupBy(_.offer.ref)))
        }
      }
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential @@ samples(30) @@ shrinks(0)
  }.provideCustomLayerShared {
    val comebackDao = ComebackDao.live
    val comebackUpdateEventDao = ComebackUpdateEventDao.live
    val transactor = TestPostgresql.managedTransactor(version = "12")
    val comebackService = (comebackDao ++ comebackUpdateEventDao ++ transactor) >>> ComebackService.live
    val comebackUpdateEventService = (comebackUpdateEventDao ++ transactor) >>> ComebackUpdateEventService.live

    comebackService ++ comebackUpdateEventService ++ transactor
  }
}
