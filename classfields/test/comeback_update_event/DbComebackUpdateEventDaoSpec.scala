package ru.auto.comeback.storage.comeback_update_event

import common.zio.doobie.testkit.TestPostgresql
import doobie.syntax.connectionio._
import doobie.util.transactor.Transactor
import ru.auto.comeback.model.IntTokenDistribution
import ru.auto.comeback.model.testkit.ComebackUpdateEventGen
import ru.auto.comeback.storage.Schema
import zio.interop.catz._
import zio.test.Assertion.{equalTo, isNone, isSome}
import zio.test.TestAspect._
import zio.test.{assert, assertTrue, checkM, DefaultRunnableSpec, Gen}
import zio.{Task, ZIO}

object DbComebackUpdateEventDaoSpec extends DefaultRunnableSpec {

  private val singleInstanceDistribution = IntTokenDistribution(Set(0), 1)

  def spec = {
    suite("DbComebackUpdateEventDao")(
      testM("select from empty db") {
        for {
          _ <- Schema.cleanup
          xa <- ZIO.service[Transactor[Task]]
          dao <- ZIO.service[ComebackUpdateEventDao.Service]
          res <- dao.getFirst(singleInstanceDistribution).transact(xa)
        } yield assert(res)(isNone)
      },
      testM("select should return inserted event") {
        checkM(Gen.listOfBounded(2, 5)(ComebackUpdateEventGen.anyNewEvent)) { events =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackUpdateEventDao.Service]
            inserted <- dao.insert(events).transact(xa)
            res <- dao.getFirst(singleInstanceDistribution).transact(xa)
          } yield assertTrue(inserted.exists(_.id > 0)) && assertTrue(res.exists(_.id > 0)) && assertTrue(
            inserted.exists(ins => res.contains(ins))
          )
        }
      },
      testM("select should return nothing after delete") {
        checkM(ComebackUpdateEventGen.anyNewEvent) { event =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackUpdateEventDao.Service]
            inserted <- dao.insert(List(event)).transact(xa)
            expectedSome <- dao.getFirst(singleInstanceDistribution).transact(xa)
            _ <- dao.delete(inserted.head.id).transact(xa)
            expectedNone <- dao.getFirst(singleInstanceDistribution).transact(xa)
          } yield assert(expectedSome.map(_.copy(id = None)))(isSome(equalTo(event))) && assert(expectedNone)(isNone)
        }
      }
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential @@ samples(30) @@ shrinks(0)
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor(version = "12") >+> ComebackUpdateEventDao.live
  )
}
