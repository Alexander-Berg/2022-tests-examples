package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.errors.FatalFeedErrors
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.FeedHash.OfferHash
import ru.yandex.vertis.general.feed.model.testkit.{FeedTaskGen, NamespaceIdGen}
import ru.yandex.vertis.general.feed.model.{FatalErrorInfo, FeedHash, FeedSource, FeedTask, TaskStatus, TaskType}
import ru.yandex.vertis.general.feed.storage.TaskDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgTaskDao
import zio.{clock, ZIO}
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object PgTaskDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTaskDao")(
      testM("insert task") {
        checkNM(1)(FeedTaskGen.any.noShrink) { task =>
          for {
            dao <- ZIO.service[TaskDao.Service]
            _ <- dao.createOrUpdate(task).transactIO
            saved <- dao.get(task.sellerId, task.namespaceId, task.taskId).transactIO
          } yield assert(saved)(isSome(equalTo(task)))
        }
      },
      testM("return none if task not found") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[TaskDao.Service]
              saved <- dao.get(sellerId, namespaceId, 0).transactIO
            } yield assert(saved)(isNone)
        }
      },
      testM("return last tasks") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[TaskDao.Service]
              task = FeedTask(
                sellerId,
                namespaceId,
                FeedSource.Feed,
                0,
                0,
                TaskType.Load,
                TaskStatus.InProgress,
                Some(Instant.now()),
                None,
                Nil,
                Some("123"),
                None,
                FeedHash.Empty
              )
              _ <- dao.createOrUpdate(task).transactIO
              _ <- dao.createOrUpdate(task.copy(taskId = 1, taskStatus = TaskStatus.Succeed)).transactIO
              _ <- dao.createOrUpdate(task.copy(taskId = 2, taskStatus = TaskStatus.InProgress)).transactIO
              _ <- dao
                .createOrUpdate(task.copy(feedVersion = 1, taskId = 3, taskStatus = TaskStatus.InProgress))
                .transactIO
              lastFinished <- dao.getLastFinished(sellerId, namespaceId, 0).transactIO
              last <- dao.getLast(sellerId, namespaceId, 0).transactIO
            } yield assert(lastFinished)(isSome(equalTo(1L))) && assert(last)(isSome(equalTo(2L)))
        }
      },
      testM("return none if last finished task not found") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[TaskDao.Service]
              task = FeedTask(
                sellerId,
                namespaceId,
                FeedSource.Feed,
                0,
                0,
                TaskType.Load,
                TaskStatus.InProgress,
                Some(Instant.now()),
                None,
                Nil,
                Some("123"),
                None,
                FeedHash.Empty
              )
              _ <- dao.createOrUpdate(task).transactIO
              _ <- dao
                .createOrUpdate(task.copy(feedVersion = 1, taskId = 1, taskStatus = TaskStatus.Succeed))
                .transactIO
              lastFinished <- dao.getLastFinished(sellerId, namespaceId, 0).transactIO
              last <- dao.getLast(sellerId, namespaceId, 0).transactIO
            } yield assert(lastFinished)(isNone) && assert(last)(isSome(equalTo(0L)))
        }
      },
      testM("return none if last task not found") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[TaskDao.Service]
              lastFinished <- dao.getLastFinished(sellerId, namespaceId, 0).transactIO
              last <- dao.getLast(sellerId, namespaceId, 0).transactIO
            } yield assert(lastFinished)(isNone) && assert(last)(isNone)
        }
      },
      testM("list & count tasks") {
        checkNM(1)(FeedTaskGen.any.noShrink) { task =>
          for {
            dao <- ZIO.service[TaskDao.Service]
            tasks = (0 until 20).map(i =>
              task.copy(taskId = i, taskType = if (i % 2 == 0) TaskType.Load else TaskType.Delete)
            )
            _ <- ZIO.foreach_(tasks)(dao.createOrUpdate(_).transactIO)
            list <- dao.list(task.sellerId, task.namespaceId, LimitOffset(3, 3)).transactIO
            count <- dao.count(task.sellerId, task.namespaceId).transactIO
            countStatus <- dao.count(TaskStatus.InProgress).transactIO
          } yield assert(list.map(_.taskId))(equalTo(Seq(12L, 10L, 8L))) && assert(count)(equalTo(10)) && assert(
            countStatus
          )(isGreaterThanEqualTo(tasks.count(_.taskStatus == TaskStatus.InProgress)))
        }
      },
      testM("update status") {
        checkNM(1)(FeedTaskGen.any.noShrink) { task =>
          val nonTerminal = task.copy(taskStatus = TaskStatus.InProgress, taskId = 22)
          val terminal = task.copy(taskStatus = TaskStatus.Succeed, taskId = 33)
          for {
            dao <- ZIO.service[TaskDao.Service]

            _ <- dao.createOrUpdate(nonTerminal).transactIO
            _ <- dao.createOrUpdate(terminal).transactIO

            time1 <- clock.instant
            _ <- dao
              .updateStatus(
                nonTerminal.key,
                TaskStatus.Failed,
                List(FatalErrorInfo("all screwed up", "", FatalFeedErrors.emptyDescriptionCode, None)),
                finishedAt = Some(time1)
              )
              .transactIO
            updated1 <- dao.get(nonTerminal.key).transactIO

            time2 <- clock.instant
            _ <- dao.updateStatus(terminal.key, TaskStatus.Succeed, finishedAt = Some(time2)).transactIO
            update2 <- dao.get(terminal.key).transactIO

            update3 <- dao
              .updateStatus(
                terminal.key,
                TaskStatus.Failed,
                List(FatalErrorInfo("ЕГГОГ", "", FatalFeedErrors.emptyDescriptionCode, None))
              )
              .transactIO
              .run
          } yield {
            assert(updated1.map(t => (t.taskStatus, t.fatalErrorsInfo.map(_.message), t.finishedAt)))(
              isSome(equalTo((TaskStatus.Failed, List("all screwed up"), Some(time1))))
            ) &&
            assert(update2)(isSome(equalTo(terminal))) &&
            assert(update3.untraced.mapError(_.cause))(fails(hasMessage(equalTo("Task already terminated"))))
          }
        }
      },
      testM("get tasks by keys") {
        checkNM(1)(FeedTaskGen.list.noShrink) { tasks =>
          for {
            dao <- ZIO.service[TaskDao.Service]
            _ <- ZIO.foreach_(tasks)(dao.createOrUpdate(_).transactIO)
            resp <- dao.getBatch(tasks.map(_.key)).transactIO
          } yield assert(resp)(hasSameElements(tasks))
        }
      },
      testM("set task semantic hash") {
        checkNM(1)(FeedTaskGen.any.noShrink) { task =>
          for {
            dao <- ZIO.service[TaskDao.Service]
            _ <- dao.createOrUpdate(task.copy(semanticHash = FeedHash.Empty)).transactIO
            _ <- dao.setSemanticHash(task.key, OfferHash(10)).transactIO
            resp <- dao.get(task.key).transactIO
          } yield assert(resp)(
            isSome(hasField("semanticHash", _.semanticHash, equalTo[OfferHash, FeedHash](OfferHash(10))))
          )
        }
      },
      testM("do not update semantic hash if set") {
        checkNM(1)(FeedTaskGen.any.noShrink) { task =>
          for {
            dao <- ZIO.service[TaskDao.Service]
            _ <- dao.createOrUpdate(task.copy(semanticHash = OfferHash(20))).transactIO
            _ <- dao.setSemanticHash(task.key, OfferHash(10)).transactIO
            resp <- dao.get(task.key).transactIO
          } yield assert(resp)(
            isSome(hasField("semanticHash", _.semanticHash, equalTo[OfferHash, FeedHash](OfferHash(20))))
          )
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        Random.live ++ TestPostgresql.managedTransactor >+> PgTaskDao.live
      }
  }
}
