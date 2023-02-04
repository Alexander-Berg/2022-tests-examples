package ru.yandex.vertis.scheduler

import ru.yandex.vertis.scheduler.model.SchedulerInstance

import scala.util.Try

/**
 * Introduces failures to delegate [[LockManager]].
 *
 * @author dimas
 */
class BeatenLockManager(delegate: LockManager,
                        val failProbability: Float)
  extends LockManager
  with FailureInjector {

  require(delegate != null, "Null LockManager")

  def acquire(instance: SchedulerInstance, id: TaskId): Try[Boolean] =
    withProbableFailure {
      delegate.acquire(instance, id)
    }

  def list(): Try[Iterable[Lock]] = withProbableFailure {
    delegate.list()
  }

  def release(instance: SchedulerInstance, id: TaskId): Try[Unit] =
    withProbableFailure {
      delegate.release(instance, id)
    }

  def shutdown(): Try[Unit] =
    delegate.shutdown()
}
