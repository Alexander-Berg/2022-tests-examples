package ru.yandex.vertis.scheduler.model

import org.joda.time.{LocalTime, Period}
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.scheduler._

/**
 * Specs on [[ru.yandex.vertis.scheduler.SchedulerImpl]] behaviour.
 *
 * @author dimas
 */
class ScheduleSpec
  extends Matchers
  with WordSpecLike {

  "EverySeconds schedule" should {
    val s = Schedule.EverySeconds(10)
    val systemTime = now()

    "run" in {
      s.shouldRun(systemTime, None) should be(true)

      s.shouldRun(systemTime,
        Some(systemTime.minusSeconds(10).minusMillis(10))) should be(true)

      s.shouldRun(systemTime,
        Some(systemTime.minusHours(1))) should be(true)

      s.shouldRun(systemTime,
        Some(systemTime.minusDays(1))) should be(true)
    }

    "not run" in {
      s.shouldRun(systemTime, Some(systemTime.minusSeconds(10))) should be(false)
      s.shouldRun(systemTime, Some(systemTime.minusSeconds(9))) should be(false)
    }
  }

  "EveryMinutes schedule" should {
    val s = Schedule.EveryMinutes(10)
    val systemTime = now()

    "run" in {
      s.shouldRun(systemTime, None) should be(true)
      s.shouldRun(systemTime,
        Some(systemTime.minusMinutes(10).minusMillis(1))) should be(true)

      s.shouldRun(systemTime,
        Some(systemTime.minusHours(1))) should be(true)

      s.shouldRun(systemTime,
        Some(systemTime.minusDays(1))) should be(true)
    }

    "not run" in {
      s.shouldRun(systemTime, Some(systemTime.minusMinutes(10))) should be(false)
      s.shouldRun(systemTime, Some(systemTime.minusMinutes(1))) should be(false)
    }
  }

  "AtFixedHourOfDay schedule" should {
    val s = Schedule.AtFixedHourOfDay(1)
    val systemTime = now().
      withHourOfDay(1).
      withMinuteOfHour(0)

    "run" in {
      s.shouldRun(systemTime, None) should be(true)

      s.shouldRun(systemTime.plusMinutes(20),
        Some(systemTime.minusMinutes(10).minusDays(1))) should be(true)
    }

    "not run" in {
      s.shouldRun(systemTime.plusHours(1),
        None) should be(false)

      s.shouldRun(systemTime.plusMinutes(20),
        Some(systemTime.minusMinutes(10))) should be(false)

      s.shouldRun(systemTime.plusHours(1),
        Some(systemTime.minusMinutes(10))) should be(false)
    }
  }

  "AtFixedTime schedule" should {
    val s = Schedule.AtFixedTime(
      new LocalTime(13, 37), Period.minutes(5))

    val systemTime = now()
      .withMillisOfDay(0)
      .withHourOfDay(13)
      .withMinuteOfHour(42)
      .minusSeconds(1)

    "run" in {
      s.shouldRun(systemTime, None) should be(true)

      s.shouldRun(systemTime.minusMinutes(10).plusSeconds(2),
        Some(systemTime.minusDays(1))) should be(true)

      s.shouldRun(systemTime.minusMinutes(5),
        Some(systemTime.minusDays(1))) should be(true)
    }

    "not run" in {
      s.shouldRun(systemTime.minusMinutes(10).plusSeconds(1),
        Some(systemTime.minusDays(1))) should be(false)

      s.shouldRun(systemTime.plusSeconds(1), None) should be(false)

      s.shouldRun(systemTime.plusHours(1), None) should be(false)

      s.shouldRun(systemTime, Some(systemTime.minusHours(2))) should be(false)

      s.shouldRun(systemTime.plusSeconds(1),
        Some(systemTime.minusDays(1))) should be(false)
    }
  }
}
