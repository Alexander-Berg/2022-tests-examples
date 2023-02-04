package ru.yandex.vertis.parsing.util.workmoments

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.components.time.TimeService

import scala.concurrent.duration._

/**
  * Created by andrey on 12/6/17.
  */
@RunWith(classOf[JUnitRunner])
class WorkMomentsTest extends FunSuite with OptionValues with MockitoSupport {
  implicit private val mockedTimeService: TimeService = mock[TimeService]

  private def setTime(moment: DateTime): Unit = {
    when(mockedTimeService.getNow).thenReturn(moment)
  }

  test("isWorkMoment") {
    val workMomentUtils = WorkMoments.daily(15, 0)
    val now = new DateTime()
    setTime(now.withHourOfDay(14))
    assert(!workMomentUtils.isWorkMoment)
    setTime(now.withHourOfDay(15))
    assert(workMomentUtils.isWorkMoment)
    assert(workMomentUtils.workMoment.value == 0)
    setTime(now.withHourOfDay(16))
    assert(!workMomentUtils.isWorkMoment)
    setTime(now.withHourOfDay(17))
    assert(!workMomentUtils.isWorkMoment)
  }

  test("durationToNextWorkMoment") {
    val workMomentUtils = WorkMoments.daily(15, 10)
    setTime(new DateTime().withHourOfDay(12).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0))
    val now: DateTime = new DateTime()
    val targetMoment1 = now.withHourOfDay(15).withMinuteOfHour(10).withSecondOfMinute(0).withMillisOfSecond(0)
    val (duration1, workMoment1) = workMomentUtils.durationToNextWorkMoment
    assert(workMoment1 == targetMoment1)
    assert(duration1.toMillis == 3.hours.plus(10.minutes).toMillis)

    setTime(new DateTime().withHourOfDay(15).withMinuteOfHour(30).withSecondOfMinute(0).withMillisOfSecond(0))
    val tomorrow: DateTime = now.plusDays(1)
    val targetMoment2 = tomorrow.withHourOfDay(15).withMinuteOfHour(10).withSecondOfMinute(0).withMillisOfSecond(0)
    val (duration2, workMoment2) = workMomentUtils.durationToNextWorkMoment
    assert(workMoment2 == targetMoment2)
    assert(duration2.toMillis == 23.hours.plus(40.minutes).toMillis)
  }

  test("WorkMomentUtils2.fromPeriods") {
    val now: DateTime = new DateTime()
    val dayStart: DateTime = now.withMillisOfDay(0)
    val workMomentUtils = WorkMoments.periods(Seq(MultipleWorkMoments(8, 0, 20, 0, 5, 1.minute)))
    (8 * 60 to 20 * 60).foreach(minute => {
      val needIsWorkMoment = minute % 5 == 0
      val workMomentNum = if (needIsWorkMoment) Some((minute - 480) / 5) else None
      val moment: DateTime = dayStart.plusMinutes(minute)
      setTime(moment)
      val nextWorkMinute = (minute + 1 to minute + 5).find(_ % 5 == 0).get
      val nextWorkMoment =
        if (nextWorkMinute < 1205) dayStart.plusMinutes(nextWorkMinute)
        else dayStart.plusDays(1).plusHours(8)
      assert(workMomentUtils.isWorkMoment == needIsWorkMoment, minute)
      assert(workMomentUtils.workMoment == workMomentNum, minute)
      assert(
        workMomentUtils.durationToNextWorkMoment ==
          ((nextWorkMoment.getMillis - moment.getMillis).millis, nextWorkMoment)
      )
    })
  }

  test("every week") {
    val now = DateTime.now()
    setTime(now)
    val nowMinuteOfDay = now.getMinuteOfDay
    val everyWeek = WorkMoments.weekly(now.getDayOfWeek, now.getHourOfDay, now.getMinuteOfHour, 1.minute)

    assert(everyWeek.isWorkMoment)
    assert(everyWeek.durationToNextWorkMoment._2 == now.withMillisOfDay(0).plusMinutes(nowMinuteOfDay).plusDays(7))

    setTime(now.plusMinutes(5))
    assert(!everyWeek.isWorkMoment)
    assert(everyWeek.durationToNextWorkMoment._2 == now.withMillisOfDay(0).plusMinutes(nowMinuteOfDay).plusDays(7))

    setTime(now.minusMinutes(5))
    assert(!everyWeek.isWorkMoment)
    assert(everyWeek.durationToNextWorkMoment._2 == now.withSecondOfMinute(0).withMillisOfSecond(0))

    val everyWeek2 = WorkMoments.weekly(now.getDayOfWeek, now.getHourOfDay, now.getMinuteOfHour, 10.minutes)

    setTime(now)
    assert(everyWeek2.isWorkMoment)
    setTime(now.plusMinutes(5))
    assert(everyWeek2.isWorkMoment)
    setTime(now.plusMinutes(11))
    assert(!everyWeek2.isWorkMoment)
    setTime(now.minusMinutes(1))
    assert(!everyWeek2.isWorkMoment)
  }

  test("every 2 minute with 1 minute offset") {
    val period1 = WorkMoments.every(2.minutes)
    val period2 = WorkMoments.period(0, 1, 23, 59, 2, 1.minute)

    assert(period1.timeline.length == period2.timeline.length)

    assert(
      period1.timeline
        .zip(period2.timeline)
        .forall(x => {
          x._1.asInstanceOf[DailyTimePoint].minute != x._2.asInstanceOf[DailyTimePoint].minute
        })
    )
  }
}
