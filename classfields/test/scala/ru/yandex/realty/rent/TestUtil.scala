package ru.yandex.realty.rent

import org.joda.time.{DateTime, LocalDate}

import scala.language.implicitConversions
import scala.util.matching.Regex

object TestUtil {
  implicit def any2option[T](v: T): Option[T] = Option(v)

  def dt(y: Int, m: Int, d: Int): DateTime =
    new LocalDate(y, m, d).toDateTimeAtStartOfDay

  private val dataRegexp: Regex = raw"(\d{1,4})-(\d{1,2})-(\d{1,2})".r
  private val dataTimeHMRegexp: Regex = raw"(\d{1,4})-(\d{1,2})-(\d{1,2})(?:T|\s+)(\d{1,2}):(\d{1,2})".r
  private val dataTimeHMSRegexp: Regex = raw"(\d{1,4})-(\d{1,2})-(\d{1,2})(?:T|\s+)(\d{1,2}):(\d{1,2}):(\d{1,2})".r

  def dt(str: String): DateTime = {
    str match {
      case dataRegexp(y, m, d) => dt(y.toInt, m.toInt, d.toInt)
      case dataTimeHMRegexp(y, m, d, hour, min) =>
        dt(y.toInt, m.toInt, d.toInt).withTime(hour.toInt, min.toInt, 0, 0)
      case dataTimeHMSRegexp(y, m, d, hour, min, sec) =>
        dt(y.toInt, m.toInt, d.toInt).withTime(hour.toInt, min.toInt, sec.toInt, 0)
      case _ => throw new IllegalArgumentException(s"invalid date format [$str]")
    }
  }

  implicit class DateTimeHelper(val sc: StringContext) extends AnyVal {
    def dt(args: Any*): DateTime = TestUtil.dt(sc.standardInterpolator(identity, args))
  }
}
