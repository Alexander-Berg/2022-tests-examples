package auto.dealers.dealer_stats.logic

import java.time.ZonedDateTime
import auto.common.clients.vos.testkit.VosTest
import common.clients.statist.testkit.StatistClientMock
import common.zio.clock.MoscowClock
import common.zio.logging.Logging
import ru.auto.api.api_offer_model.{Category, Offer}
import ru.auto.api.cars_model.CarInfo
import ru.auto.dealer_stats.proto.model.Day
import ru.yandex.vertis.statist.model.api.api_model.{MultipleDailyValues, ObjectDailyValues, ObjectDayValues}
import zio.ZLayer
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation.value

object StatisticsManagerLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("StatisticsManagerLive")(
      testM("getAverages should calculate average values") {
        val now = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, MoscowClock.timeZone)
        val from = now.minusWeeks(1)
        val until = now.plusWeeks(1)
        val offerId = "1"

        val statistClientMock =
          StatistClientMock.GetCounterPlainValuesByDay(
            anything,
            value(ObjectDailyValues(Seq(ObjectDayValues("2021-01-01", Map("card_view" -> 321)))))
          ) ++
            StatistClientMock.GetCounterPlainValuesByDay(
              anything,
              value(ObjectDailyValues(Seq(ObjectDayValues("2021-01-01", Map("phone_call" -> 456)))))
            ) ++
            StatistClientMock.GetCounterPlainValuesByDay(
              anything,
              value(
                ObjectDailyValues(
                  Seq(ObjectDayValues("2021-01-01", Map("cars" -> 123)), ObjectDayValues("2021-01-02", Map.empty))
                )
              )
            )

        val vosClientMock =
          VosTest.GetOfferOrFail(
            anything,
            value(Offer.defaultInstance.withCarInfo(CarInfo.defaultInstance.withMark("mark").withModel("model")))
          )

        val statisticsManager =
          (Logging.live ++ vosClientMock ++ statistClientMock) >>> StatisticsManager.live

        (for {
          days <- StatisticsManager.getAverages(offerId, Category.CARS, from.toLocalDate, until.toLocalDate)
        } yield assert(days)(equalTo(Seq(Day("2021-01-01", 2L, 3L))))).provideLayer(statisticsManager)
      }
    )
  }
}
