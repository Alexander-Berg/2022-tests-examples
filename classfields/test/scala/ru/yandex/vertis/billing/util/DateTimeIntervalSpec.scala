package ru.yandex.vertis.billing.util

import org.scalatest.matchers.should.Matchers
import DateTimeUtils.now
import org.joda.time.DateTime
import org.scalatest.wordspec.AnyWordSpec

/**
  * Spec on [[DateTimeInterval]]
  *
  * @author ruslansd
  */
class DateTimeIntervalSpec extends AnyWordSpec with Matchers {

  private def interval(from: String, to: String): DateTimeInterval = {
    DateTimeInterval(DateTime.parse(from), DateTime.parse(to))
  }

  "DateTimeInterval" should {
    "provide all local dates" in {
      val interval = DateTimeInterval(now().minusDays(1), now().plusDays(1))
      val expected = List(now().minusDays(1).toLocalDate, now().toLocalDate, now().plusDays(1).toLocalDate)

      DateTimeInterval.asLocalDates(interval).toList shouldBe expected
    }

    "provide single local date" in {
      val interval = DateTimeInterval(now(), now().plus(1))
      val expected = List(now().toLocalDate)

      DateTimeInterval.asLocalDates(interval).toList shouldBe expected
    }

    "split interval with 4 days" in {
      val input = interval("2022-01-10T09:00:00.000+03:00", "2022-01-13T15:00:00.000+03:00")

      val expected = Iterable(
        interval("2022-01-10T09:00:00.000+03:00", "2022-01-10T23:59:59.999+03:00"),
        interval("2022-01-11T00:00:00.000+03:00", "2022-01-11T23:59:59.999+03:00"),
        interval("2022-01-12T00:00:00.000+03:00", "2022-01-12T23:59:59.999+03:00"),
        interval("2022-01-13T00:00:00.000+03:00", "2022-01-13T15:00:00.000+03:00")
      )

      DateTimeInterval.splitByDays(input) should contain theSameElementsAs expected
    }

    "split interval with 2 days" in {
      val input = interval("2022-10-10T14:00:00.000+03:00", "2022-10-11T12:00:00.000+03:00")

      val expected = Iterable(
        interval("2022-10-10T14:00:00.000+03:00", "2022-10-10T23:59:59.999+03:00"),
        interval("2022-10-11T00:00:00.000+03:00", "2022-10-11T12:00:00.000+03:00")
      )

      DateTimeInterval.splitByDays(input) should contain theSameElementsAs expected
    }

    "split interval with 1 days" in {
      val input = interval("2022-08-19T14:00:00.000+03:00", "2022-08-19T18:00:00.000+03:00")
      DateTimeInterval.splitByDays(input) should contain theSameElementsAs Iterable(input)
    }
  }

}
