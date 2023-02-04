package auto.dealers.dealer_stats.test.consumers

import auto.dealers.dealer_stats.consumers.logic.BillingOperationProcessor
import auto.dealers.dealer_stats.consumers.logic.BillingOperationProcessor.BillingOperationProcessor
import auto.dealers.dealer_stats.model._
import auto.dealers.dealer_stats.storage.testkit.{InMemoryAutoruIdRepository, InMemoryDealerBalanceDao}
import common.scalapb.ScalaProtobuf._
import common.zio.logging.Logging
import ru.yandex.vertis.billing.billing_event.BillingOperation
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.{BillingDomain, TransactionInfo}
import ru.yandex.vertis.billing.billing_event.TransactionBillingInfo.OrderState
import ru.yandex.vertis.billing.model.CustomerId
import zio.magic._
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.{Has, ZLayer}

import java.time.OffsetDateTime

object BillingOperationProcessorSpec extends DefaultRunnableSpec {

  val balanceMapping = Map(
    BalanceId(BalanceClientId(1), BalanceAgencyId(None)) -> ClientId(20101),
    BalanceId(BalanceClientId(2), BalanceAgencyId(Some(100))) -> ClientId(68)
  )

  override def spec: ZSpec[TestEnvironment, Any] = (suite("BillingOperationProcessor")(
    testM("should save operation balance values") {
      val messages = List(
        billingOperation(
          120,
          BalanceClientId(1),
          BalanceAgencyId(None),
          OffsetDateTime.parse("2020-12-03T10:15:30+03:00")
        ),
        billingOperation(
          110,
          BalanceClientId(1),
          BalanceAgencyId(None),
          OffsetDateTime.parse("2020-12-03T10:25:30+03:00")
        ),
        billingOperation(
          220,
          BalanceClientId(2),
          BalanceAgencyId(Some(100)),
          OffsetDateTime.parse("2020-12-03T10:15:30+03:00")
        )
      )
      for {
        _ <- BillingOperationProcessor.process(messages)
        result <- InMemoryDealerBalanceDao.dump
      } yield assertTrue(result.map(_.balance).toSet == Set(120L, 110L, 220L))
    },
    testM("missing autoru ids") {
      val messages = List(
        billingOperation(
          100,
          BalanceClientId(111),
          BalanceAgencyId(None),
          OffsetDateTime.parse("2020-12-03T10:15:30+03:00")
        )
      )

      for {
        _ <- BillingOperationProcessor.process(messages)
        result <- InMemoryDealerBalanceDao.dump
      } yield assertTrue(result.isEmpty)
    }
  ) @@ after(InMemoryDealerBalanceDao.clean)).provideLayer {
    ZLayer.fromSomeMagic[TestEnvironment, Has[InMemoryDealerBalanceDao] with BillingOperationProcessor](
      Logging.live,
      InMemoryDealerBalanceDao.test,
      (ZLayer.succeed(balanceMapping) >>> InMemoryAutoruIdRepository.test),
      BillingOperationProcessor.live
    )
  }

  private def billingOperation(
      balance: Long,
      balanceClientId: BalanceClientId,
      balanceAgencyId: BalanceAgencyId,
      timestamp: OffsetDateTime): BillingOperation =
    BillingOperation(
      domain = Some(BillingDomain.AUTORU),
      timestamp = Some(toTimestamp(timestamp)),
      orderState = Some(OrderState(balance = Some(balance))),
      transactionInfo = Some(
        TransactionInfo(customerId =
          Some(CustomerId(clientId = balanceClientId.raw, agencyId = balanceAgencyId.raw, version = 0))
        )
      )
    )
}
