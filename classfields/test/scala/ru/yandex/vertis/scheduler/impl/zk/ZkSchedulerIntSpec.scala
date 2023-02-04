package ru.yandex.vertis.scheduler.impl.zk

import java.util.UUID

import ru.yandex.common.monitoring.{CompoundHealthCheckRegistry, HealthChecks}
import ru.yandex.vertis.scheduler.builder.ZkSchedulerBuilder
import ru.yandex.vertis.scheduler.impl.TestSchedulerBuilder
import ru.yandex.vertis.scheduler.journal.Journal
import ru.yandex.vertis.scheduler.model.Task
import ru.yandex.vertis.scheduler.{Scheduler, SchedulerIntSpec}

import scala.concurrent.duration._

/**
 * Runnable spec on Scheduler supported by ZooKeeper
 *
 * @author dimas
 */
class ZkSchedulerIntSpec
  extends SchedulerIntSpec
  with ZooKeeperAware {

  def createSchedulers(caseId: String,
                       nrOfInstances: Int,
                       maxConcurrentTasks: Int,
                       maxRunningWeights: Option[Int],
                       tasks: Iterable[Task],
                       journal: Option[Journal]): Iterable[Scheduler] =
    Iterator.
      continually(
        createScheduler(
          caseId,
          maxConcurrentTasks,
          maxRunningWeights,
          tasks,
          journal)).
      take(nrOfInstances).
      toIterable

  def createScheduler(caseId: String,
                      maxConcurrentTasks: Int,
                      maxRunningWeights: Option[Int],
                      tasks: Iterable[Task],
                      journal: Option[Journal]) = {
    val curator = curatorBase.usingNamespace("using/namespace")
    val instanceId = UUID.randomUUID().toString.take(5)
    val b = new ZkSchedulerBuilder with TestSchedulerBuilder
    b.setCuratorFramework(curator).
      setZooKeeperBasePath(s"/path/to/scheduler/$caseId").
      setSyncPeriod(3.seconds).
      setMaxConcurrentTasks(maxConcurrentTasks).
      setSchedulerInstanceId(instanceId).
      setSchedulerName("service-name").
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
