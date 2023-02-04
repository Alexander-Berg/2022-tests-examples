package ru.auto.salesman.service.user.schedule

import cats.data.NonEmptySet
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalTime}
import org.scalacheck.{Gen, ShrinkLowPriority}
import ru.auto.salesman.model.user.schedule.ScheduleParameters.{OnceAtDates, OnceAtTime}
import ru.auto.salesman.model.user.schedule.{ProductSchedule, ScheduleParameters}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.user.schedule.ScheduleEvaluator.Context
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.ScheduleInstanceGenerators
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.util.JodaCatsInstances._
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SimpleEvaluatorSpec
    extends BaseSpec
    with ProductScheduleModelGenerators
    with ScheduleInstanceGenerators
    with ShrinkLowPriority {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val defaultPeriod = 12.hours

  def withParameters(scheduleParams: ScheduleParameters): ProductSchedule =
    ProductScheduleGen.next.copy(scheduleParameters = scheduleParams)

  val allWeekdays = (1 to 7).toSet

  def time(hour: Int, minute: Int = 0): LocalTime =
    new LocalTime(hour, minute, 0)

  def context(lastPending: Option[DateTime] = None): Context = {
    val instanceOpt =
      lastPending.map(dt => ScheduleInstanceGen.next.copy(fireTime = dt))
    Context(instanceOpt)
  }

  def onceAtTime(
      time: LocalTime,
      weekdays: Set[Int] = allWeekdays,
      tz: DateTimeZone = DateTimeUtil.DefaultTimeZone
  ): OnceAtTime =
    OnceAtTime(weekdays, time, tz)

  def evaluate(
      schedule: ProductSchedule,
      lastPendingTime: Option[DateTime] = None,
      period: FiniteDuration = defaultPeriod
  ): Seq[DateTime] =
    new SimpleEvaluator(period).evaluate(schedule, context(lastPendingTime)).get

  "SimpleEvaluator" should {

    "calculates dayShifts" in {
      val evaluator = new SimpleEvaluator(defaultPeriod)

      for (i <- 1 to 7)
        (evaluator.dayShifts(i, allWeekdays) should contain)
          .theSameElementsInOrderAs(0 to 6)

      for (weekday <- 1 to 7)
        for (today <- 1 to 7) {
          val shifts = evaluator.dayShifts(today, Set(weekday))
          val expected =
            if (today <= weekday) weekday - today else weekday - today + 7
          shifts should (have size 1 and contain(expected))
        }
    }

    "calculates dayShift" in {
      val evaluator = new SimpleEvaluator(defaultPeriod)
      evaluator.dayShift(1, allWeekdays, allowZero = true) shouldBe 0
      evaluator.dayShift(1, allWeekdays, allowZero = false) shouldBe 1
      evaluator.dayShift(1, Set(3), allowZero = true) shouldBe 2
      evaluator.dayShift(1, Set(3), allowZero = false) shouldBe 2
      evaluator.dayShift(4, Set(2, 4), allowZero = true) shouldBe 0
      evaluator.dayShift(4, Set(2, 4), allowZero = false) shouldBe 5
    }

    "calculates dayShift for today" in {
      val evaluator = new SimpleEvaluator(defaultPeriod)
      evaluator.dayShift(today = 7, Set(7), allowZero = false) shouldBe 7
    }

    "use current time if last pending is not present or is in past" in {
      forAll(Gen.option(dateTimeInPast()), Gen.chooseNum(0, 23)) {
        (lastPending, runHour) =>
          val localNow = DateTimeUtil.now().toLocalTime
          // not use next hour to avoid problems with hour change during test run
          whenever(
            runHour != (if (localNow.getHourOfDay < 23)
                          localNow.getHourOfDay + 1
                        else 0)
          ) {
            val localNow = DateTimeUtil.now().toLocalTime

            val timeToRun =
              time((localNow.getHourOfDay + Gen.chooseNum(2, 22).next) % 24)
            val schedule = withParameters(onceAtTime(timeToRun))
            val fireTimes = evaluate(schedule, lastPending, period = 7.days)
            fireTimes should have size 7
            fireTimes.map(
              _.getDayOfWeek
            ) should contain theSameElementsAs allWeekdays

            val expectedFirst =
              if (timeToRun.isBefore(localNow))
                DateTimeUtil
                  .now()
                  .withMillisOfDay(timeToRun.getMillisOfDay)
                  .plusDays(1)
              else
                DateTimeUtil.now().withMillisOfDay(timeToRun.getMillisOfDay)
            fireTimes.head shouldBe expectedFirst
          }
      }
    }

    def forWeekForward(name: String)(testCode: DateTime => Unit): Unit = {
      val baseStartTime =
        DateTimeUtil.now().withHourOfDay(12).withMinuteOfHour(0)
      for (i <- 1 to 7) {
        val startTime = baseStartTime.plusDays(i)
        s"$name from ${startTime.getDayOfWeek} day of week" in {
          testCode(startTime)
        }
      }
    }

    forWeekForward("evaluate everyday schedule") { startTime =>
      val schedule = withParameters(onceAtTime(time(12)))
      val fireTimes = evaluate(schedule, Some(startTime), period = 7.days)
      fireTimes should have size 7
      fireTimes.head.isAfter(startTime) shouldBe true
      fireTimes.map(
        _.getDayOfWeek
      ) should (have size 7 and contain theSameElementsAs allWeekdays)
      fireTimes.map(_.toLocalTime).toSet shouldBe Set(time(12))
    }

    forWeekForward("correctly evaluate every odd work day schedule") { startTime =>
      val startDayOfWeek = startTime.getDayOfWeek
      val days = Set(1, 3, 5)
      val schedule = withParameters(onceAtTime(time(12), days))
      val fireTimes = evaluate(schedule, Some(startTime), period = 14.days)
      fireTimes should have size 6
      fireTimes.head.isAfter(startTime) shouldBe true
      val expectedDays = Seq(1, 3, 5, 8, 10, 12, 15, 17, 19)
        .dropWhile(_ <= startDayOfWeek)
        .take(6)
        .map(_ % 7)
      (fireTimes.map(_.getDayOfWeek) should contain)
        .theSameElementsInOrderAs(expectedDays)
      fireTimes.map(_.toLocalTime).toSet shouldBe Set(time(12))
    }

    forWeekForward("correctly evaluate every even work day schedule") { startTime =>
      val startDayOfWeek = startTime.getDayOfWeek
      val days = Set(2, 4)
      val schedule = withParameters(onceAtTime(time(12), days))
      val fireTimes = evaluate(schedule, Some(startTime), period = 14.days)
      fireTimes should have size 4
      fireTimes.head.isAfter(startTime) shouldBe true
      val expectedDays = Seq(2, 4, 9, 11, 16, 18)
        .dropWhile(_ <= startDayOfWeek)
        .take(4)
        .map(_ % 7)
      (fireTimes.map(_.getDayOfWeek) should contain)
        .theSameElementsInOrderAs(expectedDays)
      fireTimes.map(_.toLocalTime).toSet shouldBe Set(time(12))
    }

    forWeekForward("correctly evaluate every odd day including start") { startTime =>
      val startDayOfWeek = startTime.getDayOfWeek
      if (startDayOfWeek % 2 == 1) {
        val days = Set(1, 3, 5, 7)
        val schedule = withParameters(onceAtTime(time(12, 30), days))
        val fireTimes = evaluate(schedule, Some(startTime), period = 7.days)
        fireTimes should have size 4
        fireTimes.head.isAfter(startTime) shouldBe true
        val expectedDays = Seq(1, 3, 5, 7, 8, 10, 12, 14)
          .dropWhile(_ < startDayOfWeek)
          .take(4)
          .map(d => (d - 1) % 7 + 1)
        (fireTimes.map(_.getDayOfWeek) should contain)
          .theSameElementsInOrderAs(expectedDays)
        fireTimes.map(_.toLocalTime).toSet shouldBe Set(time(12, 30))
      }
    }

    forWeekForward("correctly evaluate every even day including start") { startTime =>
      val startDayOfWeek = startTime.getDayOfWeek
      if (startDayOfWeek % 2 == 0) {
        val days = Set(2, 4, 6)
        val schedule = withParameters(onceAtTime(time(12, 30), days))
        val fireTimes = evaluate(schedule, Some(startTime), period = 7.days)
        fireTimes should have size 3
        fireTimes.head.isAfter(startTime) shouldBe true
        val expectedDays = Seq(2, 4, 6, 9, 11, 13)
          .dropWhile(_ < startDayOfWeek)
          .take(3)
          .map(_ % 7)
        (fireTimes.map(_.getDayOfWeek) should contain)
          .theSameElementsInOrderAs(expectedDays)
        fireTimes.map(_.toLocalTime).toSet shouldBe Set(time(12, 30))
      }
    }

    "correctly evaluate in different timezone" in {
      forAll(TimeZoneGen, jodaLocalTimeGen) { (timezone, localTime) =>
        val schedule = withParameters(onceAtTime(localTime, tz = timezone))
        val fireTimes = evaluate(schedule, period = 7.days)
        fireTimes.foreach { ft =>
          ft.getZone shouldBe DateTimeUtil.DefaultTimeZone
          ft.withZone(timezone).toLocalTime shouldBe localTime
        }
      }
    }

    "don't evaluate once at dates if too early" in {
      val schedule = withParameters(
        OnceAtDates(
          NonEmptySet.one(LocalDate.now().plusDays(2)),
          time(12),
          DateTimeUtil.DefaultTimeZone
        )
      )
      evaluate(schedule, period = 1.day) shouldBe empty
    }

    "don't evaluate once at dates if too late" in {
      val now = LocalDate.now()
      val schedule = withParameters(
        OnceAtDates(
          NonEmptySet.one(now.minusDays(1)),
          time(23),
          DateTimeUtil.DefaultTimeZone
        )
      )
      evaluate(schedule, period = 1.day) shouldBe empty
    }

    "evaluate soon once at dates schedule" in {
      val now = DateTime.now()
      val schedule = withParameters(
        OnceAtDates(
          NonEmptySet
            .of(now.plusHours(10).toLocalDate, now.plusDays(2).toLocalDate),
          now.plusHours(10).toLocalTime,
          DateTimeUtil.DefaultTimeZone
        )
      )
      val result = evaluate(schedule, period = 1.day)
      result should contain only now.plusHours(10)
    }

    "evaluate soon once at dates schedule if first date passed" in {
      val now = DateTime.now()
      val schedule = withParameters(
        OnceAtDates(
          NonEmptySet
            .of(
              now.plusHours(10).toLocalDate,
              now.plusDays(2).toLocalDate,
              now.minusDays(1).toLocalDate
            ),
          now.plusHours(10).toLocalTime,
          DateTimeUtil.DefaultTimeZone
        )
      )
      val result = evaluate(schedule, period = 1.day)
      result should contain only now.plusHours(10)
    }
  }
}
