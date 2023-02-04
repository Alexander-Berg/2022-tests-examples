package ru.yandex.vertis.scheduler

import com.typesafe.config.Config
import org.joda.time.DateTime
import ru.yandex.vertis.scheduler.model.{JobResult, SchedulerInstance, TaskContext}

import scala.util.Try

/**
  * Introduces failures to [[TaskService]]
  *
  * @author dimas
  */
class BeatenTaskService(delegate: TaskService,
                        val failProbability: Float)
  extends TaskService
    with FailureInjector {

  def get(id: TaskId): Try[TaskContext] = withProbableFailure {
    delegate.get(id)
  }

  def list(): Try[Iterable[TaskContext]] = withProbableFailure {
    delegate.list()
  }

  def start(id: TaskId, instance: SchedulerInstance, config: Config, at: DateTime): Try[Unit] =
    withProbableFailure {
      delegate.start(id, instance, config, at)
    }

  def finish(id: TaskId, instance: SchedulerInstance,
             result: JobResult): Try[Unit] =
    withProbableFailure {
      delegate.finish(id, instance, result)
    }

  def shutdown(): Try[Unit] =
    delegate.shutdown()
}
