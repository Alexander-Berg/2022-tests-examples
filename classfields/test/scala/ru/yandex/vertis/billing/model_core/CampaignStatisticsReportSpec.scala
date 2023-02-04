package ru.yandex.vertis.billing.model_core

import org.joda.time.DateTime
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.model_core.gens.{OrderGen, Producer}
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.now

/**
  * Specs on [[CampaignStatisticsReport]]
  *
  * @author alesavin
  */
class CampaignStatisticsReportSpec extends AnyWordSpec with Matchers {

  "CampaignStatisticsReport" should {
    val Now = now()

    val order = OrderGen.next

    val statistics = Iterable(
      getStatistic(Now.withTimeAtStartOfDay(), 1L),
      getStatistic(Now.withHourOfDay(1), 2L),
      getStatistic(Now.withHourOfDay(2), 3L)
    )

    "calculate correct total" in {
      val averagePosition = StatisticUtils.average(1 * 1 + 2 * 2 + 3 * 3, 6)
      CampaignStatisticsReport("test", order, statistics).total match {
        case StatisticPoint(_, 6, 6, 6, 6, `averagePosition`, 6) => info("Done")
        case other => fail(s"Unexpected $other")
      }

      val averagePosition2 = StatisticUtils.average(2 * 2 + 3 * 3, 5)
      CampaignStatisticsReport("test2", order, statistics.tail).total match {
        case StatisticPoint(_, 5, 5, 5, 5, `averagePosition2`, 5) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "calculate correct total of empty statistics" in {
      CampaignStatisticsReport("test", order, Iterable.empty).total match {
        case StatisticPoint(_, 0, 0, 0, 0, 0, 0) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
  }

  def getStatistic(date: DateTime, count: Long) =
    StatisticPoint(DateTimeInterval.hourIntervalFrom(date), count, count, count, count, count, count)
}
