package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.FeedTask
import ru.yandex.vertis.general.feed.model.testkit.NamespaceIdGen
import ru.yandex.vertis.general.feed.storage.TaskCleanerQueueDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgTaskCleanerQueueDao
import zio._
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._

object PgTaskCleanerQueueDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTaskCleanerQueueDao")(
      testM("Добавляет и возвращает таски на удаление") {
        checkNM(1)(
          SellerGen.anySellerId,
          SellerGen.anySellerId,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId1, sellerId2, namespaceId1, namespaceId2) =>
          for {
            dao <- ZIO.service[TaskCleanerQueueDao.Service]
            _ <- dao.createOrUpdate(sellerId1, namespaceId1, 0).transactIO
            _ <- dao.createOrUpdate(sellerId1, namespaceId1, 1).transactIO
            _ <- dao.createOrUpdate(sellerId2, namespaceId2, 2).transactIO
            results <- dao.pullUnlocked(2).transactIO
          } yield assert(results)(
            hasSameElements(Chunk(FeedTask.Key(sellerId1, namespaceId1, 1), FeedTask.Key(sellerId2, namespaceId2, 2)))
          )
        }
      },
      testM("Удаляет таски на удаление из очереди") {
        checkNM(1)(
          SellerGen.anySellerId,
          SellerGen.anySellerId,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId1, sellerId2, namespaceId1, namespaceId2) =>
          for {
            dao <- ZIO.service[TaskCleanerQueueDao.Service]
            _ <- dao.createOrUpdate(sellerId1, namespaceId1, 1).transactIO
            _ <- dao.createOrUpdate(sellerId2, namespaceId2, 2).transactIO
            _ <- dao.delete(sellerId1, namespaceId1, 1).transactIO
            results <- dao.pullUnlocked(2).transactIO
          } yield assert(results)(not(contains(FeedTask.Key(sellerId1, namespaceId1, 1))))
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgTaskCleanerQueueDao.live
      }
  }
}
