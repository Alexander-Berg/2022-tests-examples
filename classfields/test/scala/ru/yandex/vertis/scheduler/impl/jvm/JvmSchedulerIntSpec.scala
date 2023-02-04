package ru.yandex.vertis.scheduler.impl.jvm

import java.util.UUID

import com.codahale.metrics.health.HealthCheckRegistry
import ru.yandex.common.monitoring.{CompoundHealthCheckRegistry, HealthCheckRegistryFactories, HealthChecks}
import ru.yandex.vertis.scheduler.builder.{Builders, JvmSchedulerBuilder}
import ru.yandex.vertis.scheduler.impl.TestSchedulerBuilder
import ru.yandex.vertis.scheduler.journal.Journal
import ru.yandex.vertis.scheduler.logging.LoggingLockManager
import ru.yandex.vertis.scheduler.model.Task
import ru.yandex.vertis.scheduler.{Scheduler, SchedulerIntSpec}

import scala.concurrent.duration._

/**
 * Runnable spec on Scheduler supported by JVM.
 *
 * @author dimas
 */
class JvmSchedulerIntSpec
  extends SchedulerIntSpec {

  def createSchedulers(caseId: String,
                       nrOfInstances: Int,
                       maxConcurrentTasks: Int,
                       maxRunningWeights: Option[Int],
                       tasks: Iterable[Task],
                       journal: Option[Journal]): Iterable[Scheduler] = {
    val lockManager = new JvmLockManager
    Iterator.
      continually(
        createScheduler(
          caseId,
          maxConcurrentTasks,
          maxRunningWeights,
          lockManager,
          tasks,
          journal)).
      take(nrOfInstances).
      toIterable
  }

  def createScheduler(caseId: String,
                      maxConcurrentTasks: Int,
                      maxRunningWeights: Option[Int],
                      lockManager: JvmLockManager,
                      tasks: Iterable[Task],
                      journal: Option[Journal]) = {
    val instanceId = UUID.randomUUID().toString.take(5)
    val b = new JvmSchedulerBuilder with TestSchedulerBuilder
    b.setMaxConcurrentTasks(maxConcurrentTasks).
      setSchedulerInstanceId(instanceId).
      setSchedulerName("service-name").
      setLockManager(lockManager).
      setHealthChecks(new CompoundHealthCheckRegistry()).
      register(tasks)

    journal foreach {
      j => b.setJobJournal(j)
    }
    maxRunningWeights.map(b.setMaxRunningWeight)
    b.setLockCorrectPeriod(5.seconds)
    b.build()
  }
}
