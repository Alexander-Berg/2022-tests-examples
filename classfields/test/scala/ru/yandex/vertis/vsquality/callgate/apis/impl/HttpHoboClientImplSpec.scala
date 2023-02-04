package ru.yandex.vertis.vsquality.callgate.apis.impl

import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import org.scalatest.Ignore
import ru.yandex.vertis.vsquality.callgate.apis.DefaultApis
import ru.yandex.vertis.vsquality.callgate.model.TaskDescriptor
import ru.yandex.vertis.vsquality.callgate.model.TaskDescriptor._
import ru.yandex.vertis.hobo.proto.model.{QueueId, Task}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

/**
  * @author mpoplavkov
  */
@Ignore
class HttpHoboClientImplSpec extends SpecBase with DefaultApis {

  private val hoboClient = getHoboClient[IO]
  private val queue = QueueId.TEST_QUEUE
  private val taskKey1 = "019b3139ece9c46a7cb290a35d48e34c"
  private val taskKey2 = "cfcced79cd669e215eb5deb871af1ee3"

  after {
    val io =
      for {
        tasks <- getAllInProgress
        _     <- tasks.toList.traverse(task => hoboClient.putBack(task.getDescriptor))
      } yield ()
    io.await
  }

  "HttpHoboClient" should {

    "get in progress tasks" in {
      hoboClient.takeTask(queue).await
      getAllInProgress.await should not be empty
    }

    "put task back" in {
      val tasks1 = getAllInProgress.await
      hoboClient.takeTask(queue).await
      val tasks2 = getAllInProgress.await
      tasks2.size should be > tasks1.size
      hoboClient.putBack(tasks2.head.getDescriptor).await
      val tasks3 = getAllInProgress.await
      tasks3.size shouldBe tasks1.size
    }

    "take task" in {
      val tasks1 = getAllInProgress.await
      hoboClient.takeTask(queue).await
      val tasks2 = getAllInProgress.await
      tasks2.size should be > tasks1.size
    }

    "cancel task" in {
      hoboClient.takeTask(queue).await
      val tasks1 = getAllInProgress.await
      hoboClient.cancelTask(tasks1.head.getDescriptor).await
      val tasks2 = getAllInProgress.await
      tasks2.size should be < tasks1.size
    }

    "get new tasks" in {
      val tasks = hoboClient.getNewTasks(queue, 2).await.tasks
      tasks should not be empty
      tasks.flatMap(_.key).toSet shouldBe Set(taskKey1, taskKey2)
    }

    "take task by descriptor" in {
      val tasks1 = getAllInProgress.await
      hoboClient.takeTaskByDescriptor(TaskDescriptor(queue, taskKey1)).await
      val tasks2 = getAllInProgress.await
      tasks2.size should be > tasks1.size
      tasks2.flatMap(_.key) should contain(taskKey1)
    }
  }

  private def getAllInProgress: IO[Seq[Task]] = hoboClient.getInProgressTasks(queue, 0, 100).map(_.tasks)

}
