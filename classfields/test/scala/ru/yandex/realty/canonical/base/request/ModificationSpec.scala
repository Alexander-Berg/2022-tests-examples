package ru.yandex.realty.canonical.base.request

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}

import java.time.{DayOfWeek, LocalDate, ZoneId}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ModificationSpec extends WordSpec with Matchers {

  "ModificationDate.everyWeekDayFrom" should {
    "correctly return date" in {
      // wednesday
      val date = LocalDate
        .of(2022, 6, 29)

      Seq(
        DayOfWeek.MONDAY -> date.minusDays(2),
        DayOfWeek.TUESDAY -> date.minusDays(1),
        DayOfWeek.WEDNESDAY -> date,
        DayOfWeek.THURSDAY -> date.minusDays(7).plusDays(1),
        DayOfWeek.FRIDAY -> date.minusDays(7).plusDays(2),
        DayOfWeek.SATURDAY -> date.minusDays(7).plusDays(3),
        DayOfWeek.SUNDAY -> date.minusDays(7).plusDays(4)
      ).map {
          case (day, expectedDate) =>
            day -> AbsoluteDate(expectedDate)
        }
        .foreach {
          case (day, expected) =>
            withClue(s"on $day") {
              ModificationDate.everyWeekDayFrom(date, day) shouldBe expected
            }
        }
    }
  }
}
