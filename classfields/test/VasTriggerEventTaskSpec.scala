package auto.dealers.amoyak.tasks

import auto.common.clients.cabinet.testkit.CabinetTest
import auto.common.clients.statistics.testkit.{DealerStatsClientTest, FinstatClientTest}
import auto.dealers.amoyak.model.triggers.{TriggerEvent, TriggerEventType}
import auto.dealers.amoyak.storage.testkit.dao.TriggerEventsDaoMock
import auto.dealers.amoyak.tasks.VasTriggerEventsTask.vasHumanNames
import billing.finstat.api_model.{GetSpendingsRequest, GetSpendingsResponse, Spending}
import ru.auto.cabinet.api_model.{ClientProperties, DetailedClient, FindClientsResponse}
import ru.auto.dealer_stats.proto.rpc.{
  DealerProductActivationsDailyStat,
  GetProductActivationsDailyStatsRequest,
  GetProductActivationsDailyStatsResponse
}
import zio.test.{DefaultRunnableSpec, _}
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock.MockClock

import java.time.{LocalDate, OffsetDateTime, OffsetTime}

object VasTriggerEventTaskSpec extends DefaultRunnableSpec {
  val today: LocalDate = LocalDate.of(2021, 2, 15)
  val now: OffsetDateTime = today.atTime(OffsetTime.MIN)
  val twoWeeksBefore = "2021-02-01"

  val clientId = 123L

  val dealerActivationsDailyRequest = GetProductActivationsDailyStatsRequest.defaultInstance
    .withFrom(twoWeeksBefore)
    .withTo(today.toString)
    .withClientId(clientId)
    .withProducts(Seq("premium", "turbo-package", "boost", "special-offer", "badge", "reset"))

  val cabinet = CabinetTest.InternalSearchClients(
    anything,
    value(
      FindClientsResponse.defaultInstance.addClients(FindClientsResponse.ClientDto.defaultInstance.withId(clientId))
    )
  ) ++ CabinetTest.GetDetailedClient(
    equalTo(clientId),
    value(DetailedClient.defaultInstance.withProperties(ClientProperties.defaultInstance.withRegionId(123L)))
  )

  val clock = MockClock.CurrentDateTime(value(now))

  val task = new VasTriggerEventsTask()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("VasTriggerEventTaskSpec")(
      testM("do nothing if no transactions exist") {
        val dealerStats = DealerStatsClientTest.GetProductActivationsDailyStats(
          equalTo(dealerActivationsDailyRequest),
          value(GetProductActivationsDailyStatsResponse.defaultInstance)
        )

        assertM(task.program)(isUnit)
          .provideCustomLayer(TriggerEventsDaoMock.empty ++ clock ++ cabinet ++ dealerStats)
      },
      testM("do nothing if diff is less then allowed") {
        val getProductActivationsDailyStatsResponse = GetProductActivationsDailyStatsResponse.defaultInstance
          .addActivationStats(
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-01")
              .withSpentKopecks(10000)
              .withProduct("badge"),
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-08")
              .withSpentKopecks(9000)
              .withProduct("badge")
          )

        val dealerStats = DealerStatsClientTest.GetProductActivationsDailyStats(
          equalTo(dealerActivationsDailyRequest),
          value(getProductActivationsDailyStatsResponse)
        )

        assertM(task.program)(isUnit)
          .provideCustomLayer(TriggerEventsDaoMock.empty ++ clock ++ cabinet ++ dealerStats)
      },
      testM("do nothing if first week has no spendings") {
        val getProductActivationsDailyStatsResponse = GetProductActivationsDailyStatsResponse.defaultInstance
          .addActivationStats(
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-08")
              .withSpentKopecks(9000)
              .withProduct("badge")
          )

        val dealerStats = DealerStatsClientTest.GetProductActivationsDailyStats(
          equalTo(dealerActivationsDailyRequest),
          value(getProductActivationsDailyStatsResponse)
        )

        assertM(task.program)(isUnit)
          .provideCustomLayer(TriggerEventsDaoMock.empty ++ clock ++ cabinet ++ dealerStats)
      },
      testM("create event if vas spendings decreased") {
        val getProductActivationsDailyStatsResponse = GetProductActivationsDailyStatsResponse.defaultInstance
          .addActivationStats(
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-01")
              .withSpentKopecks(10000)
              .withProduct("badge"),
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-01")
              .withSpentKopecks(9)
              .withProduct("premium"),
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-08")
              .withSpentKopecks(5000)
              .withProduct("badge"),
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-08")
              .withSpentKopecks(10)
              .withProduct("premium"),
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-09")
              .withSpentKopecks(1000)
              .withProduct("badge"),
            DealerProductActivationsDailyStat.defaultInstance
              .withDate("2021-02-10")
              .withSpentKopecks(1000)
              .withProduct("something")
          )

        val dealerStats = DealerStatsClientTest.GetProductActivationsDailyStats(
          equalTo(dealerActivationsDailyRequest),
          value(getProductActivationsDailyStatsResponse)
        )

        val triggerEvent = TriggerEvent(
          eventType = TriggerEventType.VasVolumeDecreased,
          clientId = clientId,
          updatedAt = now.toInstant,
          title = s"Упал объем VAS на 30%",
          note = Some(
            """
            |Сравниваемые периоды: неделя с 2021-02-01 по 2021-02-07 и неделя с 2021-02-08 по 2021-02-14
            |Сократился объем VAS на 30% с 10009р до 7010р за последнюю неделю
            |Премиум объявление: увеличилось с 9р до 10р
            |Стикеры: сократилось с 10000р до 6000р""".stripMargin
          )
        )

        val triggerEventsDao = TriggerEventsDaoMock.Upsert(equalTo(Seq(triggerEvent)), unit)

        assertM(task.program)(isUnit)
          .provideCustomLayer(clock ++ cabinet ++ dealerStats ++ triggerEventsDao)
      }
    )
}
