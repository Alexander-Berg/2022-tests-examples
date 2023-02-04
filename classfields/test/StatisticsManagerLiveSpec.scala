package auto.common.manager.statistics.test

import auto.common.clients.statistics.testkit.DealerStatsClientTest
import auto.common.manager.statistics.model.Expense
import auto.common.manager.statistics.{StatisticsManager, StatisticsManagerLive}
import ru.auto.dealer_stats.proto.rpc.DealerActivationsDailyResponse.DayActivations
import ru.auto.dealer_stats.proto.rpc.{DealerActivationsDailyResponse, DealerWeekAverageOutcomeResponse}
import zio.test.Assertion._
import zio.test.mock.Expectation.value
import zio.test.{DefaultRunnableSpec, ZSpec, _}

import java.time.{OffsetDateTime, ZoneOffset}

object StatisticsManagerLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("StatisticsManagerLive")(
      testM("getActivationsDaily should pass proper arguments to the dealer-stats client") {
        val clientId = 20101
        val datetime: OffsetDateTime =
          OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0))
        val amountKopeck = 10000
        val amountRubles = amountKopeck / 100.0

        val mockedFinstatResponse = DealerActivationsDailyResponse.of(
          Seq(
            createDealerResponse("2019-12-31", amountRubles),
            createDealerResponse("2020-01-01", amountRubles)
          )
        )

        val dealerStatsMock = DealerStatsClientTest.GetActivationsDaily(anything, value(mockedFinstatResponse)).toLayer

        val statisticsManager =
          dealerStatsMock >>> StatisticsManagerLive.live

        (for {
          expenses <- StatisticsManager.getActivationsDaily(clientId, datetime.toZonedDateTime, 2)
        } yield assertTrue(
          expenses == Seq(
            Expense(datetime.minusDays(1).plusDays(1).minusSeconds(1), amountRubles),
            Expense(datetime.plusDays(1).minusSeconds(1), amountRubles)
          )
        ))
          .provideLayer(statisticsManager)
      },
      testM("getAverageWeekOutcome should pass proper arguments to the dealer-stat client") {
        val clientId = 20101
        val datetime: OffsetDateTime =
          OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0))
        val amountKopeck = 10000L

        val mockedDealerStatsResponse = DealerWeekAverageOutcomeResponse.of(
          outcome = amountKopeck + 2200
        )

        val finstatClient =
          DealerStatsClientTest.GetWeekAverageOutcome(anything, value(mockedDealerStatsResponse)).toLayer

        val statisticsManager =
          finstatClient >>> StatisticsManagerLive.live

        (for {
          outcome <- StatisticsManager.getAverageWeekOutcome(clientId, datetime.toZonedDateTime)
        } yield assertTrue(outcome == amountKopeck + 2200.0))
          .provideLayer(statisticsManager)
      }
    )
  }

  private def createDealerResponse(date: String, value: Double) = {
    DayActivations(date, value)
  }
}
