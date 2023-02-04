package vsmoney.auction.storage.test

import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestMySQL
import doobie.implicits._
import doobie.util.transactor.Transactor
import vsmoney.auction.model.{ProductId, Project, UserId}
import vsmoney.auction.storage.MySqlAuctionBlockLockDao
import zio.clock.Clock
import zio.duration._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{Task, ZIO}

object MySqlAuctionBlockLockDaoSpec extends DefaultRunnableSpec {

  private case class TimeoutException() extends RuntimeException

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("MySqlAuctionBlockLockDao")(
      suite("executeWithLock")(
        testM("should create lock record if missing then lock it for update") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockLockDao
            result <- dao
              .executeWithLock(
                UserId("dealer:123"),
                ProductId("call:cars:used"),
                Project.Autoru
              )(sql"""select 1 from auction_block_lock
                where user_id='dealer:123' and product="call:cars:used" and project="Autoru"
                 """.query[Int].unique)
              .transact(xa)
          } yield assertTrue(result == 1)
        },
        testM("should hold existing lock record for update") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockLockDao
            _ <- dao
              .executeWithLock(
                UserId("dealer:123"),
                ProductId("call:cars:used"),
                Project.Autoru
              )(AsyncConnectionIO.unit)
              .transact(xa)
            result <- dao
              .executeWithLock(
                UserId("dealer:123"),
                ProductId("call:cars:used"),
                Project.Autoru
              )(sql"""select count(*) from auction_block_lock
                where user_id='dealer:123' and product="call:cars:used" and project="Autoru"
                 """.query[Int].unique)
              .transact(xa)
          } yield assertTrue(result == 1)
        },
        testM("ensure that raw is locked") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockLockDao
            firstLockHoldAsync <- dao
              .executeWithLock(
                UserId("dealer:123"),
                ProductId("call:cars:used"),
                Project.Autoru
              )(sql"""select SLEEP(5)""".query[Int].unique)
              .transact(xa)
              .fork
            _ <- ZIO.sleep(500.millis)
            secondLockHold <- dao
              .executeWithLock(
                UserId("dealer:123"),
                ProductId("call:cars:used"),
                Project.Autoru
              )(AsyncConnectionIO.unit)
              .transact(xa)
              .timeoutFail(TimeoutException())(500.millis)
              .run
            firstLockHold <- firstLockHoldAsync.join
          } yield assert(secondLockHold)(fails(isSubtype[TimeoutException](anything))) &&
            assertTrue(firstLockHold == 0) // sleep was not interrupted

        }
      )
    ) @@
      beforeAll(
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            for {
              _ <- InitSchema("/schema.sql", xa)
            } yield ()
          }
      ) @@
      after(
        ZIO.service[Transactor[Task]].flatMap(sql"TRUNCATE TABLE auction_block_lock".update.run.transact(_).unit)
      ) @@
      sequential).provideCustomLayerShared(TestMySQL.managedTransactor).provideCustomLayer(Clock.live)
  }
}
