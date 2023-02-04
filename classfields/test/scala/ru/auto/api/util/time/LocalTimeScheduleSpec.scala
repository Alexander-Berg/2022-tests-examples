package ru.auto.api.util.time

import ru.auto.api.BaseSpec
import java.time.LocalTime
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.MatchResult

class LocalTimeScheduleSpec extends BaseSpec {
  "ofHours" should {
    "work as expected for simple cases" in {
      val result = LocalTimeSchedule.ofHours(10, 18)
      unwrap(result) shouldBe Seq(
        (LocalTime.of(10, 0), LocalTime.of(18, 0))
      )
    }

    "work as expected when ending at midnight" in {
      val result = LocalTimeSchedule.ofHours(10, 24)
      unwrap(result) shouldBe Seq(
        (LocalTime.of(10, 0), LocalTime.MAX)
      )
    }

    "work as expected when wrapping around midnight" in {
      val result = LocalTimeSchedule.ofHours(10, 1)
      unwrap(result) shouldBe Seq(
        (LocalTime.MIN, LocalTime.of(1, 0)),
        (LocalTime.of(10, 0), LocalTime.MAX)
      )
    }
  }

  "intersect" should {
    "work as expected for simple cases" in {
      val result = LocalTimeSchedule.ofHours(10, 18).intersect(LocalTimeSchedule.ofHours(12, 20))
      result should beHours((12, 18))
    }

    "work as expected for schedules with multiple intervals" in {
      val result = LocalTimeSchedule.ofHours(10, 1).intersect(LocalTimeSchedule.ofHours(12, 3))
      result should beHours((0, 1), (12, 0))
    }

    "omit degenerate intervals" in {
      val result = LocalTimeSchedule
        .of(LocalTime.MIN, LocalTime.of(10, 0))
        .intersect(LocalTimeSchedule.of(LocalTime.of(10, 0), LocalTime.MAX))
      result.isEmpty shouldBe true
    }
  }

  "contains" should {
    "work as expected" in {
      val s = LocalTimeSchedule.ofHours(10, 1).intersect(LocalTimeSchedule.ofHours(12, 3))
      s should beHours((0, 1), (12, 0)) // sanity check

      s.contains(LocalTime.of(1, 0)) shouldBe true
      s.contains(LocalTime.of(1, 1)) shouldBe false
      s.contains(LocalTime.of(11, 59)) shouldBe false
      s.contains(LocalTime.of(12, 0)) shouldBe true
    }
  }

  private def unwrap(s: LocalTimeSchedule): Seq[(LocalTime, LocalTime)] =
    s.intervals.map(i => (i.start, i.finish))

  private def beHours(expectedHours: (Int, Int)*): Matcher[LocalTimeSchedule] = new Matcher[LocalTimeSchedule] {

    override def apply(left: LocalTimeSchedule): MatchResult = {
      val expectedIntervals = expectedHours.map {
        case (startHour, finishHour) =>
          val start = LocalTime.of(startHour, 0)
          val rawFinish = LocalTime.of(finishHour, 0)
          // Compensate for the quirk of the implementation that "swallows" 00:00 at the end of an interval.
          val finish = if (rawFinish == LocalTime.MIN) LocalTime.MAX else rawFinish
          (start, finish)
      }

      be(expectedIntervals).apply(unwrap(left))
    }

  }
}
