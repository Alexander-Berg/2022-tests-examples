package ru.yandex.vertis.general.feed.logic.test

import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.logic._
import ru.yandex.vertis.general.feed.logic.test.FeedTestInitUtils._
import ru.yandex.vertis.general.feed.model._
import ru.yandex.vertis.general.feed.model.testkit.NamespaceIdGen
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object TaskManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("TaskManager")(
      testM("Возвращает последнюю версию скачивания") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId, namespaceId, namespaceId2) =>
          for {
            _ <- FeedManager.updateFeed(sellerId, namespaceId, OneShotFeed("ya.ru"))
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, OneShotFeed("best-host-ever.ru"))
            _ <- FeedManager.deleteFeed(sellerId, namespaceId)
            _ <- FeedManager.updateFeed(sellerId, namespaceId, OneShotFeed("ya.ru"))
            _ <- TaskManager.updateStatus(FeedTask.Key(sellerId, namespaceId, 1), TaskStatus.Succeed, Nil, None)
            lastTask1 <- TaskManager.get(sellerId, namespaceId, None)
            lastTask2 <- TaskManager.get(sellerId, namespaceId2, None)
          } yield assert(lastTask1)(
            isSome[FeedTask](
              hasField[FeedTask, Long]("taskId", _.taskId, equalTo(2L)) &&
                hasField("namespaceId", _.namespaceId, equalTo(namespaceId))
            )
          ) &&
            assert(lastTask2)(
              isSome[FeedTask](
                hasField[FeedTask, Long]("taskId", _.taskId, equalTo(0L)) &&
                  hasField[FeedTask, NamespaceId]("namespaceId", _.namespaceId, equalTo(namespaceId2)) &&
                  hasField("url", _.url, isSome(equalTo("best-host-ever.ru")))
              )
            )
        }
      },
      testM("Возвращает сквозной листинг со скачиваниями без удалений") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId, namespaceId, namespaceId2) =>
          for {
            _ <- FeedManager.updateFeed(sellerId, namespaceId, OneShotFeed("ya.ru"))
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, OneShotFeed("another-ya.ru"))
            _ <- TaskManager.updateStatus(FeedTask.Key(sellerId, namespaceId, 0), TaskStatus.Succeed, Nil, None)
            _ <- TaskManager.updateStatus(FeedTask.Key(sellerId, namespaceId2, 0), TaskStatus.Succeed, Nil, None)
            _ <- FeedManager.deleteFeed(sellerId, namespaceId)
            _ <- FeedManager.updateFeed(sellerId, namespaceId, OneShotFeed("ya.ru"))
            list1 <- TaskManager.list(sellerId, namespaceId, LimitOffset(10, 0))
            list2 <- TaskManager.list(sellerId, namespaceId2, LimitOffset(10, 0))
            listWithoutCreatedAt = (list1 ++ list2).map(_.copy(createdAt = None))
          } yield assert(listWithoutCreatedAt)(
            equalTo(
              Seq(
                FeedTask(
                  sellerId,
                  namespaceId,
                  FeedSource.Feed,
                  2,
                  2,
                  TaskType.Load,
                  TaskStatus.InProgress,
                  None,
                  None,
                  Nil,
                  Some("ya.ru"),
                  None,
                  FeedHash.Empty
                ),
                FeedTask(
                  sellerId,
                  namespaceId,
                  FeedSource.Feed,
                  0,
                  0,
                  TaskType.Load,
                  TaskStatus.Succeed,
                  None,
                  None,
                  Nil,
                  Some("ya.ru"),
                  None,
                  FeedHash.Empty
                ),
                FeedTask(
                  sellerId,
                  namespaceId2,
                  FeedSource.Feed,
                  0,
                  0,
                  TaskType.Load,
                  TaskStatus.Succeed,
                  None,
                  None,
                  Nil,
                  Some("another-ya.ru"),
                  None,
                  FeedHash.Empty
                )
              )
            )
          )
        }
      }
    ) @@ sequential).provideCustomLayerShared {
      val transactor = TestPostgresql.managedTransactor
      Random.live ++ transactor ++ feedManager ++ taskManager
    }
  } @@ ignore
}
