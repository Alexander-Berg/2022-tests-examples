package ru.yandex.vertis.general.feed.logic.test

import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.SellerId.UserId
import ru.yandex.vertis.general.feed.logic.FeedEventProducer.{feedExportConfigReader, FeedExportConfig}
import ru.yandex.vertis.general.feed.logic.{
  FeedEventProducer,
  FeedLockManager,
  FeedManager,
  TaskQueueManager,
  TaskStore
}
import ru.yandex.vertis.general.feed.model.TaskStatus.InProgress
import ru.yandex.vertis.general.feed.model.TaskType.Load
import ru.yandex.vertis.general.feed.model.{FeedHash, FeedSource, FeedTask, LockContext, NamespaceId, OneShotFeed}
import ru.yandex.vertis.general.feed.storage.postgresql._

import zio.blocking.Blocking
import zio.{duration, Has, Promise, UIO, ZIO, ZLayer}
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import ru.yandex.vertis.general.feed.logic.test.FeedTestInitUtils._
import java.util.concurrent.TimeUnit
import zio.{Promise, ZLayer}

import scala.concurrent.duration._

object TaskQueueManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("TaskQueueManager")(
      testM("Отдает незаблокированные задачи. Для каждого пользователя только самые ранние") {
        for {
          _ <- FeedManager.updateFeed(UserId(1), NamespaceId("test_1"), OneShotFeed("ya1.ru"))
          _ <- FeedManager.updateFeed(UserId(1), NamespaceId("test_2"), OneShotFeed("ya2.ru"))
          _ <- FeedManager.updateFeed(UserId(1), NamespaceId("test_1"), OneShotFeed("ya3.ru"))
          _ <- FeedManager.updateFeed(UserId(1), NamespaceId("test_2"), OneShotFeed("ya4.ru"))
          _ <- FeedManager.updateFeed(UserId(1), NamespaceId("test_1"), OneShotFeed("ya5.ru"))
          _ <- FeedManager.updateFeed(UserId(2), NamespaceId("test_2"), OneShotFeed("ya6.ru"))
          _ <- FeedManager.updateFeed(UserId(3), NamespaceId("test_3"), OneShotFeed("ya7.ru"))
          batchPromise <- Promise.make[Nothing, Seq[FeedTask]]
          _ <- FeedLockManager.withinLock(LockContext.Processor, UserId(3), NamespaceId("test_3")) {
            TaskQueueManager
              .pullUnlocked(10)
              .map(_.map(_.copy(createdAt = None)))
              .flatMap(batchPromise.succeed)
          }
          batchWithoutCreatedAt <- batchPromise.await
        } yield assert(batchWithoutCreatedAt)(
          equalTo(
            Seq(
              FeedTask(
                UserId(1),
                NamespaceId("test_1"),
                FeedSource.Feed,
                0,
                0,
                Load,
                InProgress,
                None,
                None,
                Nil,
                Some("ya1.ru"),
                None,
                FeedHash.Empty
              ),
              FeedTask(
                UserId(1),
                NamespaceId("test_2"),
                FeedSource.Feed,
                0,
                0,
                Load,
                InProgress,
                None,
                None,
                Nil,
                Some("ya2.ru"),
                None,
                FeedHash.Empty
              ),
              FeedTask(
                UserId(2),
                NamespaceId("test_2"),
                FeedSource.Feed,
                0,
                0,
                Load,
                InProgress,
                None,
                None,
                Nil,
                Some("ya6.ru"),
                None,
                FeedHash.Empty
              )
            )
          )
        )
      } @@ ignore
    ) @@ sequential).provideCustomLayerShared {
      val transactor = TestPostgresql.managedTransactor
      val clock = Clock.live
      val config = ZLayer.succeed(FeedLockManager.Config(10.minutes))
      val feedLockManager = Random.live ++ (clock ++ (transactor >+> dao) ++ config) >>> FeedLockManager.live
      Random.live ++ transactor ++ feedManager ++ taskQueueManager ++ feedLockManager
    }
  }
}
