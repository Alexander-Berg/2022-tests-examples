package ru.yandex.vertis.clustering.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class DateTimeUtilsSpec extends BaseSpec {

  import DateTimeUtils._

  "DateTimeUtils" should {

    val start = DateTimeUtils.now
    Thread.sleep(500L)
    val finish = DateTimeUtils.now

    "RichZonedDateTime Between" in {
      val b = start.between(finish)
      b >= 500.millisecond && b < 1.seconds shouldBe true
      start.betweenNow >= 500.millisecond shouldBe true
    }

    "zonedDateTimeOrdering" in {
      zonedDateTimeOrdering.max(start, finish).isEqual(finish) shouldBe true
      zonedDateTimeOrdering.min(start, finish).isEqual(start) shouldBe true
    }
  }

}
