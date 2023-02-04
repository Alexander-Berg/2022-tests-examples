package ru.auto.comeback.storage.comeback_update_event

import common.zio.doobie.testkit.TestPostgresql
import ru.auto.comeback.model.IntTokenDistribution
import ru.auto.comeback.model.testkit.ComebackUpdateEventGen
import ru.auto.comeback.storage.Schema
import zio.ZIO
import zio.test.Assertion.{equalTo, isNone, isSome}
import zio.test.TestAspect.{after, beforeAll, samples, sequential, shrinks}
import zio.test._

object LiveComebackUpdateEventServiceSpec extends DefaultRunnableSpec {

  private val singleInstanceDistribution = IntTokenDistribution(Set(0), 1)

  def spec = {
    suite("LiveComebackUpdateEventService")(
      testM("select from empty db") {
        for {
          _ <- Schema.cleanup
          service <- ZIO.service[ComebackUpdateEventService.Service]
          res <- service.getFirst(singleInstanceDistribution)
        } yield assert(res)(isNone)
      },
      testM("select should return inserted event") {
        checkM(ComebackUpdateEventGen.anyNewEvent) { event =>
          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackUpdateEventService.Service]
            _ <- service.insert(List(event))
            res <- service.getFirst(singleInstanceDistribution)
          } yield assertTrue(res.exists(_.id > 0)) && assert(res.map(_.copy(id = None)))(isSome(equalTo(event)))
        }
      },
      testM("select should return nothing after delete") {
        checkM(ComebackUpdateEventGen.anyNewEvent) { event =>
          for {
            _ <- Schema.cleanup
            service <- ZIO.service[ComebackUpdateEventService.Service]
            inserted <- service.insert(List(event))
            expectedSome <- service.getFirst(singleInstanceDistribution)
            _ <- service.delete(inserted.head.id)
            expectedNone <- service.getFirst(singleInstanceDistribution)
          } yield assert(expectedSome.map(_.copy(id = None)))(isSome(equalTo(event))) && assert(expectedNone)(isNone)
        }
      }
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential @@ samples(30) @@ shrinks(0)
  }.provideCustomLayerShared {
    val transactor = TestPostgresql.managedTransactor(version = "12")
    (ComebackUpdateEventDao.live ++ transactor) >+> ComebackUpdateEventService.live
  }
}
