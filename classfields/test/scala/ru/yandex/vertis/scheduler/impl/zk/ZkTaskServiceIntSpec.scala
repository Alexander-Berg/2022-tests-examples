package ru.yandex.vertis.scheduler.impl.zk

import com.typesafe.config.ConfigFactory
import ru.yandex.vertis.scheduler.logging.{LoggingTaskService, LoggingTaskServiceMixin}
import ru.yandex.vertis.scheduler.{LastJob, TaskServiceSpec, now}

import scala.util.Try

/**
 * Runnable specs on [[ZkTaskService]].
 *
 * @author dimas
 */
class ZkTaskServiceIntSpec
  extends TaskServiceSpec
  with ZooKeeperAware {

  import ru.yandex.vertis.scheduler.TestData._

  val taskService = new ZkTaskService(
    getDescriptors,
    curatorBase,
    "/project/module/scheduler/jobs")
    with LoggingTaskServiceMixin

  "TaskService within another scheduler instance" should {
    "see started task" in {

      val anotherTaskService = new ZkTaskService(
        getDescriptors,
        curatorBase,
        "/project/module/scheduler/jobs")
        with LoggingTaskServiceMixin

      anotherTaskService.list().get.size should be(getDescriptors.size)

      val awaiter = new Awaiter[String, LastJob]
      anotherTaskService.backedMap.subscribe(awaiter)

      taskService.start(descriptor3.id, instance1, ConfigFactory.empty(), now()).get

      awaiter.await()

      anotherTaskService.get(descriptor3.id).get.lastJob.isDefined should be(true)

      anotherTaskService.shutdown()
    }
  }

}
