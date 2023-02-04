package auto.dealers.balance_alerts.logic.test

import auto.common.clients.cabinet.model.BalanceClient
import auto.common.clients.cabinet.testkit.CabinetTest
import auto.common.clients.statistics.model._
import auto.common.converters.BillingOperationConverterLive
import auto.common.manager.statistics.testkit.StatisticsManagerMock
import auto.dealers.balance_alerts.logic.BillingTransactionProcessor
import auto.dealers.balance_alerts.logic.BillingTransactionProcessor.StatisticsClientFailure
import auto.dealers.balance_alerts.model.BalanceEventType
import auto.dealers.balance_alerts.storage.postgres.PgBalanceEventsRepository
import auto.dealers.balance_alerts.storage.testkit.Schema
import auto.dealers.balance_alerts.storage.BalanceEventsRepository
import auto.dealers.balance_alerts.storage.BalanceEventsRepository.BalanceEventsRepository
import common.scalapb.ScalaProtobuf
import common.zio.clock.MoscowClock
import common.zio.doobie.testkit.TestPostgresql
import common.zio.logging.Logging
import ru.auto.cabinet.api_model.ClientIdsResponse.ClientInfo
import ru.yandex.vertis.billing.billing_event.BillingOperation
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.{BillingDomain, TransactionInfo}
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.TransactionInfo.TransactionType
import ru.yandex.vertis.billing.billing_event.TransactionBillingInfo.OrderState
import ru.yandex.vertis.billing.model.CustomerId
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.TestAspect._
import zio.test.mock.MockClock

import java.time.OffsetDateTime

object BillingTransactionProcessorSpec extends DefaultRunnableSpec {

  val timestamp = OffsetDateTime
    .parse("2007-09-03T10:11:30+00:00")

  val transactionInfo = TransactionInfo(
    `type` = Some(TransactionType.INCOMING),
    customerId = Some(CustomerId(version = 1, clientId = 1))
  )

  val orderState = OrderState(balance = Some(900))

  val billingOperation = BillingOperation(
    timestamp = Some(ScalaProtobuf.toTimestamp(timestamp)),
    transactionInfo = Some(transactionInfo),
    orderState = Some(orderState),
    domain = Some(BillingDomain.AUTORU)
  )

  val cabinetMock = CabinetTest.GetClientByBalanceIds(
    equalTo(Set(BalanceClient(1, None))),
    value(Seq(ClientInfo.defaultInstance.withClientId(1)))
  )

  val clockMock = MockClock.CurrentDateTime {
    value(OffsetDateTime.now())
  }.optional

