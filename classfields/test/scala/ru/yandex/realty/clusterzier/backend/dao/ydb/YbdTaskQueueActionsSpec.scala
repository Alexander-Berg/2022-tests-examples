package ru.yandex.realty.clusterzier.backend.dao.ydb

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.db.DBAction
import ru.yandex.realty.application.ng.ydb.YdbAction.YDBAction
import ru.yandex.realty.application.ng.ydb.YdbActionRunner
import ru.yandex.realty.clusterizer.backend.dao.TaskQueueActions
import ru.yandex.realty.clusterizer.backend.dao.TaskQueueActions._
import ru.yandex.realty.clusterizer.backend.dao.ydb.{YdbTaskQueueActions, YdbTaskQueueTable}
import ru.yandex.realty.clusterzier.backend.dao.TestYdb

import java.time.Instant

@RunWith(classOf[JUnitRunner])
class YbdTaskQueueActionsSpec extends AsyncSpecBase with TestYdb {

  override def beforeAll(): Unit = {
    YdbTaskQueueTable.initTable(ydbWrapper, ydbConfig.tablePrefix)
  }

  private lazy val actions = new YdbTaskQueueActions(ydbConfig.tablePrefix)

  before {
    clean(ydbConfig.tablePrefix, YdbTaskQueueTable.TableName).futureValue
  }

  "YbdTaskQueueActions" should {
    "peek added tasks" in {
      val task1 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(1), "1"), "payload1".getBytes)
      val task2 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(2), "2"), "payload2".getBytes)
      val task3 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(2), "3"), "payload3".getBytes)
      val task4 = Task(TaskId(2, TaskType.SendOfferUpdate, Instant.ofEpochMilli(1), "4"), "payload4".getBytes)
      val task5 = Task(TaskId(2, TaskType.SendOfferUpdate, Instant.ofEpochMilli(1), "5"), "payload5".getBytes)

      val a = for {
        _ <- actions.add(Seq(task1, task2, task3, task4, task5))
        tasksShard1 <- actions.peek(shardId = 1, TaskType.SendOfferUpdate, 10)
        tasksShard2 <- actions.peek(shardId = 2, TaskType.SendOfferUpdate, 10)
      } yield tasksShard1 -> tasksShard2
      val (tasksShard1, tasksShard2) = actionsRunner.run(a).futureValue
      tasksShard1 shouldBe Seq(task1, task2, task3)
      tasksShard2 shouldBe Seq(task4, task5)
    }

    "pop tasks sorted by process time" in {
      def pull(shardId: Int, taskType: TaskType, limit: Int): YDBAction[Seq[Task]] = {
        for {
          tasks <- actions.peek(shardId, taskType, limit)
          _ <- if (tasks.nonEmpty) {
            actions.remove(tasks.map(_.taskId))
          } else {
            DBAction.successful[YdbActionRunner.Context, Unit](Unit)
          }
        } yield tasks
      }

      val task1 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(1), "1"), s"payload1".getBytes)
      val task2 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(2), "2"), s"payload2".getBytes)
      val task3 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(3), "3"), s"payload3".getBytes)

      val a = for {
        _ <- actions.add(Seq(task1, task2, task3))
        t1 <- pull(shardId = 1, TaskType.SendOfferUpdate, 1)
        t2 <- pull(shardId = 1, TaskType.SendOfferUpdate, 1)
        t3 <- pull(shardId = 1, TaskType.SendOfferUpdate, 1)
        t4 <- pull(shardId = 1, TaskType.SendOfferUpdate, 1)
      } yield (t1, t2, t3, t4)

      val (t1, t2, t3, t4) = actionsRunner.run(a).futureValue
      t1 shouldBe Seq(task1)
      t2 shouldBe Seq(task2)
      t3 shouldBe Seq(task3)
      t4 shouldBe Seq.empty
    }

    "provide valid stats" in {
      val task1 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(10), "1"), s"payload1".getBytes)
      val task2 = Task(TaskId(1, TaskType.SendOfferUpdate, Instant.ofEpochMilli(20), "2"), s"payload2".getBytes)
      val task3 = Task(TaskId(2, TaskType.SendOfferUpdate, Instant.ofEpochMilli(30), "3"), s"payload3".getBytes)

      val a = for {
        _ <- actions.add(Seq(task1, task2, task3))
        stats <- actions.stats(TaskType.SendOfferUpdate)
      } yield stats

      val stats = actionsRunner.run(a).futureValue
      stats shouldBe TaskQueueActions.QueueStats(3, Some(task1.taskId.processTime))
    }

  }

}
