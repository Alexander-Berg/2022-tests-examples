package auto.dealers.dealer_stats.logic

import auto.common.clients.statistics.testkit.FinstatClientTest
import auto.dealers.dealer_stats.model.{ClientId, DailyActivations, DailyProductActivations, OfferId}
import billing.finstat.api_model.{GetSpendingsResponse, Spending}
import common.clients.statist.testkit.StatistClientMock
import common.zio.logging.Logging
import ru.auto.api.api_offer_model.{Category, Section}
import ru.yandex.vertis.statist.model.api.api_model.{MultipleDailyValues, ObjectDailyValues, ObjectDayValues}
import zio.ZIO
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation.value

import java.time.LocalDate

object FinStatisticsManagerLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("FinStatisticsManagerLive")(
      testM("get getWeekAverageOutcome should calculate outcome") {
        val to = LocalDate.parse("2021-12-31")
        val clientId = ClientId(1L)

        val statistClientMock =
          StatistClientMock.empty

        val finstatClient =
          FinstatClientTest.GetSpendings(
            anything,
            value(
              GetSpendingsResponse.of(
                Seq(
                  Spending.of("2021-11-27", groupedBy = Map.empty, spentKopecks = 1000, count = 3),
                  Spending.of("2021-11-28", groupedBy = Map.empty, spentKopecks = 2000, count = 3),
                  Spending.of("2021-11-29", groupedBy = Map.empty, spentKopecks = 3000, count = 3),
                  Spending.of("2021-11-30", groupedBy = Map.empty, spentKopecks = 4000, count = 3),
                  Spending.of("2021-12-01", groupedBy = Map.empty, spentKopecks = 5000, count = 3),
                  Spending.of("2021-12-03", groupedBy = Map.empty, spentKopecks = 6000, count = 3),
                  Spending.of("2021-12-13", groupedBy = Map.empty, spentKopecks = 7000, count = 3),
                  Spending.of("2021-12-02", groupedBy = Map.empty, spentKopecks = 8000, count = 3),
                  Spending.of("2021-12-05", groupedBy = Map.empty, spentKopecks = 9000, count = 3),
                  Spending.of("2021-12-06", groupedBy = Map.empty, spentKopecks = 10000, count = 3),
                  Spending.of("2021-12-07", groupedBy = Map.empty, spentKopecks = 21000, count = 3),
                  Spending.of("2021-12-08", groupedBy = Map.empty, spentKopecks = 22000, count = 3),
                  Spending.of("2021-12-09", groupedBy = Map.empty, spentKopecks = 23000, count = 3),
                  Spending.of("2021-12-10", groupedBy = Map.empty, spentKopecks = 24000, count = 3),
                  Spending.of("2021-12-14", groupedBy = Map.empty, spentKopecks = 25000, count = 3),
                  Spending.of("2021-12-15", groupedBy = Map.empty, spentKopecks = 26000, count = 3),
                  Spending.of("2021-12-17", groupedBy = Map.empty, spentKopecks = 27000, count = 3),
                  Spending.of("2021-12-18", groupedBy = Map.empty, spentKopecks = 28000, count = 3),
                  Spending.of("2021-12-20", groupedBy = Map.empty, spentKopecks = 29000, count = 3),
                  Spending.of("2021-12-21", groupedBy = Map.empty, spentKopecks = 30000, count = 3),
                  Spending.of("2021-12-23", groupedBy = Map.empty, spentKopecks = 31000, count = 3),
                  Spending.of("2021-12-24", groupedBy = Map.empty, spentKopecks = 32000, count = 3),
                  Spending.of("2021-12-26", groupedBy = Map.empty, spentKopecks = 33000, count = 3),
                  Spending.of("2021-12-04", groupedBy = Map.empty, spentKopecks = 34000, count = 3),
                  Spending.of("2021-12-19", groupedBy = Map.empty, spentKopecks = 35000, count = 3),
                  Spending.of("2021-12-12", groupedBy = Map.empty, spentKopecks = 36000, count = 3),
                  Spending.of("2021-12-22", groupedBy = Map.empty, spentKopecks = 37000, count = 3),
                  Spending.of("2021-12-16", groupedBy = Map.empty, spentKopecks = 38000, count = 3),
                  Spending.of("2021-12-11", groupedBy = Map.empty, spentKopecks = 39000, count = 3),
                  Spending.of("2021-12-27", groupedBy = Map.empty, spentKopecks = 40000, count = 3),
                  Spending.of("2021-12-25", groupedBy = Map.empty, spentKopecks = 41000, count = 3)
                )
              )
            )
          )

        val finStatisticsManager =
          (finstatClient.toLayer ++ statistClientMock ++ Logging.live) >>> FinStatisticsManagerLive.live

        (for {
          outcome <- FinStatisticsManager(_.getWeekAverageOutcome(clientId, to))
        } yield assertTrue(outcome == 26500L)).provideLayer(finStatisticsManager)
      },
      testM("get getActivationsDaily should calculate daily activations") {
        val from = LocalDate.parse("2021-12-01")
        val to = LocalDate.parse("2021-12-30")
        val clientId = ClientId(1L)

        val statistClientMock =
          StatistClientMock.empty

        val finstatClient =
          FinstatClientTest.GetSpendings(
            anything,
            value(
              GetSpendingsResponse.of(
                Seq(
                  Spending.of("2021-12-01", groupedBy = Map.empty, spentKopecks = 10000, count = 3),
                  Spending.of("2021-12-22", groupedBy = Map.empty, spentKopecks = 50000, count = 7)
                )
              )
            )
          )

        val finStatisticsManager =
          (finstatClient.toLayer ++ statistClientMock ++ Logging.live) >>> FinStatisticsManagerLive.live

        val dailyActivationsResponse =
          Seq(
            DailyActivations(day = "2021-12-22", amountRubles = 500),
            DailyActivations(day = "2021-12-01", amountRubles = 100)
          )

        (for {
          response <- FinStatisticsManager(_.getActivationsDaily(clientId, from, to))
        } yield assertTrue(dailyActivationsResponse == response)).provideLayer(finStatisticsManager)
      },
      testM("get getTotalActivations should calculate total activations") {
        val from = LocalDate.parse("2021-12-01")
        val to = LocalDate.parse("2021-12-30")
        val clientId = ClientId(1L)

        val statistClientMock =
          StatistClientMock.empty

        val finstatClient =
          FinstatClientTest.GetSpendings(
            anything,
            value(
              GetSpendingsResponse.of(
                Seq(
                  Spending.of("2021-12-01", groupedBy = Map("product" -> "badge"), spentKopecks = 10000, count = 3),
                  Spending.of("2021-12-01", groupedBy = Map("product" -> "placement"), spentKopecks = 20000, count = 2),
                  Spending.of("2021-12-22", groupedBy = Map("product" -> "badge"), spentKopecks = 50000, count = 7)
                )
              )
            )
          )

        val finStatisticsManager =
          (finstatClient.toLayer ++ statistClientMock ++ Logging.live) >>> FinStatisticsManagerLive.live

        (for {
          outcome <- FinStatisticsManager(_.getTotalActivations(clientId, Category.CARS, Section.USED, from, to))
        } yield assertTrue(80000L == outcome)).provideLayer(finStatisticsManager)
      },
      testM("getLastNotZeroMedianFromWeekOfMedians should be correct") {
        val spendings = Seq(
          Spending.of("2021-11-27", groupedBy = Map.empty, spentKopecks = 1000, count = 3),
          Spending.of("2021-11-28", groupedBy = Map.empty, spentKopecks = 2000, count = 3),
          Spending.of("2021-11-29", groupedBy = Map.empty, spentKopecks = 3000, count = 3),
          Spending.of("2021-11-30", groupedBy = Map.empty, spentKopecks = 4000, count = 3),
          Spending.of("2021-12-01", groupedBy = Map.empty, spentKopecks = 5000, count = 3),
          Spending.of("2021-12-02", groupedBy = Map.empty, spentKopecks = 8000, count = 3),
          Spending.of("2021-12-03", groupedBy = Map.empty, spentKopecks = 6000, count = 3),
          Spending.of("2021-12-04", groupedBy = Map.empty, spentKopecks = 7000, count = 3),
          Spending.of("2021-12-05", groupedBy = Map.empty, spentKopecks = 8000, count = 3),
          Spending.of("2021-12-06", groupedBy = Map.empty, spentKopecks = 9000, count = 3),
          Spending.of("2021-12-07", groupedBy = Map.empty, spentKopecks = 10000, count = 3),
          Spending.of("2021-12-08", groupedBy = Map.empty, spentKopecks = 11000, count = 3),
          Spending.of("2021-12-09", groupedBy = Map.empty, spentKopecks = 12000, count = 3),
          Spending.of("2021-12-10", groupedBy = Map.empty, spentKopecks = 13000, count = 3),
          Spending.of("2021-12-11", groupedBy = Map.empty, spentKopecks = 14000, count = 3),
          Spending.of("2021-12-27", groupedBy = Map.empty, spentKopecks = 30000, count = 3),
          Spending.of("2021-12-13", groupedBy = Map.empty, spentKopecks = 16000, count = 3),
          Spending.of("2021-12-14", groupedBy = Map.empty, spentKopecks = 17000, count = 3),
          Spending.of("2021-12-15", groupedBy = Map.empty, spentKopecks = 18000, count = 3),
          Spending.of("2021-12-16", groupedBy = Map.empty, spentKopecks = 19000, count = 3),
          Spending.of("2021-12-17", groupedBy = Map.empty, spentKopecks = 20000, count = 3),
          Spending.of("2021-12-18", groupedBy = Map.empty, spentKopecks = 21000, count = 3),
          Spending.of("2021-12-19", groupedBy = Map.empty, spentKopecks = 22000, count = 3),
          Spending.of("2021-12-20", groupedBy = Map.empty, spentKopecks = 23000, count = 3),
          Spending.of("2021-12-21", groupedBy = Map.empty, spentKopecks = 24000, count = 3),
          Spending.of("2021-12-22", groupedBy = Map.empty, spentKopecks = 25000, count = 3),
          Spending.of("2021-12-23", groupedBy = Map.empty, spentKopecks = 26000, count = 3),
          Spending.of("2021-12-24", groupedBy = Map.empty, spentKopecks = 27000, count = 3),
          Spending.of("2021-12-25", groupedBy = Map.empty, spentKopecks = 28000, count = 3),
          Spending.of("2021-12-26", groupedBy = Map.empty, spentKopecks = 29000, count = 3),
          Spending.of("2021-12-12", groupedBy = Map.empty, spentKopecks = 15000, count = 3)
        )
        assertM(ZIO.succeed(FinStatisticsManagerLive.getLastNotZeroMedianFromWeekOfMedians(spendings)))(equalTo(15500L))
      },
      testM("getTotalProductsActivationsByOffer should calculate total product activations") {
        val from = LocalDate.parse("2022-03-01")
        val to = LocalDate.parse("2022-03-03")
        val clientId = ClientId(1L)
        val offerId = OfferId("123-abc")

        val statistClientMock =
          StatistClientMock.GetCounterMultiComponentValuesByDay(
            Assertion.anything,
            value(
              MultipleDailyValues.of(
                Map(
                  "123-abc" -> ObjectDailyValues.of(
                    Seq(
                      ObjectDayValues.of("2022-03-01", Map("special_click" -> 2, "special_show" -> 3)),
                      ObjectDayValues.of("2022-03-02", Map("special_click" -> 0, "special_show" -> 0)),
                      ObjectDayValues.of("2022-03-03", Map("special_click" -> 2, "special_show" -> 6))
                    )
                  )
                )
              )
            )
          )

        val finstatClient =
          FinstatClientTest.GetSpendings(
            Assertion.anything,
            value(
              GetSpendingsResponse.of(
                Seq(
                  Spending
                    .of("2022-03-01", groupedBy = Map("product" -> "special-offer"), spentKopecks = 10000, count = 1),
                  Spending.of("2022-03-01", groupedBy = Map("product" -> "placement"), spentKopecks = 50000, count = 1),
                  Spending.of("2022-03-02", groupedBy = Map("product" -> "placement"), spentKopecks = 40000, count = 1),
                  Spending
                    .of("2022-03-02", groupedBy = Map("product" -> "special-offer"), spentKopecks = 20000, count = 1),
                  Spending.of("2022-03-03", groupedBy = Map("product" -> "placement"), spentKopecks = 30000, count = 1),
                  Spending.of("2022-03-03", groupedBy = Map("product" -> "reset"), spentKopecks = 20000, count = 1)
                )
              )
            )
          )

        val finStatisticsManager =
          (finstatClient.toLayer ++ statistClientMock.toLayer ++ Logging.live) >>> FinStatisticsManagerLive.live

        val dailyActivationsResponse =
          Seq(
            DailyProductActivations(
              day = "2022-03-01",
              specialShows = 3,
              specialClicks = 2,
              placement = 50000,
              specialOffer = 10000
            ),
            DailyProductActivations(
              day = "2022-03-02",
              specialShows = 0,
              specialClicks = 0,
              placement = 40000,
              specialOffer = 20000
            ),
            DailyProductActivations(
              day = "2022-03-03",
              specialShows = 0,
              specialClicks = 0,
              placement = 30000,
              reset = 20000
            )
          )

        (for {
          response <- FinStatisticsManager(_.getTotalProductsActivationsByOffer(clientId, offerId, from, to))
        } yield assertTrue(dailyActivationsResponse == response)).provideLayer(finStatisticsManager)
      }
    )
  }
}
