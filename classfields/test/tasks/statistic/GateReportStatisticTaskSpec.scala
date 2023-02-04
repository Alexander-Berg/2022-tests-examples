package ru.yandex.vertis.billing.banker.tasks.statistic

import com.typesafe.config.ConfigFactory
import org.joda.time.{DateTime, Duration}
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.scheduler.model.{Job, JobCompleted, JobFailed, JobResult, SchedulerInstance}

class GateReportStatisticTaskSpec
  extends Matchers
  with AnyWordSpecLike
  with MockFactory
  with OneInstancePerTest
  with AsyncSpecBase {

  "GateReportStatisticTask" should {

    val testSchedulerInstance = SchedulerInstance("test")
    val testConfig = ConfigFactory.empty
    val testMessage = "test"
    val testDuration = Duration.standardMinutes(1)

    def job(start: DateTime, result: Option[JobResult]) =
      Some(Job(start, testSchedulerInstance, testConfig, result))

    def jobCompleted(start: DateTime) =
      job(start, Some(JobCompleted(start.plus(testDuration), testDuration)))

    def jobFailed(start: DateTime) =
      job(start, Some(JobFailed(start.plus(testDuration), testDuration, testMessage)))

    "run if last invocation was in last month" in {
      GateReportStatisticTask.schedule.shouldRunAt(
        systemTime = DateTime.parse("2021-05-01T01:00+03:00"),
        lastJob = jobCompleted(start = DateTime.parse("2021-04-01T01:00+03:00"))
      ) shouldBe true
    }

    "not run if task already succeeded in this month" in {
      GateReportStatisticTask.schedule.shouldRunAt(
        systemTime = DateTime.parse("2021-05-11T01:00+03:00"),
        lastJob = jobCompleted(start = DateTime.parse("2021-05-01T01:00+03:00"))
      ) shouldBe false
    }

    "not run if that's 1st of the month, but less than 01:00 am" in {
      GateReportStatisticTask.schedule.shouldRunAt(
        systemTime = DateTime.parse("2021-05-01T00:59+03:00"),
        lastJob = jobCompleted(start = DateTime.parse("2021-04-01T01:00+03:00"))
      ) shouldBe false
    }

    "not run if last invocation failed in this month, but too recently" in {
      GateReportStatisticTask.schedule.shouldRunAt(
        systemTime = DateTime.parse("2021-05-01T01:30+03:00"),
        lastJob = jobFailed(start = DateTime.parse("2021-05-01T01:00+03:00"))
      ) shouldBe false
    }

    "run if last invocation failed in this month more than an hour ago" in {
      GateReportStatisticTask.schedule.shouldRunAt(
        systemTime = DateTime.parse("2021-05-01T02:01+03:00"),
        lastJob = jobFailed(start = DateTime.parse("2021-05-01T01:00+03:00"))
      ) shouldBe true
    }

    "not run if it's running now" in {
      GateReportStatisticTask.schedule.shouldRunAt(
        systemTime = DateTime.parse("2021-05-01T01:01+03:00"),
        lastJob = job(start = DateTime.parse("2021-05-01T01:00+03:00"), result = None)
      ) shouldBe false
    }
  }
}
