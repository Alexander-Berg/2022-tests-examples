package auto.dealers.amoyak.tasks

import java.time.{Instant, OffsetDateTime}

import cats.syntax.option._
import auto.common.clients.cabinet.model.BalanceClient
import auto.common.clients.cabinet.testkit.CabinetTest
import common.zio.clock.MoscowClock
import common.zio.tagging.syntax._
import auto.dealers.amoyak.model.BillingTransaction.Withdraw
import auto.dealers.amoyak.model.blocks._
import auto.dealers.amoyak.tasks.PushTransactionsSummaryTask.PushTransactionsSummaryTaskConfig
import auto.dealers.amoyak.storage.dao.BillingTransactionsDao
import auto.dealers.amoyak.storage.testkit.{BillingTransactionsDaoMock, LastSyncTimestampDaoMock}
import ru.auto.cabinet.api_model.ClientIdsResponse.ClientInfo
import zio.Has
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.{Expectation, MockClock}
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, _}

object PushTransactionsSummaryTaskSpec extends DefaultRunnableSpec {

  object testConsts {

    val callWithdraw = Withdraw(
      name = "call",
      costName = "PerCall",
      costUnits = 240000,
      actual = 0,
      expected = 100,
      transactionOrderId = "91775",
      trId = "5048iddqd",
      eventId = None,
      balanceClientId = 2L,
      balanceAgencyId = 3L.some,
      priceWithoutDiscount = None,
      timestamp = OffsetDateTime.parse("2020-12-07T11:30:00.000000Z").toInstant.toEpochMilli,
      operationTimestamp = OffsetDateTime.parse("2020-12-07T11:30:00.000000Z").toInstant.toEpochMilli
    )

    val placementWithdraw = Withdraw(
      name = "placement",
      costName = "PerIndexing",
      costUnits = 0,
      actual = 2000,
      expected = 3000,
      transactionOrderId = "91775",
      trId = "ba71140idkfa",
      eventId = "3150a36bc497c96cd6d2f3".some,
      balanceClientId = 5L,
      balanceAgencyId = 6L.some,
      priceWithoutDiscount = 2000L.some,
      timestamp = OffsetDateTime.parse("2020-12-07T11:30:00.000000Z").toInstant.toEpochMilli,
      operationTimestamp = OffsetDateTime.parse("2020-12-07T11:30:00.000000Z").toInstant.toEpochMilli
    )

    // callWithdraw - for checking discount sum and last transaction state
    // placement withdraw - checking sums
    val withdraws = Seq(
      // first transaction without any costs
      callWithdraw,
      // the same transaction with next stateEpoch and some actual amount
      callWithdraw.copy(
        actual = 2000,
        timestamp = OffsetDateTime.parse("2020-12-07T11:40:00.000000Z").toInstant.toEpochMilli
      ),
      // another transaction without costs
      callWithdraw.copy(trId = "5048iddqddd"),
      placementWithdraw,
      placementWithdraw.copy(trId = "5028iddqddd", actual = 1000)
    )

    val beginingInstant = Instant.parse("2020-12-07T10:00:00Z") // 13 in msk
    val beginningDt = MoscowClock.ofInstant(beginingInstant)
    val now = MoscowClock.asMoscowTime(OffsetDateTime.parse("2020-12-08T13:04:00Z")) // 16 in msc

    val task = new PushTransactionsSummaryTask()

    val cfg = PushTransactionsSummaryTaskConfig(
      timeWindowSizeHours = 1,
      deliveryLagHours = 24,
      includedProducts = Seq("placement", "call")
    )
  }

  val checkGetExpenses = testM("data gathering and aggregation") {
    import testConsts._

    val daoLayer: Expectation[Has[BillingTransactionsDao.Service]] = BillingTransactionsDaoMock.GetWithdraws(
      equalTo((beginningDt, beginningDt.plusHours(cfg.timeWindowSizeHours))),
      result = value(withdraws)
    )

    val cabinetLayer = {
      val expected = Set(
        BalanceClient(clientId = 2, agencyId = 3),
        BalanceClient(clientId = 5, agencyId = 6)
      )
      val clientInfos = Seq(
        ClientInfo(clientId = 1, agencyId = 3, balanceId = 2),
        ClientInfo(clientId = 4, agencyId = 6, balanceId = 5)
      )
      CabinetTest.GetClientByBalanceIds(equalTo(expected), value(clientInfos))
    }

    val clockLayer = MockClock.CurrentDateTime {
      value(now.toOffsetDateTime)
    }.optional

    val expected = Map(
      ClientInfo(clientId = 1, agencyId = 3, balanceId = 2) -> Seq(
        ProductExpensesSummary(
          product = "call",
          amount = 20L,
          amountWithoutDiscount = 2L,
          count = 2L
        )
      ),
      ClientInfo(clientId = 4, agencyId = 6, balanceId = 5) -> Seq(
        ProductExpensesSummary(
          product = "placement",
          amount = 30L,
          amountWithoutDiscount = 40L,
          count = 2L
        )
      )
    )

    val result = task
      .getExpenses(beginningDt, beginningDt.plusHours(cfg.timeWindowSizeHours), cfg.includedProducts)
      .provideLayer(daoLayer ++ cabinetLayer ++ clockLayer)
    assertM(result)(equalTo(expected))
  }

  val checkDealerExpenses = test("aggregating expenses") {
    import testConsts._
    val expected = Seq(
      // extractDealerExpenses does not deduplicate
      ProductExpensesSummary(product = "call", amount = 20L, amountWithoutDiscount = 3L, count = 3L),
      ProductExpensesSummary("placement", 30L, 40L, 2L)
    )

    val result = task.extractDealerExpenses(withdraws).toSeq
    assert(result)(hasSameElements(expected))
  }

  val checkTaskHourShifting = testM("check remaining hours") {
    import testConsts._

    val lastSyncDao = LastSyncTimestampDaoMock.Get(value(beginingInstant))

    /** if we start at 13.04(yesterday), and at last time ended at 10:00,
      * then we should process 11 and 12 of yesterday (delivery lag)
      */
    val expectedHours = (1 to 2).map(i => beginningDt.plusHours(i))

    val remainingHours = task
      .getRemainingTimeWindows(now, cfg)
      .provideLayer(lastSyncDao.toLayer.tagged[PushTransactionsSummaryTask])
    assertM(remainingHours)(equalTo(expectedHours))

  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PushTransactionsSummaryTask")(checkTaskHourShifting, checkDealerExpenses, checkGetExpenses)
}
