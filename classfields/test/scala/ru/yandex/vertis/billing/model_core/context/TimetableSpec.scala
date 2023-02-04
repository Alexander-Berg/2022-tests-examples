package ru.yandex.vertis.billing.model_core.context

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.{LocalDateInterval, LocalTimeInterval, Timetable}

/**
  * Spec on [[Timetable]]
  *
  * @author ruslansd
  */
class TimetableSpec extends AnyWordSpec with Matchers {

  private val TimeIntervals =
    Iterable(
      LocalTimeInterval("09:00", "13:00"),
      LocalTimeInterval("14:00", "19:00")
    )

  private val DateInterval =
    Iterable(
      LocalDateInterval("2016-09-10", "2016-09-13", TimeIntervals),
      LocalDateInterval("2016-09-15", "2016-09-17", TimeIntervals)
    )

  private val Timetable =
    new Timetable(DateInterval, DateTimeZone.forID("+03:00"))

  "Timetable" should {

    "work correctly with time in same time zone" in {
      val shouldBeContained = Iterable(
        new DateTime("2016-09-11T10:00:00+03:00"),
        new DateTime("2016-09-10T09:00:00+03:00"),
        new DateTime("2016-09-12T13:00:00+03:00"),
        new DateTime("2016-09-13T19:00:00+03:00"),
        new DateTime("2016-09-12T14:00:00+03:00"),
        new DateTime("2016-09-17T19:00:00+03:00")
      )
      val shouldNotBeContained = Iterable(
        new DateTime("2016-09-11T08:59:59+03:00"),
        new DateTime("2016-09-10T07:00:00+03:00"),
        new DateTime("2016-09-12T13:30:00+03:00"),
        new DateTime("2016-09-13T19:00:01+03:00"),
        new DateTime("2016-09-14T10:00:00+03:00"),
        new DateTime("2016-09-18T11:00:00+03:00"),
        new DateTime("2016-09-09T11:00:00+03:00")
      )

      shouldBeContained.foreach(t => Timetable.contains(t) shouldBe true)
      shouldNotBeContained.foreach(t => Timetable.contains(t) shouldBe false)
    }

    "work correctly with time in different time zones" in {
      val shouldBeContained = Iterable(
        new DateTime("2016-09-11T10:00:00+04:00"),
        new DateTime("2016-09-10T11:00:00+05:00"),
        new DateTime("2016-09-12T13:00:00+03:45"),
        new DateTime("2016-09-13T16:00:00+01:00"),
        new DateTime("2016-09-12T14:00:00+00:00"),
        new DateTime("2016-09-17T19:00:00+10:00"),
        new DateTime("2016-09-14T01:00:00+10:00"),
        new DateTime("2016-09-12T08:00:00+00:00")
      )

      val shouldNotBeContained = Iterable(
        new DateTime("2016-09-11T09:50:00+04:00"),
        new DateTime("2016-09-10T10:00:00+05:00"),
        new DateTime("2016-09-12T14:00:00+03:45"),
        new DateTime("2016-09-13T18:00:00+01:00"),
        new DateTime("2016-09-14T14:00:00+00:00"),
        new DateTime("2016-09-12T12:00:00+10:00")
      )

      shouldBeContained.foreach(t => Timetable.contains(t) shouldBe true)
      shouldNotBeContained.foreach(t => Timetable.contains(t) shouldBe false)
    }

  }

}
