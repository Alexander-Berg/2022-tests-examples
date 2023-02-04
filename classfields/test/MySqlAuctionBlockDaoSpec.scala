package vsmoney.auction.storage.test

import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestMySQL
import doobie.implicits._
import doobie.util.transactor.Transactor
import vsmoney.auction.model.{ProductId, Project, UserId}
import vsmoney.auction.storage.AuctionBlockDao.UserAuctionBlockIdentifier
import vsmoney.auction.storage.{MySqlAuctionBlockDao, MySqlAuctionBlockLockDao}
import zio.interop.catz._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.{Task, ZIO}

import java.time.Instant

object MySqlAuctionBlockDaoSpec extends DefaultRunnableSpec {

  val userId = UserId("dealer:123")

  val userIdentifier = UserAuctionBlockIdentifier(
    userId,
    ProductId("call:cars:used"),
    Project.Autoru
  )

  val start = Instant.parse("2022-04-07T12:04:57.600300Z")
  val end = start.plusSeconds(60)

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("MySqlAuctionBlockDao")(
      testM("should block user with lock and return user as blocked") {
        for {
          xa <- ZIO.service[Transactor[Task]]
          dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
          _ <- dao.blockUser(userIdentifier, start)
          usersWithBlock <- dao.getAllUsersWithActiveBlock(userIdentifier.project, userIdentifier.product)
        } yield assertTrue(usersWithBlock.contains(userId))
      },
      testM("should neither fail if user already blocked nor create double record") {
        for {
          xa <- ZIO.service[Transactor[Task]]
          dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
          _ <- dao.blockUser(userIdentifier, start)
          _ <- dao.blockUser(userIdentifier, start)
          usersWithBlockCount <-
            sql"""
              select count(*) from auction_block
               """.query[Int].unique.transact(xa)
        } yield assertTrue(usersWithBlockCount == 1)
      },
      testM("should block user then unblock") {
        for {
          xa <- ZIO.service[Transactor[Task]]
          dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
          _ <- dao.blockUser(userIdentifier, start)
          _ <- dao.unblockUser(userIdentifier, end)
          usersWithBlock <- dao.getAllUsersWithActiveBlock(userIdentifier.project, userIdentifier.product)
        } yield assertTrue(!usersWithBlock.contains(userId))
      },
      suite("userHadBlockAtMoment")(
        testM("should return true if user had block at given moment in past") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
            _ <- dao.blockUser(userIdentifier, start)
            _ <- dao.unblockUser(userIdentifier, end)
            userHadBlock <- dao.userHadBlockAtMoment(userIdentifier, start)
          } yield assertTrue(userHadBlock)
        },
        testM("should return false if user had block in past but has no active block now") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
            _ <- dao.blockUser(userIdentifier, start)
            _ <- dao.unblockUser(userIdentifier, end)
            userHadBlock <- dao.userHadBlockAtMoment(userIdentifier, end.plusSeconds(10))
          } yield assertTrue(!userHadBlock)
        },
        testM("should not fail and  return true if user had more than 1 active block at moment") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
            _ <- dao.blockUser(userIdentifier, start)
            _ <- dao.unblockUser(userIdentifier, end)
            _ <- dao.blockUser(userIdentifier, start)
            _ <- dao.unblockUser(userIdentifier, end)
            userHadBlock <- dao.userHadBlockAtMoment(userIdentifier, end.minusSeconds(1))
          } yield assertTrue(userHadBlock)
        },
        testM("should return false if user has no any blocks at given moment ") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
            userHadBlock <- dao.userHadBlockAtMoment(userIdentifier, start)
          } yield assertTrue(!userHadBlock)
        },
        testM("should return true if user has block at given moment") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao = new MySqlAuctionBlockDao(xa, new MySqlAuctionBlockLockDao())
            _ <- dao.blockUser(userIdentifier, start)
            userHasBlock <- dao.userHadBlockAtMoment(userIdentifier, start)
          } yield assertTrue(userHasBlock)
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
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            sql"TRUNCATE TABLE auction_block_lock".update.run.transact(xa) *>
              sql"TRUNCATE TABLE auction_block".update.run.transact(xa)
          }
      ) @@
      sequential).provideCustomLayerShared(TestMySQL.managedTransactor)
  }
}
