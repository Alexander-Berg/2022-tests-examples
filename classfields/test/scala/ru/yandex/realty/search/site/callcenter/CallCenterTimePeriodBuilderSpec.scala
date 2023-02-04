package ru.yandex.realty.search.site.callcenter

import com.google.protobuf.Timestamp
import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.timetable.{DatePattern, TimePattern, WeekTimetable}
import ru.yandex.vertis.telepony.model.proto.PeriodOfTime

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class CallCenterTimePeriodBuilderSpec extends SpecBase {

  "CallCenterTimePeriodBuilderSpec" should {

    val April13Daytime = DateTime.parse("2022-04-13T12:00:00") // Wednesday
    val April13Nighttime = DateTime.parse("2022-04-13T23:50:00") // Wednesday
    val April14Nighttime = DateTime.parse("2022-04-14T00:00:00") // Thursday
    val April14Daytime = DateTime.parse("2022-04-14T12:00:00") // Thursday
    val April15Daytime = DateTime.parse("2022-04-15T12:00:00") // Friday
    val April16Daytime = DateTime.parse("2022-04-16T12:00:00") // Saturday
    val April17Daytime = DateTime.parse("2022-04-17T12:00:00") // Sunday
    val April18Daytime = DateTime.parse("2022-04-18T12:00:00") // Monday
    val April19Daytime = DateTime.parse("2022-04-19T12:00:00") // Tuesday

    "Return periods (7 working days a week)" in {
      DateTimeUtils.setCurrentMillisFixed(April13Daytime.getMillis)
      val patterns = List(DatePattern.getDefault).asJava
      val weekTimetable = new WeekTimetable(patterns, WeekTimetable.getDefaultTimeZone)
      val workingPeriods = CallCenterTimePeriodBuilder.buildWorkingPeriod(Some(weekTimetable))

      workingPeriods shouldEqual (0 until 7).map(April13Daytime.plusDays).map(makeDefaultPeriodFromDate)
    }

    "Return period for daytime call in the weekend (standard 5 working days a week)" in {
      DateTimeUtils.setCurrentMillisFixed(April16Daytime.getMillis)
      val patterns = List(new DatePattern(1, 5, TimePattern.getDefault)).asJava
      val weekTimetable = new WeekTimetable(patterns, WeekTimetable.getDefaultTimeZone)
      val workingPeriods = CallCenterTimePeriodBuilder.buildWorkingPeriod(Some(weekTimetable))

      workingPeriods shouldEqual (0 until 5).map(April18Daytime.plusDays).map(makeDefaultPeriodFromDate)
    }

    "Return periods for daytime call in the day before weekend (standard 5 working days a week)" in {
      DateTimeUtils.setCurrentMillisFixed(April15Daytime.getMillis)
      val patterns = List(new DatePattern(1, 5, TimePattern.getDefault)).asJava
      val weekTimetable = new WeekTimetable(patterns, WeekTimetable.getDefaultTimeZone)
      val workingPeriods = CallCenterTimePeriodBuilder.buildWorkingPeriod(Some(weekTimetable))

      workingPeriods shouldEqual (April15Daytime +: (0 until 4).map(April18Daytime.plusDays))
        .map(makeDefaultPeriodFromDate)
    }

    "Return periods for weekday nighttime call (standard 5 working days a week) #1" in {
      DateTimeUtils.setCurrentMillisFixed(April13Nighttime.getMillis)
      val patterns = List(new DatePattern(1, 5, TimePattern.getDefault)).asJava
      val weekTimetable = new WeekTimetable(patterns, WeekTimetable.getDefaultTimeZone)
      val workingPeriods = CallCenterTimePeriodBuilder.buildWorkingPeriod(Some(weekTimetable))

      workingPeriods shouldEqual (April14Daytime +: April15Daytime +: (0 until 2).map(April18Daytime.plusDays))
        .map(makeDefaultPeriodFromDate)
    }

    "Return periods for weekday nighttime call (standard 5 working days a week) #2" in {
      DateTimeUtils.setCurrentMillisFixed(April14Nighttime.getMillis)
      val patterns = List(new DatePattern(1, 5, TimePattern.getDefault)).asJava
      val weekTimetable = new WeekTimetable(patterns, WeekTimetable.getDefaultTimeZone)
      val workingPeriods = CallCenterTimePeriodBuilder.buildWorkingPeriod(Some(weekTimetable))

      workingPeriods shouldEqual (April14Daytime +: April15Daytime +: (0 until 3).map(April18Daytime.plusDays))
        .map(makeDefaultPeriodFromDate)
    }

    "Don't update options" in {
      DateTimeUtils.setCurrentMillisFixed(April18Daytime.getMillis)
      val patterns = List(new DatePattern(1, 3, TimePattern.getDefault)).asJava
      val weekTimetable = new WeekTimetable(patterns, WeekTimetable.getDefaultTimeZone)
      val workingPeriods = CallCenterTimePeriodBuilder.buildWorkingPeriod(Some(weekTimetable))

      CallCenterTimePeriodBuilder.needToUpdate(workingPeriods) shouldEqual false
    }

    "Update options" in {
      DateTimeUtils.setCurrentMillisFixed(April18Daytime.getMillis)
      val patterns = List(new DatePattern(1, 3, TimePattern.getDefault)).asJava
      val weekTimetable = new WeekTimetable(patterns, WeekTimetable.getDefaultTimeZone)
      val workingPeriods = CallCenterTimePeriodBuilder.buildWorkingPeriod(Some(weekTimetable))
      DateTimeUtils.setCurrentMillisFixed(April19Daytime.getMillis)
      val needToUpdate = CallCenterTimePeriodBuilder.needToUpdate(workingPeriods)

      needToUpdate shouldEqual true
    }

    def makeDefaultPeriodFromDate(date: DateTime): PeriodOfTime =
      PeriodOfTime
        .newBuilder()
        .setOpenTime(Timestamp.newBuilder().setSeconds(date.secondOfDay().setCopy(32400).getMillis / 1000)) // 09:00
        .setCloseTime(Timestamp.newBuilder().setSeconds(date.secondOfDay().setCopy(75600).getMillis / 1000)) // 21:00
        .build()
  }

  override def afterAll: Unit =
    DateTimeUtils.setCurrentMillisSystem()
}
