package auto.dealers.amoyak.logic.managers

import auto.common.clients.billing.model.{BalanceClientId, Order, OrderBalance, OrdersResponse}
import auto.common.clients.billing.testkit.BillingClientMock
import auto.common.clients.cabinet.testkit.CabinetTest
import auto.common.manager.statistics.testkit.StatisticsManagerMock
import auto.common.model.ClientId
import auto.dealers.amoyak.model.triggers.{TriggerEvent, TriggerEventType}
import auto.dealers.amoyak.storage.dao.OffersCountDao
import auto.dealers.amoyak.storage.testkit.dao.{OffersCountDaoMock, TriggerEventsDaoMock}
import common.zio.logging.Logging
import ru.auto.api.api_offer_model.{Category, Section}
import ru.auto.cabinet.api_model.ClientIdsResponse.ClientInfo
import zio.magic._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.mock.MockClock
import zio.test._
import zio.{Has, ZLayer}

import java.time.{LocalDate, ZoneId}

object OffersCountManagerLiveSpec extends DefaultRunnableSpec {

  private val today = LocalDate.of(2022, 7, 22)
  private val now = today.atStartOfDay(ZoneId.of("Europe/Moscow")).toOffsetDateTime

  private val clock = MockClock.CurrentDateTime(value(now))
  private val log = Logging.live

  override def spec: ZSpec[TestEnvironment, Any] = {

    suite("OffersCountManagerLiveSpec")(
      testM("create title and note") {

        val category = Category.CARS
        val section = Section.USED

        val clientId = ClientId(20101L)
        val reductionRatio = 0.37
        val reductionPercent = 37

        val offersCountDao = OffersCountDaoMock.DayToDayReduction(
          equalTo((category, Some(section), today.minusDays(2), today.minusDays(1))),
          value(List(OffersCountDao.Reduction(clientId, reductionRatio)))
        )

        val expected = TriggerEvent(
          eventType = TriggerEventType.OffersCountDecreased,
          clientId = clientId.value,
          updatedAt = now.toInstant,
          title = "Количество объявлений сократилось на 25% или более",
          note = Some(s"На $reductionPercent% сократилось количество объявлений в тарифе легковые с пробегом")
        )
        val triggerEventsDao = TriggerEventsDaoMock.Upsert(equalTo(Seq(expected)))

        val statisticsManager = StatisticsManagerMock.empty
        val cabinet = CabinetTest.empty
        val billing = BillingClientMock.empty

        val dependencies = ZLayer.fromMagic[Has[OffersCountManager]](
          clock,
          log,
          cabinet,
          billing,
          statisticsManager,
          offersCountDao,
          triggerEventsDao,
          OffersCountManagerLive.live
        )

        assertM(OffersCountManager(_.notifyDayToDayReduction(category, Some(section))))(Assertion.isUnit)
          .provideCustomLayer(dependencies)
      },
      testM("handle 100% reduction") {

        val category = Category.CARS
        val section = Section.USED

        val clientId = ClientId(20101L)
        val balanceId = BalanceClientId(123)
        val reductionRatio = 1.0
        val reductionPercent = 100

        val averageOutcomeKopecks = 10000.0
        val balanceKopecks = 500000

        val offersCountDao = OffersCountDaoMock.DayToDayReduction(
          equalTo((category, Some(section), today.minusDays(2), today.minusDays(1))),
          value(List(OffersCountDao.Reduction(clientId, reductionRatio)))
        )

        val expected = TriggerEvent(
          eventType = TriggerEventType.EmptyStock,
          clientId = clientId.value,
          updatedAt = now.toInstant,
          title = "Количество объявлений сократилось на 100%",
          note = Some(s"На $reductionPercent% сократилось количество объявлений в тарифе легковые с пробегом")
        )
        val triggerEventsDao = TriggerEventsDaoMock.Upsert(equalTo(Seq(expected)))

        val statisticsManager =
          StatisticsManagerMock.GetAverageWeekOutcome(
            equalTo((clientId.value, now.atZoneSameInstant(ZoneId.of("Europe/Moscow")))),
            value(averageOutcomeKopecks)
          )
        val cabinet =
          CabinetTest.GetClientBalanceIds(
            equalTo(Set(20101L)),
            value(List(ClientInfo.of(20101, 0, balanceId.value, false)))
          )
        val billing = BillingClientMock.GetCustomerOrders(
          equalTo((balanceId, None)),
          value(OrdersResponse(1, 1, 1, Seq(Order(0, balanceId.value, None, OrderBalance(0, 0, None, balanceKopecks)))))
        )

        val dependencies = ZLayer.fromMagic[Has[OffersCountManager]](
          clock,
          log,
          cabinet,
          billing,
          statisticsManager,
          offersCountDao,
          triggerEventsDao,
          OffersCountManagerLive.live
        )

        assertM(OffersCountManager(_.notifyDayToDayReduction(category, Some(section))))(Assertion.isUnit)
          .provideCustomLayer(dependencies)
      },
      testM("don't create event if balance is empty") {

        val category = Category.CARS
        val section = Section.USED

        val clientId = ClientId(20101L)
        val balanceId = BalanceClientId(123)
        val reductionRatio = 1.0

        val averageOutcomeKopecks = 10000.0
        val balanceKopecks = 500

        val offersCountDao = OffersCountDaoMock.DayToDayReduction(
          equalTo((category, Some(section), today.minusDays(2), today.minusDays(1))),
          value(List(OffersCountDao.Reduction(clientId, reductionRatio)))
        )

        // не создаём триггер
        val triggerEventsDao = TriggerEventsDaoMock.empty

        val statisticsManager =
          StatisticsManagerMock.GetAverageWeekOutcome(
            equalTo((clientId.value, now.atZoneSameInstant(ZoneId.of("Europe/Moscow")))),
            value(averageOutcomeKopecks)
          )
        val cabinet =
          CabinetTest.GetClientBalanceIds(
            equalTo(Set(20101L)),
            value(List(ClientInfo.of(20101, 0, balanceId.value, false)))
          )
        val billing = BillingClientMock.GetCustomerOrders(
          equalTo((balanceId, None)),
          value(OrdersResponse(1, 1, 1, Seq(Order(0, balanceId.value, None, OrderBalance(0, 0, None, balanceKopecks)))))
        )

        val dependencies = ZLayer.fromMagic[Has[OffersCountManager]](
          clock,
          log,
          cabinet,
          billing,
          statisticsManager,
          offersCountDao,
          triggerEventsDao,
          OffersCountManagerLive.live
        )

        assertM(OffersCountManager(_.notifyDayToDayReduction(category, Some(section))))(Assertion.isUnit)
          .provideCustomLayer(dependencies)
      }
    )
  }
}
