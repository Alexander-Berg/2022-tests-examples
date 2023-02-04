package ru.auto.salesman.schedule

import org.joda.time.DateTime
import ru.auto.salesman.test.BaseSpec

class FirstDayOfMonthScheduleSpec extends BaseSpec {

  "first day of month schedule " should {
    "run and then not run" in {
      val schedule = FirstDayOfMonthSchedule(hourOfDay = 7)

      schedule.shouldRun(
        DateTime.parse("2017-09-01T06:00:00+03:00"),
        None
      ) shouldBe false
      schedule.shouldRun(
        DateTime.parse("2017-09-01T07:00:00+03:00"),
        None
      ) shouldBe true
      schedule.shouldRun(
        DateTime.parse("2017-09-01T08:00:00+03:00"),
        None
      ) shouldBe false

      schedule.shouldRun(
        DateTime.parse("2017-08-31T23:00:00+03:00"),
        None
      ) shouldBe false
      schedule.shouldRun(
        DateTime.parse("2017-09-02T08:00:00+03:00"),
        None
      ) shouldBe false
    }
  }
}
