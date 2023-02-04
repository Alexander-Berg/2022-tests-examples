package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.testkit.TestPostgresql
import doobie.implicits._
import doobie.util.transactor.Transactor
import ru.yandex.vertis.general.common.model.user.SellerId.UserId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.testkit.NamespaceIdGen
import ru.yandex.vertis.general.feed.model.{FeedLoaderTask, FeedSource, LockContext, NamespaceId}
import ru.yandex.vertis.general.feed.storage.postgresql.{PgFeedLoaderTaskDao, PgFeedLockDao}
import ru.yandex.vertis.general.feed.storage.{FeedLoaderTaskDao, FeedLockDao}
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.{Task, ZIO}

import java.time.Instant
import java.util.UUID

object PgFeedLoaderTaskDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgFeedLoaderTaskDao")(
      testM("Ищет фид по продавцу") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId().noShrink
        ) { (sellerId1, sellerId2, namespaceId) =>
          val task1 = FeedLoaderTask(sellerId1, namespaceId, FeedSource.Feed, 123, "some url", None)
          val task2 = FeedLoaderTask(
            sellerId2,
            namespaceId,
            FeedSource.Feed,
            456,
            "another url",
            Some(Instant.ofEpochMilli(1234))
          )
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[FeedLoaderTaskDao.Service]
            _ <- dao.createOrUpdate(task1).transact(xa)
            _ <- dao.createOrUpdate(task2).transact(xa)
            saved1 <- dao.get(sellerId1, namespaceId).transact(xa)
            saved2 <- dao.get(sellerId2, namespaceId).transact(xa)
          } yield assert(saved1)(isSome(equalTo(task1))) && assert(saved2)(isSome(equalTo(task2)))
        }
      },
      testM("Ищет фид по namespace_id") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId1, namespaceId1, namespaceId2) =>
          val task1 = FeedLoaderTask(sellerId1, namespaceId1, FeedSource.Feed, 123, "some url", None)
          val task2 = FeedLoaderTask(
            sellerId1,
            namespaceId2,
            FeedSource.Feed,
            456,
            "another url",
            Some(Instant.ofEpochMilli(1234))
          )
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[FeedLoaderTaskDao.Service]
            _ <- dao.createOrUpdate(task1).transact(xa)
            _ <- dao.createOrUpdate(task2).transact(xa)
            saved1 <- dao.get(sellerId1, namespaceId1).transact(xa)
            saved2 <- dao.get(sellerId1, namespaceId2).transact(xa)
          } yield assert(saved1)(isSome(equalTo(task1))) && assert(saved2)(isSome(equalTo(task2)))
        }
      },
      testM("Ищет еще не взятые задачи для скачивания") {
        val namespaceId = NamespaceId("test_100500")
        val task1 = FeedLoaderTask(UserId(1), namespaceId, FeedSource.Feed, 1, "some url", None)
        val task2 =
          FeedLoaderTask(
            UserId(2),
            namespaceId,
            FeedSource.Feed,
            2,
            "another url",
            Some(Instant.ofEpochMilli(1234)),
            Some(Instant.ofEpochMilli(1234))
          )
        val task3 = FeedLoaderTask(UserId(3), namespaceId, FeedSource.Feed, 3, "another url (2)", None)
        val task4 = FeedLoaderTask(
          UserId(4),
          namespaceId,
          FeedSource.Feed,
          4,
          "another url (3)",
          Some(Instant.ofEpochMilli(4567)),
          Some(Instant.ofEpochMilli(4567))
        )
        val uuid = UUID.randomUUID()
        for {
          xa <- ZIO.service[Transactor[Task]]
          feedLoaderTaskDao <- ZIO.service[FeedLoaderTaskDao.Service]
          feedLockDao <- ZIO.service[FeedLockDao.Service]
          _ <- ZIO.foreach_(List(task1, task2, task3, task4))(task =>
            feedLoaderTaskDao.createOrUpdate(task).transact(xa)
          )
          _ <- feedLockDao
            .lock(LockContext.Loader, task1.sellerId, namespaceId, Instant.ofEpochMilli(123), uuid)
            .transact(xa)
          batch <- feedLoaderTaskDao.pullUnlocked(Instant.ofEpochMilli(5555), 2).transact(xa)
        } yield assert(batch)(equalTo(Seq(task3, task2)))
      },
      testM("Удаляет задачи на скачивание по продавцу") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            val task = FeedLoaderTask(sellerId, namespaceId, FeedSource.Feed, 123, "some url", None)
            for {
              xa <- ZIO.service[Transactor[Task]]
              dao <- ZIO.service[FeedLoaderTaskDao.Service]
              saved1 <- dao.get(sellerId, namespaceId).transact(xa)
              _ <- dao.createOrUpdate(task).transact(xa)
              saved2 <- dao.get(sellerId, namespaceId).transact(xa)
              _ <- dao.delete(sellerId, namespaceId).transact(xa)
              saved3 <- dao.get(sellerId, namespaceId).transact(xa)
            } yield assert(saved1)(isNone) && assert(saved2)(isSome(equalTo(task))) && assert(saved3)(isNone)
        }
      },
      testM("Удаляет задачи на скачивание по namespace_id") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId, namespaceId1, namespaceId2) =>
          val task = FeedLoaderTask(sellerId, namespaceId1, FeedSource.Feed, 123, "some url", None)
          val task2 = FeedLoaderTask(sellerId, namespaceId2, FeedSource.Feed, 234234, "some another url", None)
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[FeedLoaderTaskDao.Service]
            saved11 <- dao.get(sellerId, namespaceId1).transact(xa)
            saved21 <- dao.get(sellerId, namespaceId1).transact(xa)
            _ <- dao.createOrUpdate(task).transact(xa)
            _ <- dao.createOrUpdate(task2).transact(xa)
            saved12 <- dao.get(sellerId, namespaceId1).transact(xa)
            saved22 <- dao.get(sellerId, namespaceId2).transact(xa)
            _ <- dao.delete(sellerId, namespaceId1).transact(xa)
            saved13 <- dao.get(sellerId, namespaceId1).transact(xa)
            saved23 <- dao.get(sellerId, namespaceId2).transact(xa)
            _ <- dao.delete(sellerId, namespaceId2).transact(xa)
            saved24 <- dao.get(sellerId, namespaceId2).transact(xa)
          } yield {
            assert(saved11)(isNone) &&
            assert(saved12)(isSome(equalTo(task))) &&
            assert(saved13)(isNone) &&
            assert(saved21)(isNone) &&
            assert(saved22)(isSome(equalTo(task2))) &&
            assert(saved23)(isSome(equalTo(task2))) &&
            assert(saved24)(isNone)
          }
        }
      }
    ) @@ before(clean) @@ sequential)
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> (PgFeedLoaderTaskDao.live ++ PgFeedLockDao.live)
      }
  }

  private val clean = {
    ZIO.service[Transactor[Task]].flatMap(xa => PgFeedLoaderTaskDao.clean.transact(xa).orDie)
  }
}
