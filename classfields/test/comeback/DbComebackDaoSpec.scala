package ru.auto.comeback.storage.comeback

import common.zio.doobie.testkit.TestPostgresql
import doobie.syntax.connectionio._
import doobie.util.transactor.Transactor
import ru.auto.comeback.model.testkit.ComebackGen
import ru.auto.comeback.model.testkit.OfferGen.anyOfferRef
import ru.auto.comeback.storage.Schema
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{assert, assertTrue, checkM, DefaultRunnableSpec, Gen}
import zio.{Task, ZIO}

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 20/01/2020
  */
object DbComebackDaoSpec extends DefaultRunnableSpec {

  def spec = {
    suite("DbComebackDao")(
      testM("select by ref from empty db") {
        checkM(anyOfferRef) { ref =>
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            res <- dao.findCreatedBy(ref).transact(xa)
          } yield assert(res)(isEmpty)
        }
      },
      testM("select should return inserted comeback") {
        checkM(ComebackGen.anyNewComeback) { cb =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            inserted <- dao.insert(List(cb)).transact(xa)
            res <- dao.getByIds(inserted.map(_.id)).transact(xa)
          } yield assert(res)(equalTo(inserted))
        }
      },
      testM("update should do nothing if comeback not exists") {
        checkM(ComebackGen.anyComeback) { cb =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            _ <- dao.update(List(cb)).transact(xa)
            res <- dao.getByIds(List(cb.id)).transact(xa)
          } yield assert(res)(isEmpty)
        }
      },
      testM("batch insert comebacks should return generated ids") {
        checkM(Gen.listOf(ComebackGen.anyNewComeback)) { cbs =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            res <- dao.insert(cbs).transact(xa)
          } yield assert(res)(hasSize(equalTo(cbs.size))) &&
            assert(res.map(_.withoutId))(hasSameElements(cbs.map(_.withoutId)))
        }
      },
      testM("update inserted data") {
        checkM(ComebackGen.anyNewComeback, ComebackGen.anyNewComeback) { (cb1, cb2) =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            inserted <- dao.insert(List(cb1)).transact(xa)
            toUpdate = cb2.copy(id = inserted.head.id)
            _ <- dao.update(List(toUpdate)).transact(xa)
            selected <- dao.getByIds(inserted.map(_.id)).transact(xa)
          } yield assertTrue(selected == List(toUpdate))
        }
      },
      testM("select comebacks by offerRef") {
        checkM(Gen.listOf(ComebackGen.anyNewComeback)) { cbs =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            inserted <- dao.insert(cbs).transact(xa)
            refs = inserted.map(_.offer.ref).distinct
            res <- ZIO.foreach(refs)(ref => dao.findCreatedBy(ref).transact(xa).map(ref -> _))
          } yield assert(res.toMap)(equalTo(inserted.groupBy(_.offer.ref)))
        }
      }
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential @@ samples(30) @@ shrinks(0)
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor(version = "12") >+> ComebackDao.live
  )
}
