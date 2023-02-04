package ru.yandex.vertis.promocoder.tasks

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.util.DateTimeInterval
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** @author ruslansd
  */
class FeatureProlongationHelperSpec extends AnyWordSpecLike with Matchers with ModelGenerators {

  private val helper = new FeatureProlongationHelper()

  "FeatureProlongationHelper" should {
    "get correctly start time for current day" in {
      val startDay = DateTimeUtil.now().withTimeAtStartOfDay()
      helper.getStartTime(
        startDay,
        1.day,
        DateTimeUtil.now()
      ) shouldBe startDay
    }

    "get correctly start time" in {
      val times = dateTimeInPast.next(10)
      times.foreach { t =>
        val now = DateTimeUtil.now()
        val nexTime = helper.getStartTime(
          t,
          1.day,
          DateTimeUtil.now()
        )
        interval(nexTime, 1.day).contains(now) shouldBe true
      }
    }
  }

  private def interval(time: DateTime, period: FiniteDuration) =
    DateTimeInterval(time, time.plusSeconds(period.toSeconds.toInt))
}
