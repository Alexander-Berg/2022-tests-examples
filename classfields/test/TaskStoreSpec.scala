package ru.yandex.vertis.general.feed.logic.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import doobie.free.connection.pure
import doobie.util.transactor.Transactor
import ru.yandex.vertis.general.common.errors.FatalFeedErrors
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.feed.logic.TaskStore
import ru.yandex.vertis.general.feed.model.{
  FatalErrorInfo,
  FeedSource,
  FeedTask,
  NamespaceId,
  NoTasksFound,
  TaskAlreadyRunning,
  TaskStatus
}
import ru.yandex.vertis.general.feed.storage.postgresql.{PgTaskCleanerQueueDao, PgTaskDao, PgTaskQueueDao}
import ru.yandex.vertis.general.feed.storage.{TaskDao, TaskQueueDao}
import zio.clock.{instant, Clock}
import zio.test.Assertion._
import zio.test._
import zio.{Task, ZIO}

object TaskStoreSpec extends DefaultRunnableSpec {

  private def getLastTask(seller: SellerId, namespaceId: NamespaceId, xa: Transactor[Task]) = {
    for {
      taskDao <- ZIO.service[TaskDao.Service]
      last <- taskDao
        .getLast(seller, namespaceId)
        .flatMap { lastId =>
          lastId.map(taskDao.get(seller, namespaceId, _)).getOrElse(pure(Option.empty[FeedTask]))
        }
        .transactIO(xa)
    } yield last
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("TaskStore")(
      testM("Рефрешить заваленные таски") {
        for {
          store <- ZIO.service[TaskStore.Service]
          xa <- ZIO.service[Transactor[Task]]
          queueDao <- ZIO.service[TaskQueueDao.Service]
          create <- instant
          sellerId = SellerId.UserId(6686L)
          namespaceId = NamespaceId("test_1")
          namespaceId2 = NamespaceId("test_2")
          _ <- store
            .createFailedLoadTask(
              sellerId,
              namespaceId,
              FeedSource.Feed,
              0,
              FatalErrorInfo("", "", FatalFeedErrors.emptyDescriptionCode, None) :: Nil,
              create
            )
            .transactIO(xa)
          _ <- store
            .createFailedLoadTask(
              sellerId,
              namespaceId2,
              FeedSource.Feed,
              1523,
              FatalErrorInfo("fail", "fail description", FatalFeedErrors.emptyDescriptionCode, None) :: Nil,
              create
            )
            .transactIO(xa)
          failed <- getLastTask(sellerId, namespaceId, xa)
          failed2 <- getLastTask(sellerId, namespaceId2, xa)
          refreshTime <- instant
          _ <- store.refreshTask(sellerId, namespaceId, refreshTime).transactIO(xa)
          refreshed <- getLastTask(sellerId, namespaceId, xa)
          notYetRefreshed2 <- getLastTask(sellerId, namespaceId2, xa)
          _ <- store.refreshTask(sellerId, namespaceId2, refreshTime).transactIO(xa)
          refreshed2 <- getLastTask(sellerId, namespaceId2, xa)
          enqueued <- refreshed.map(_.key).map(queueDao.isEnqueued).getOrElse(pure(false)).transactIO(xa)
          enqueued2 <- refreshed2.map(_.key).map(queueDao.isEnqueued).getOrElse(pure(false)).transactIO(xa)
        } yield {
          assert(failed.map(_.taskStatus))(isSome(equalTo(TaskStatus.Failed))) &&
          assert(refreshed.map(_.taskStatus))(isSome(equalTo(TaskStatus.InProgress))) &&
          assertTrue(enqueued) &&
          assert(failed2.map(_.taskStatus))(isSome(equalTo(TaskStatus.Failed))) &&
          assert(notYetRefreshed2.map(_.taskStatus))(isSome(equalTo(TaskStatus.Failed))) &&
          assert(refreshed2.map(_.taskStatus))(isSome(equalTo(TaskStatus.InProgress))) &&
          assertTrue(enqueued2)
        }
      },
      testM("Падать если таска нет") {
        for {
          store <- ZIO.service[TaskStore.Service]
          xa <- ZIO.service[Transactor[Task]]
          refreshTime <- instant
          failure <- store
            .refreshTask(SellerId.UserId(774L), NamespaceId("test_12581"), refreshTime)
            .transactIO(xa)
        } yield assert(failure)(isLeft(isSubtype[NoTasksFound](anything)))
      },
      testM("Падать если таск запущен") {
        for {
          store <- ZIO.service[TaskStore.Service]
          xa <- ZIO.service[Transactor[Task]]
          createTime <- instant
          sellerId = SellerId.UserId(6687L)
          namespaceId = NamespaceId("test_19571451")
          _ <- store
            .createLoadTask(
              sellerId,
              namespaceId,
              FeedSource.Feed,
              1,
              "http://somefeeds.ru/randomfeed/123",
              Some("md5"),
              createTime
            )
            .transactIO(xa)
          refreshTime <- instant
          failure <- store.refreshTask(sellerId, namespaceId, refreshTime).transactIO(xa)
        } yield assert(failure)(isLeft(isSubtype[TaskAlreadyRunning](anything)))
      }
    )
  }.provideCustomLayerShared {
    val transactor = TestPostgresql.managedTransactor

    val taskDao = transactor >>> PgTaskDao.live
    val queueDao = transactor >>> PgTaskQueueDao.live
    val cleanerDao = transactor >>> PgTaskCleanerQueueDao.live
    val dao = taskDao ++ queueDao ++ cleanerDao

    (dao >>> TaskStore.live) ++ transactor ++ Clock.live ++ taskDao ++ queueDao
  }

}
