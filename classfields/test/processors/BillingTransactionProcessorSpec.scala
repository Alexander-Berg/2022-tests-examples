package auto.dealers.amoyak.logic.processors

import cats.syntax.option._
import auto.common.clients.cabinet.model.BalanceClient
import auto.common.clients.cabinet.testkit.CabinetTest
import common.models.finance.Money.Kopecks
import common.scalapb.ScalaProtobuf.instantToTimestamp
import common.zio.clock.MoscowClock
import common.zio.ops.prometheus.Prometheus
import auto.common.manager.statistics.StatisticsManagerLive
import auto.dealers.amoyak.consumers.logic.BillingTransactionProcessor
import auto.dealers.amoyak.logic.gens.gens._
import auto.dealers.amoyak.model.AmoCrmPatch.FinancePatch
import auto.dealers.amoyak.model._
import auto.dealers.amoyak.model.model.defaultAmoDateTimeFormatter
import auto.dealers.amoyak.storage.testkit.AmoIntegratorClientMock
import ru.auto.cabinet.api_model.ClientIdsResponse
import ru.yandex.vertis.billing.billing_event.TransactionBillingInfo.OrderState
import ru.yandex.vertis.billing.model.CustomerId
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.mock.MockClock

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime

object BillingTransactionProcessorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {

//    val timePoint = ZonedDateTime.parse("2020-08-01 03:00:00", defaultAmoDateTimeFormatter).toInstant
//
//    def instantToOffsetDt(instant: Instant) = OffsetDateTime
//      .ofInstant(
//        Instant.ofEpochMilli(instant.toEpochMilli),
//        MoscowClock.timeZone
//      )
//
//    val clockMock = MockClock.CurrentDateTime(value(instantToOffsetDt(timePoint)))

    suite("BillingTransactionProcessor")(
//      testM(
//        "should send billing's balance info about client to amo if average outcome > 0 and balance / averageOutcome <= 7"
//      ) {
//
//        val firstBalanceId = 1L
//        val firstAgencyId = 2L
//        val firstClientId = 3L
//
//        val isAgency = true
//        val firstCustomerId = CustomerId(1, firstBalanceId, firstAgencyId.some)
//
//        val billingOperationsGen = for {
//          billingOperation <- billingOperationGen(
//            customerIdGen = firstCustomerId,
//            timestampGen = instantToTimestamp(timePoint)
//          ).map(_.withOrderState(OrderState.defaultInstance.withBalance(100L)))
//          transactionWithoutPayload = billingOperation.clearTransactionInfo
//          transactionWithoutDomain = transactionWithoutPayload.clearDomain
//          transactions = List(
//            transactionWithoutPayload,
//            transactionWithoutDomain,
//            billingOperation
//          )
//        } yield transactions
//
//        val cabinet = {
//          val expectedCabinetReq =
//            Set(BalanceClient(firstBalanceId, firstAgencyId))
//
//          val cabinetResp =
//            Seq(
//              ClientIdsResponse.ClientInfo(
//                clientId = firstClientId,
//                agencyId = firstAgencyId,
//                balanceId = firstBalanceId,
//                isAgency = isAgency
//              )
//            )
//
//          CabinetTest.GetClientByBalanceIds(equalTo(expectedCabinetReq), value(cabinetResp))
//        }
//
//        val statistics = {
//          val expectedStatisticsReq = (
//            ActivateSumMedian30(firstClientId),
//            Workspace.Autoru,
//            Producer.SalesmanServicesStatDailyAggregated,
//            instantToOffsetDt(timePoint).minusDays(7).toLocalDate,
//            Some(instantToOffsetDt(timePoint).toLocalDate)
//          )
//
//          val statisticsClient = StatisticsClientTest.GetMetrics(
//            equalTo(expectedStatisticsReq),
//            value(Seq(Metric(name = "average", timestamp = instantToOffsetDt(timePoint), value = 300.0)))
//          )
//          statisticsClient >>> StatisticsManagerLive.live
//        }
//
//        val amoClient = {
//          val balanceTs = MoscowClock.ofInstant(timePoint)
//
//          val expectedReq: AmoJson[FinancePatch] with AmoMessageType = AmoCrmPatch.FinancePatch(
//            firstClientId,
//            isAgency,
//            balance = Kopecks(100L),
//            stateEpoch = balanceTs,
//            averageOutcome = Kopecks(300L).some,
//            balanceTs
//          )
//
//          AmoIntegratorClientMock.PushFinance.of[AmoJson[FinancePatch] with AmoMessageType](
//            equalTo(expectedReq),
//            unit
//          )
//        }
//
//        val processor =
//          (clockMock ++ cabinet ++ amoClient) >+> statistics >+> Prometheus.live >>> BillingTransactionProcessor.live
//
//        checkM(billingOperationsGen) { billingOperations =>
//          assertM(BillingTransactionProcessor.process(billingOperations))(isUnit)
//            .provideLayer(TestEnvironment.live ++ processor)
//        }
//      }
    )
  }

}
