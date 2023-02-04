package ru.yandex.vertis.punisher.util

import java.time.ZonedDateTime

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class DateTimeUtilsSpec extends BaseSpec {

  import DateTimeUtils._

  "DateTimeUtils" should {
    "provide correct TimeInterval" in {
      val from = now
      TimeInterval(from, from.plusMinutes(1)) match {
        case TimeInterval(`from`, to) if to == from.plusMinutes(1) => info("Done")
        case other                                                 => fail(s"Unexpected $other")
      }
      TimeInterval(from.minusYears(20), from) match {
        case TimeInterval(fr, `from`) if fr == from.minusYears(20) => info("Done")
        case other                                                 => fail(s"Unexpected $other")
      }
      intercept[IllegalArgumentException] {
        TimeInterval(from, from)
      }
      intercept[IllegalArgumentException] {
        TimeInterval(from, from.minusSeconds(1))
      }
      intercept[IllegalArgumentException] {
        TimeInterval(from, from.minusMonths(5))
      }
    }
    "provide correct TimeInterval if construct if by period, offset" in {
      val to = now
      TimeInterval(to, 1.second) match {
        case TimeInterval(fr, `to`) if fr == to.minusSeconds(1) => info("Done")
        case other                                              => fail(s"Unexpected $other")
      }
      TimeInterval(to, 10.days) match {
        case TimeInterval(fr, `to`) if fr == to.minusDays(10) => info("Done")
        case other                                            => fail(s"Unexpected $other")
      }
      TimeInterval(to, 10.days, Some(10.days)) match {
        case TimeInterval(fr, tto) if fr == to.minusDays(20) && tto == to.minusDays(10) =>
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
      TimeInterval(to, 2.hours, Some(24.hours)) match {
        case TimeInterval(fr, tto) if fr == to.minusHours(24).minusHours(2) && tto == to.minusHours(24) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "provide correct TimeInterval for specific dates" in {
      val now = ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]")
      val ti = TimeInterval(now, 2.hours, Some(24.hours))
      ti.from.toString should be("2017-10-01T13:34:17.120+03:00[Europe/Moscow]")
      ti.to.toString should be("2017-10-01T15:34:17.120+03:00[Europe/Moscow]")
    }
  }
}
