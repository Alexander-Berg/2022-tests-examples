package ru.yandex.vos2.autoru.utils

import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.TelephonyModel.PhoneCallsCounter
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.utils.converters.dailycounters.{DailyCountersFromTo, DailyCountersHelper}

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class DailyCountersHelperTest extends AnyFunSuite with Matchers {
  test("count calls by days") {
    val b = TestUtils.createOffer()
    val autoruBuilder: AutoruOffer.Builder = b.getOfferAutoruBuilder

    autoruBuilder.setPhoneCallsCounter(
      PhoneCallsCounter
        .newBuilder()
        .putAllDaily(
          Map[java.lang.Long, java.lang.Integer](
            (DateTime.parse("2020-04-10T12:10").getMillis, 1),
            (DateTime.parse("2020-04-11T12:20").getMillis, 2),
            (DateTime.parse("2020-04-17T12:30").getMillis, 4),
            (DateTime.parse("2020-04-18T12:40").getMillis, 8)
          ).asJava
        )
        .build()
    )

    val offer = b.build()
    val period = DailyCountersFromTo(Some(LocalDate.parse("2020-04-11")), Some(LocalDate.parse("2020-04-18")))

    val dailyCounters = DailyCountersHelper.countCallsByDay(offer, period)

    dailyCounters.size shouldBe 2
    assert(dailyCounters("2020-04-11") == 2)
    assert(dailyCounters("2020-04-17") == 4)
  }
}
