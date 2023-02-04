package ru.yandex.vertis.moisha.model

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.moisha.environment._

/**
  * Specs on [[DateTimeInterval]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class DateTimeIntervalSpec extends Matchers with WordSpecLike {

  val time: DateTime = DateTime.parse("2016-07-12T18:36:25.123+03:00")

  "DateTimeInterval.isDailyInterval" should {
    import DateTimeInterval.isDailyInterval

    "return true" in {
      isDailyInterval(
        DateTimeInterval(
          time.withTimeAtStartOfDay(),
          time.withTimeAtStartOfDay().plusDays(1).minus(1)
        )
      ) should be(true)
      isDailyInterval(
        DateTimeInterval(
          time.withTimeAtStartOfDay(),
          time.withTimeAtStartOfDay().plusDays(3).minus(1)
        )
      ) should be(true)
    }
    "return false" in {
      isDailyInterval(
        DateTimeInterval(
          time.withTimeAtStartOfDay(),
          time
        )
      ) should be(false)
      isDailyInterval(
        DateTimeInterval(
          time,
          time.withTimeAtStartOfDay().plusDays(2).minus(1)
        )
      ) should be(false)
    }
  }

  "DateTimeInterval.asDailyIntervals" should {
    import DateTimeInterval.asDailyIntervals
    "correctly split interval into days" in {
      val oneDay = asDailyIntervals(DateTimeInterval(time.withTimeAtStartOfDay(), time)).toSeq
      oneDay.size should be(1)
      oneDay.head should be(wholeDay(time))

      val threeDays = asDailyIntervals(
        DateTimeInterval(
          time,
          time.plusDays(2)
        )
      ).toSeq
      threeDays.size should be(3)
      threeDays.head should be(wholeDay(time))
      threeDays(1) should be(wholeDay(time.plusDays(1)))
      threeDays(2) should be(wholeDay(time.plusDays(2)))
    }

  }
}
