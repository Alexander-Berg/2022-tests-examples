package ru.yandex.vertis.scheduler

import ru.yandex.vertis.scheduler.model.{SchedulerInstance, Schedule, TaskDescriptor}

/**
 * Some useful object for test scheduler stuff.
 *
 * @author dimas
 */
trait TestData {
  val descriptor1 = TaskDescriptor("1", Schedule.EveryMinutes(10))
  val descriptor2 = TaskDescriptor("2", Schedule.EveryHours(1))
  val descriptor3 = TaskDescriptor("3", Schedule.EverySeconds(50))

  val descriptors = Set(descriptor1, descriptor2, descriptor3)

  val instance1 = SchedulerInstance("foo", Some("Scheduler"))
  val instance2 = SchedulerInstance("bar", Some("Scheduler"))
}

object TestData extends TestData
