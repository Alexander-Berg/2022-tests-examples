package ru.yandex.vertis.scheduler.impl

import ru.yandex.vertis.scheduler.builder.SchedulerBuilderBase
import ru.yandex.vertis.scheduler.impl.TestSchedulerBuilder.FailProbability
import ru.yandex.vertis.scheduler.model.Task
import ru.yandex.vertis.scheduler.{BeatenTaskService, TaskService, BeatenLockManager, LockManager}

/**
 * Builder for testing ZooKeeper scheduler.
 *
 * @author dimas
 */
trait TestSchedulerBuilder
  extends SchedulerBuilderBase {
  abstract override protected def buildLockManager(): LockManager =
    new BeatenLockManager(super.buildLockManager(), FailProbability)

  abstract override protected def buildTaskService(tasks: Iterable[Task]): TaskService =
    new BeatenTaskService(super.buildTaskService(tasks), FailProbability)
}

object TestSchedulerBuilder {
  val FailProbability = 0.4f
}
