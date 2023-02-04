package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.FeedTask
import ru.yandex.vertis.general.feed.model.testkit.NamespaceIdGen
import ru.yandex.vertis.general.feed.storage.TaskQueueDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgTaskQueueDao
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.{clock, ZIO}

object PgTaskQueueDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTaskQueueDao")(
      testM("push & pull batches") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[TaskQueueDao.Service]
              now <- clock.instant
              taskKey1 = FeedTask.Key(sellerId, namespaceId, 0)
              taskKey2 = FeedTask.Key(sellerId, namespaceId, 1)
              _ <- dao.push(taskKey1, now).transactIO
              _ <- dao.push(taskKey2, now.plusSeconds(60)).transactIO
              results <- dao.pullUnlocked(2).transactIO
            } yield assert(results)(equalTo(Seq(taskKey1, taskKey2)))
        }
      },
      testM("delete batches") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[TaskQueueDao.Service]
              now <- clock.instant
              taskKey1 = FeedTask.Key(sellerId, namespaceId, 3)
              taskKey2 = FeedTask.Key(sellerId, namespaceId, 4)
              _ <- dao.push(taskKey1, now).transactIO
              _ <- dao.push(taskKey2, now.plusSeconds(60)).transactIO
              _ <- dao.delete(List(taskKey1, taskKey2)).transactIO
              results <- dao.pullUnlocked(2).transactIO
            } yield assert(results)(not(equalTo(Seq(taskKey1, taskKey2))))
        }
      },
      testM("check if task is enqueued") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[TaskQueueDao.Service]
              now <- clock.instant
              taskKey = FeedTask.Key(sellerId, namespaceId, 616)
              _ <- dao.push(taskKey, now).transactIO
              should <- dao.isEnqueued(taskKey).transactIO
              _ <- dao.delete(taskKey :: Nil).transactIO
              shouldNot <- dao.isEnqueued(taskKey).transactIO
            } yield assert(should)(isTrue) &&
              assert(shouldNot)(isFalse)
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgTaskQueueDao.live
      }
  }
}
