package ru.yandex.auto.carfax.scheduler.tasks.scheduler

import auto.carfax.common.utils.tracing.Traced
import com.typesafe.config.ConfigFactory
import org.joda.time.Duration
import org.mockito.Mockito.{times, verify}
import org.scalatest.wordspec.AnyWordSpecLike
import auto.carfax.common.utils.app.TestJaegerTracingSupport.tracer
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.scheduler.OfferResult.Accepted
import ru.yandex.vertis.scheduler._
import ru.yandex.vertis.scheduler.model._
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class HangingTasksRestarterTest extends AnyWordSpecLike with MockitoSupport {
  implicit val t: Traced = Traced.empty

  private val scheduler = mock[Scheduler]

  private val apiMock = mock[SchedulerApi]

  private def jobTemplate =
    Job(DateTimeUtil.now().minusHours(3), SchedulerInstance("333", Some("3345")), ConfigFactory.empty(), None)

  private def toTaskContext(job: Option[Job]) = {
    TaskContext(model.TaskDescriptor("123", Schedule.EveryMinutes(2), 2, None), job)
  }

  private val hangingJob = jobTemplate

  private val successfulJob = jobTemplate.copy(result = Some(JobCompleted(DateTimeUtil.now(), Duration.ZERO, None)))

  private val failedJob = jobTemplate
    .copy(result = Some(JobFailed(DateTimeUtil.now(), Duration.ZERO, "Error")))

  private val inProgressJob = jobTemplate.copy(start = DateTimeUtil.now().minusMinutes(1))

  when(apiMock.list()).thenReturn {
    Try(
      List(
        toTaskContext(Some(hangingJob)),
        toTaskContext(Some(successfulJob)),
        toTaskContext(Some(failedJob)),
        toTaskContext(Some(inProgressJob)),
        toTaskContext(None)
      )
    )
  }
  when(apiMock.offer(?, ?)).thenReturn(Try(Accepted("123")))
  when(scheduler.getApi).thenReturn(apiMock)

  "HangingTasksRestarter" should {
    "restart only hanging tasks" in {
      val restarter = new HangingTasksRestarter(Feature("", _ => true))
      restarter.initScheduler(scheduler)
      restarter.restartHangingTasks

      verify(apiMock, times(1)).offer(?, ?)
    }
  }
}
