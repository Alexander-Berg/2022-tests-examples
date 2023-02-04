package ru.yandex.vos2.autoru.utils.recommendation

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.utils.recommendation.OfferStatisticsCalculator.DayAndCount

import java.time.LocalDate

@RunWith(classOf[JUnitRunner])
class OfferStatisticsCalculatorTest extends AnyWordSpec with Matchers with OptionValues {
  "OfferStatisticsCalculator" should {
    "return average values" in {
      val eventCountByDay = Seq(
        DayAndCount(LocalDate.parse("2021-01-01"), 12),
        DayAndCount(LocalDate.parse("2021-01-02"), 12),
        DayAndCount(LocalDate.parse("2021-01-03"), 12),
        DayAndCount(LocalDate.parse("2021-01-04"), 12)
      )
      val offerCountByDay = Seq(
        DayAndCount(LocalDate.parse("2021-01-01"), 2),
        DayAndCount(LocalDate.parse("2021-01-02"), 3),
        DayAndCount(LocalDate.parse("2021-01-03"), 4),
        DayAndCount(LocalDate.parse("2021-01-04"), 5)
      )
      val expectedResult = Seq(
        DayAndCount(LocalDate.parse("2021-01-01"), 6),
        DayAndCount(LocalDate.parse("2021-01-02"), 4),
        DayAndCount(LocalDate.parse("2021-01-03"), 3),
        DayAndCount(LocalDate.parse("2021-01-04"), 2)
      )
      val result = OfferStatisticsCalculator.computeAveragePerOffer(eventCountByDay, offerCountByDay)
      result shouldBe expectedResult
    }

    "only handle days present in both eventCounts and offerCounts" in {
      val eventCountByDay = Seq(
        DayAndCount(LocalDate.parse("2021-01-01"), 12),
        DayAndCount(LocalDate.parse("2021-01-02"), 12),
        DayAndCount(LocalDate.parse("2021-01-03"), 12),
        DayAndCount(LocalDate.parse("2021-01-04"), 12)
      )
      val offerCountByDay = Seq(
        DayAndCount(LocalDate.parse("2021-01-02"), 2),
        DayAndCount(LocalDate.parse("2021-01-04"), 3),
        DayAndCount(LocalDate.parse("2021-01-06"), 4),
        DayAndCount(LocalDate.parse("2021-01-08"), 5)
      )
      val expectedResult = Seq(
        DayAndCount(LocalDate.parse("2021-01-02"), 6),
        DayAndCount(LocalDate.parse("2021-01-04"), 4)
      )
      val result = OfferStatisticsCalculator.computeAveragePerOffer(eventCountByDay, offerCountByDay)
      result shouldBe expectedResult
    }

    "ignore days with zero offer count" in {
      val eventCountByDay = Seq(
        DayAndCount(LocalDate.parse("2021-01-01"), 12),
        DayAndCount(LocalDate.parse("2021-01-02"), 12),
        DayAndCount(LocalDate.parse("2021-01-03"), 0),
        DayAndCount(LocalDate.parse("2021-01-04"), 0),
        DayAndCount(LocalDate.parse("2021-01-05"), 12),
        DayAndCount(LocalDate.parse("2021-01-06"), 12)
      )
      val offerCountByDay = Seq(
        DayAndCount(LocalDate.parse("2021-01-01"), 2),
        DayAndCount(LocalDate.parse("2021-01-02"), 0),
        DayAndCount(LocalDate.parse("2021-01-03"), 4),
        DayAndCount(LocalDate.parse("2021-01-04"), 0),
        DayAndCount(LocalDate.parse("2021-01-05"), 3),
        DayAndCount(LocalDate.parse("2021-01-06"), 0)
      )
      val expectedResult = Seq(
        DayAndCount(LocalDate.parse("2021-01-01"), 6),
        DayAndCount(LocalDate.parse("2021-01-03"), 0),
        DayAndCount(LocalDate.parse("2021-01-05"), 4)
      )
      val result = OfferStatisticsCalculator.computeAveragePerOffer(eventCountByDay, offerCountByDay)
      result shouldBe expectedResult
    }
  }
}
