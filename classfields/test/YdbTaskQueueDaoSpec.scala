package ru.yandex.vertis.billing.emon.storage.ydb

import cats.data.NonEmptyList
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.emon.model.Task
import ru.yandex.vertis.billing.emon.model.Task.{TaskId, TaskType}
import ru.yandex.vertis.billing.emon.storage.TaskQueueDao
import ru.yandex.vertis.billing.emon.storage.TaskQueueDao.TaskQueueDao
import ru.yandex.vertis.ydb.zio.TxURIO
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object YdbTaskQueueDaoSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("YdbTaskQueueDao")(
      testM("peek added tasks") {
        val task1 = Task(TaskId(1, TaskType.GetEventPrice, Instant.ofEpochMilli(1), "1"), "payload1".getBytes)
        val task2 = Task(TaskId(1, TaskType.GetEventPrice, Instant.ofEpochMilli(2), "2"), "payload2".getBytes)
        val task3 = Task(TaskId(1, TaskType.SendEventState, Instant.ofEpochMilli(1), "3"), "payload3".getBytes)
        val task4 = Task(TaskId(1, TaskType.SendEventState, Instant.ofEpochMilli(2), "4"), "payload4".getBytes)
        val task5 = Task(TaskId(2, TaskType.SendEventState, Instant.ofEpochMilli(1), "5"), "payload5".getBytes)
        val task6 = Task(TaskId(2, TaskType.SendEventState, Instant.ofEpochMilli(1), "6"), "payload6".getBytes)

        for {
          _ <- runTx(TaskQueueDao.add(NonEmptyList.of(task1, task2, task3, task4, task5, task6)))
          priceTasksShard1 <- runTx(TaskQueueDao.peek(shardId = 1, TaskType.GetEventPrice, 10))
          priceTasksShard2 <- runTx(TaskQueueDao.peek(shardId = 2, TaskType.GetEventPrice, 10))
          sendTasksShard2 <- runTx(TaskQueueDao.peek(shardId = 2, TaskType.SendEventState, 10))
        } yield {
          assert(priceTasksShard1)(equalTo(Seq(task1, task2))) &&
          assert(priceTasksShard2)(equalTo(Seq.empty)) &&
          assert(sendTasksShard2)(equalTo(Seq(task5, task6)))
        }
      },
      testM("pop tasks sorted by process time") {
        val task1 = Task(TaskId(1, TaskType.GetEventPrice, Instant.ofEpochMilli(1), "1"), s"payload1".getBytes)
        val task2 = Task(TaskId(1, TaskType.GetEventPrice, Instant.ofEpochMilli(2), "2"), s"payload2".getBytes)
        val task3 = Task(TaskId(1, TaskType.GetEventPrice, Instant.ofEpochMilli(3), "3"), s"payload3".getBytes)

        def pull(shardId: Int, taskType: TaskType, limit: Int): TxURIO[TaskQueueDao, Seq[Task]] =
          for {
            tasks <- TaskQueueDao.peek(shardId, taskType, limit)
            _ <- NonEmptyList
              .fromList(tasks.map(_.taskId).toList)
              .map(ts => TaskQueueDao.remove(ts))
              .getOrElse(ZIO.unit)
          } yield tasks

        for {
          _ <- runTx(TaskQueueDao.add(NonEmptyList.of(task1, task2, task3)))
          t1 <- runTx(pull(shardId = 1, TaskType.GetEventPrice, 1))
          t2 <- runTx(pull(shardId = 1, TaskType.GetEventPrice, 1))
          t3 <- runTx(pull(shardId = 1, TaskType.GetEventPrice, 1))
          t4 <- runTx(pull(shardId = 1, TaskType.GetEventPrice, 1))
        } yield {
          assert(t1)(equalTo(Seq(task1))) &&
          assert(t2)(equalTo(Seq(task2))) &&
          assert(t3)(equalTo(Seq(task3))) &&
          assert(t4)(equalTo(Seq.empty))
        }
      },
      testM("provide valid stats") {
        val task1 = Task(TaskId(TaskType.GetEventPrice, Instant.ofEpochMilli(10), "1"), s"payload1".getBytes)
        val task2 = Task(TaskId(TaskType.GetEventPrice, Instant.ofEpochMilli(20), "2"), s"payload2".getBytes)
        val task3 = Task(TaskId(TaskType.GetEventPrice, Instant.ofEpochMilli(30), "3"), s"payload3".getBytes)

        val task4 = Task(TaskId(TaskType.SendEventState, Instant.ofEpochMilli(40), "1"), s"payload1".getBytes)
        val task5 = Task(TaskId(TaskType.SendEventState, Instant.ofEpochMilli(50), "2"), s"payload2".getBytes)

        for {
          _ <- runTx(TaskQueueDao.add(NonEmptyList.of(task1, task2, task3, task4, task5)))
          stats1 <- runTx(TaskQueueDao.stats(TaskType.GetEventPrice))
          stats2 <- runTx(TaskQueueDao.stats(TaskType.SendEventState))
        } yield {
          assert(stats1)(equalTo(TaskQueueDao.QueueStats(3, Some(task1.taskId.processTime)))) &&
          assert(stats2)(equalTo(TaskQueueDao.QueueStats(2, Some(task4.taskId.processTime))))
        }
      }
    ) @@ sequential @@ before(TestYdb.clean(YdbTaskQueueDao.TableName))
  }.provideCustomLayerShared {
    TestYdb.ydb >+> YdbTaskQueueDao.live ++ Ydb.txRunner
  }
}
