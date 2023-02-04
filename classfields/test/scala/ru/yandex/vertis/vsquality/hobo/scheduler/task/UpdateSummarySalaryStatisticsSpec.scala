package ru.yandex.vertis.vsquality.hobo.scheduler.task

import org.joda.time.DateTime

import ru.yandex.vertis.vsquality.hobo.scheduler.task.UpdateSummarySalaryStatistics.{reportRanges, ReportRange}
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

/**
  * @author akhazhoyan 03/2019
  */

class UpdateSummarySalaryStatisticsSpec extends SpecBase {

  "reportRanges" should {
    "return start of day, week and month" in {
      val now = DateTime.parse("2019-02-15T14:23:04.213")
      val ReportRange(startOfDay, startOfWeek, startOfMonth) = reportRanges(now)
      startOfDay should equal(DateTime.parse("2019-02-15T00:00:00"))
      startOfWeek should equal(DateTime.parse("2019-02-11T00:00:00"))
      startOfMonth should equal(DateTime.parse("2019-02-01T00:00:00"))
    }

    "ceil start of week if it is before than start of month" in {
      val now = DateTime.parse("2019-03-01T14:23:04.213")
      val ReportRange(startOfDay, startOfWeek, startOfMonth) = reportRanges(now)
      startOfDay should equal(DateTime.parse("2019-03-01T00:00:00"))
      startOfWeek should equal(DateTime.parse("2019-03-01T00:00:00"))
      startOfMonth should equal(DateTime.parse("2019-03-01T00:00:00"))
    }
  }
}
