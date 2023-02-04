package ru.auto.salesman.tasks.user.schedule

import cats.data.NonEmptySet
import org.joda.time.LocalDate
import ru.auto.salesman.tasks.user.schedule.VipRefreshScheduleCalculator._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.JodaCatsInstances._

class VipRefreshScheduleCalculatorSpec extends BaseSpec {

  private val calculator = new VipRefreshScheduleCalculator()

  import calculator._

  "calculate valid refresh activation dates" in {
    val startFrom =
      LocalDate.parse("2020-03-01").toDateTimeAtStartOfDay
    val vipDeadline = startFrom.plusDays(60)
    val currentTime = startFrom.plusHours(12)
    scheduleFromNow(vipDeadline)
      .provideConstantClock(currentTime)
      .map(
        _.dates shouldBe NonEmptySet.of(
          LocalDate.parse("2020-03-01"),
          LocalDate.parse("2020-03-08"),
          LocalDate.parse("2020-03-15"),
          LocalDate.parse("2020-03-22"),
          LocalDate.parse("2020-03-29"),
          LocalDate.parse("2020-04-05"),
          LocalDate.parse("2020-04-12"),
          LocalDate.parse("2020-04-19"),
          LocalDate.parse("2020-04-26")
        )
      )
  }.success

  "throw exception if vip ends before first refresh" in {
    val startFrom =
      LocalDate.parse("2020-03-01").toDateTimeAtStartOfDay
    val vipDeadline = startFrom.plusDays(6)
    val currentTime = startFrom.plusHours(12)
    scheduleFromNow(
      vipDeadline
    ).provideConstantClock(currentTime).failure.exception shouldBe an[InvalidVipDuration]
  }

  "calculate valid refresh activation time" in {
    val startFrom = LocalDate.parse("2020-03-01").toDateTimeAtStartOfDay
    val vipDeadline = startFrom.plusDays(60)
    val currentTime = startFrom.plusHours(12)
    scheduleFromNow(vipDeadline)
      .provideConstantClock(currentTime)
      .map(_.time shouldEqual currentTime.plusHours(1).toLocalTime)
  }.success
}
