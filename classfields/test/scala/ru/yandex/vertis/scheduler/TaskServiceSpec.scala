package ru.yandex.vertis.scheduler

import com.typesafe.config.ConfigFactory
import org.joda.time.Duration
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.vertis.scheduler.model.{Job, JobCompleted, TaskContext, TaskDescriptor}

/**
  * Basic specs on [[TaskService]].
  *
  * @author dimas
  */
trait TaskServiceSpec
  extends Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  import TestData._

  def getDescriptors: Iterable[TaskDescriptor] = descriptors

  def taskService: TaskService

  "TaskService" should {
    "list descriptors" in {
      val got = taskService.list().get
      got.map(_.descriptor).toSet should be(descriptors)
    }

    "get exists descriptor by id" in {
      for {
        descriptor <- descriptors
        taskContext = taskService.get(descriptor.id).get
      } taskContext should be(TaskContext(descriptor, None))
    }

    "throw NoSuchElementException on operations unknown task" in {
      intercept[NoSuchElementException] {
        taskService.get("unknown-task-id").get
      }
      intercept[NoSuchElementException] {
        taskService.start("unknown-task-id", instance1, ConfigFactory.empty(), now()).get
      }
      intercept[NoSuchElementException] {
        taskService.finish("unknown-task-id", instance1, JobCompleted(now(), Duration.ZERO, None)).get
      }
    }

    "not finish not started task" in {
      val jobResult = JobCompleted(now(), Duration.ZERO, Some("Job done"))
      intercept[IllegalStateException] {
        taskService.finish(descriptor2.id, instance1, jobResult).get
      }
    }

    "mark task as started" in {
      val startTime = now()
      val config = ConfigFactory.empty()
      taskService.start(descriptor1.id, instance1, config, startTime).get
      taskService.get(descriptor1.id).get should
        be(TaskContext(descriptor1, Some(Job(startTime, instance1, config, None))))
    }

    "not finish task started by another scheduler instance" in {
      val jobResult = JobCompleted(now(), Duration.ZERO, Some("Job done"))
      intercept[IllegalStateException] {
        taskService.finish(descriptor1.id, instance2, jobResult).get
      }
    }

    "mark task as finished" in {
      val stopTime = now()
      val jobResult = JobCompleted(stopTime, Duration.ZERO, Some("Job done"))

      val pendingTask = taskService.get(descriptor1.id).get
      val startTime = pendingTask.lastJob.get.start

      taskService.finish(descriptor1.id, instance1, jobResult).get
      taskService.get(descriptor1.id).get should
        be(TaskContext(descriptor1,
          Some(Job(startTime, instance1, ConfigFactory.empty(), Some(jobResult)))))
    }

    "not finish already finished task" in {
      val jobResult = JobCompleted(now(), Duration.ZERO, Some("Job done"))
      intercept[IllegalStateException] {
        taskService.finish(descriptor1.id, instance1, jobResult).get
      }
    }
  }

  override protected def beforeAll(): Unit = {
    try taskService.shutdown()
    finally super.beforeAll()
  }
}