  def spec: ZSpec[Environment, Failure] = {
    val suit = suite("BillingTransactionProcessorSpec")(
      trivialEventTest,
      withdrawManyDaysLeftEventTest,
      statsErrorTest,
      manyDaysLeftTest,
      multipleEventsTest,
      multipleDealersTest
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential

    val env = (TestPostgresql.managedTransactor >+> PgBalanceEventsRepository.live) ++ Logging.live

    suit.provideCustomLayerShared(env)
  }

  val trivialEventTest = testM("Single billing event") {
    val statsMock = statisticsCall(
      billingOperation.getTransactionInfo.getCustomerId.clientId,
      timestamp,
      Right(300) // balance = 900, three days left
    )

    val test = for {
      _ <- BillingTransactionProcessor.process(Seq(billingOperation))
      events <- BalanceEventsRepository.getEvents
    } yield assertTrue {
      events
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.ThreeDaysLeft, timestamp)
          )
        )
    }

    val env =
      ZLayer.requires[
        BalanceEventsRepository with Logging.Logging
      ] ++ (statsMock >>> BillingOperationConverterLive.live) ++ cabinetMock ++ clockMock >>> BillingTransactionProcessor.live

    test.provideSomeLayer(env)
  }

  val withdrawManyDaysLeftEventTest = testM("Withdraw event when many days left") {
    val statsMock = statisticsCall(
      billingOperation.getTransactionInfo.getCustomerId.clientId,
      timestamp,
      Right(10) // balance = 900, many days left
    )

    val billingOp = billingOperation
      .withTransactionInfo(
        transactionInfo
          .withType(TransactionType.WITHDRAW)
      )

    val test = for {
      _ <- BillingTransactionProcessor.process(Seq(billingOp))
      events <- BalanceEventsRepository.getEvents
    } yield assertTrue(events.isEmpty)

    val env =
      ZLayer.requires[
        BalanceEventsRepository with Logging.Logging
      ] ++ (statsMock >>> BillingOperationConverterLive.live) ++ cabinetMock ++ clockMock >>> BillingTransactionProcessor.live

    test.provideSomeLayer(env)
  }

  val statsErrorTest = testM("Statistics returns error") {
    val statsMock = statisticsCall(
      billingOperation.getTransactionInfo.getCustomerId.clientId,
      timestamp,
      Left(
        StatisticsError.UnknownError.apply(
          StatisticsMetadata(
            None,
            None
          ),
          Left("unknown error")
        )
      )
    )

    val test = for {
      res <- BillingTransactionProcessor.process(Seq(billingOperation)).run
    } yield assert(res)(fails(isSubtype[StatisticsClientFailure](anything)))

    val env =
      ZLayer.requires[
        BalanceEventsRepository with Logging.Logging
      ] ++ (statsMock >>> BillingOperationConverterLive.live) ++ cabinetMock ++ clockMock >>> BillingTransactionProcessor.live

    test.provideSomeLayer(env)
  }

  val manyDaysLeftTest = testM("Single billing event (type = Ok)") {
    val statsMock = statisticsCall(
      billingOperation.getTransactionInfo.getCustomerId.clientId,
      timestamp,
      Right(100) // balance = 900, Ok event type
    )

    val test = for {
      _ <- BillingTransactionProcessor.process(Seq(billingOperation))
      events <- BalanceEventsRepository.getEvents
    } yield assertTrue {
      events
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.Ok, timestamp)
          )
        )
    }

    val env =
      ZLayer.requires[
        BalanceEventsRepository with Logging.Logging
      ] ++ (statsMock >>> BillingOperationConverterLive.live) ++ cabinetMock ++ clockMock >>> BillingTransactionProcessor.live

    test.provideSomeLayer(env)
  }

  val multipleEventsTest = testM("Multiple events") {
    val statsMock = statisticsCall(
      billingOperation.getTransactionInfo.getCustomerId.clientId,
      timestamp,
      Right(100)
    )

    // Balance = 100, 1 day left
    val billingOp1 = billingOperation
      .withTransactionInfo(transactionInfo.withType(TransactionType.WITHDRAW))
      .withOrderState(orderState.withBalance(100))
      .withTimestamp(ScalaProtobuf.toTimestamp(timestamp.minusDays(1)))

    // Balance = 900, many days left
    val billingOp2 = billingOperation

    val test = for {
      _ <- BillingTransactionProcessor.process(Seq(billingOp1, billingOp2))
      events <- BalanceEventsRepository.getEvents
    } yield assertTrue {
      events
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.Ok, timestamp)
          )
        )
    }

    val env =
      ZLayer.requires[
        BalanceEventsRepository with Logging.Logging
      ] ++ (statsMock >>> BillingOperationConverterLive.live) ++ cabinetMock ++ clockMock >>> BillingTransactionProcessor.live

    test.provideSomeLayer(env)
  }

  val multipleDealersTest = testM("Multiple events from several") {
    val clientId1 = 1L
    val clientId2 = 2L

    val billingOp1 = billingOperation
      .withTransactionInfo(
        transactionInfo
          .withCustomerId(CustomerId(version = 1, clientId = clientId2))
          .withType(TransactionType.WITHDRAW)
      )
      .withOrderState(orderState.withBalance(100))
      .withTimestamp(ScalaProtobuf.toTimestamp(timestamp.minusDays(1)))

    val billingOp2 = billingOperation

    val statsMock1 = statisticsCall(
      clientId1,
      timestamp,
      Right(100)
    )

    val statsMock2 = statisticsCall(
      clientId2,
      timestamp.minusDays(1),
      Right(100)
    )

    val statsMock = statsMock2.atMost(1) && statsMock1.atMost(1)

    val cabinetMock2 = CabinetTest.GetClientByBalanceIds(
      equalTo(Set(BalanceClient(clientId2, None))),
      value(Seq(ClientInfo.defaultInstance.withClientId(clientId2)))
    )

    val test = for {
      _ <- BillingTransactionProcessor.process(Seq(billingOp1, billingOp2))
      events <- BalanceEventsRepository.getEvents
    } yield assertTrue {
      events
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .toSet ==
        Set[(Long, BalanceEventType, OffsetDateTime)](
          (1L, BalanceEventType.Ok, timestamp),
          (2L, BalanceEventType.OneDayLeft, timestamp.minusDays(1))
        )
    }

    val env =
      ZLayer.requires[
        BalanceEventsRepository with Logging.Logging
      ] ++ (statsMock >>> BillingOperationConverterLive.live) ++ (cabinetMock && cabinetMock2) ++ clockMock >>> BillingTransactionProcessor.live

    test.provideSomeLayer(env)
  }

  private def statisticsCall(clientId: Long, till: OffsetDateTime, result: Either[StatisticsError, Double]) =
    StatisticsManagerMock
      .GetAverageWeekOutcome(
        equalTo(
          (clientId, MoscowClock.asMoscowTime(till))
        ),
        result match {
          case Right(balance) => value(balance)
          case Left(error) => failure(error)
        }
      )

}
