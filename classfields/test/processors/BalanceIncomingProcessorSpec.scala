package auto.dealers.amoyak.logic.processors

import auto.dealers.amoyak.consumers.logic.BalanceIncomingProcessor

import java.time.ZonedDateTime
import cats.syntax.option._
import auto.common.clients.cabinet.model.BalanceClient
import auto.common.clients.cabinet.testkit.CabinetTest
import common.models.finance.Money.Kopecks
import common.scalapb.ScalaProtobuf.toTimestamp
import common.zio.clock.MoscowClock
import auto.dealers.amoyak.model._
import auto.dealers.amoyak.storage.testkit.AmoIntegratorClientMock
import ru.auto.cabinet.api_model.ClientIdsResponse
import ru.yandex.vertis.billing.billing_event.BillingOperation
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.{BillingDomain, TransactionInfo}
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.TransactionInfo.TransactionType
import ru.yandex.vertis.billing.billing_event.TransactionBillingInfo.OrderState
import ru.yandex.vertis.billing.model.CustomerId
import zio.test.{DefaultRunnableSpec, _}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.mock.MockClock

object BalanceIncomingProcessorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    val timePoint =
      ZonedDateTime.of(2020, 1, 8, 0, 0, 0, 0, MoscowClock.timeZone).toOffsetDateTime
    val clockMock = MockClock.CurrentDateTime {
      value(timePoint)
    }

    suite("BalanceIncomingProcessor")(
      testM("should process incoming proto message") {
        val balanceId = 1L
        val agencyId = 2L
        val clientId = 3L
        val balance = 10L
        val incoming = 3L
        val customerId = CustomerId(1, balanceId, agencyId.some)

        val billingOperation = BillingOperation.defaultInstance
          .withTransactionInfo {
            TransactionInfo.defaultInstance
              .withAmount(incoming)
              .withCustomerId(customerId)
              .withType(TransactionType.INCOMING)
          }
          .withOrderState {
            OrderState.defaultInstance
              .withBalance(balance)
          }
          .withTimestamp(toTimestamp(timePoint))
          .withDomain(BillingDomain.AUTORU)

        val cabinet = {
          val expectedCabinetReq = Set(BalanceClient(balanceId, agencyId))

          val cabinetResp = Seq(
            ClientIdsResponse.ClientInfo(
              clientId = clientId,
              agencyId = agencyId,
              balanceId = balanceId,
              isAgency = false
            )
          )

          CabinetTest.GetClientByBalanceIds(equalTo(expectedCabinetReq), value(cabinetResp))
        }
        val amoClient = {
          val balanceTs = timePoint.toZonedDateTime

          val expectedReq: AmoJson[BalanceIncomingEvent] with AmoMessageType = BalanceIncomingEvent(
            clientId = clientId,
            isAgency = false,
            incoming = Kopecks(incoming),
            balance = Kopecks(balance),
            stateEpoch = balanceTs,
            timestamp = balanceTs
          )

          AmoIntegratorClientMock.PushMessage
            .of[AmoJson[BalanceIncomingEvent] with AmoMessageType](equalTo(expectedReq), unit)
        }
        val processor = (clockMock ++ cabinet ++ amoClient) >>> BalanceIncomingProcessor.live

        assertM(BalanceIncomingProcessor.process(billingOperation))(isUnit)
          .provideLayer(TestEnvironment.live ++ processor)

      },
      testM("should ignore negative incoming amount") {
        val balance = 10L
        val incoming = -6L

        val billingOperation = BillingOperation.defaultInstance
          .withTransactionInfo {
            TransactionInfo.defaultInstance
              .withAmount(incoming)
              .withCustomerId(CustomerId(1, clientId = 1L, agencyId = 2L.some))
              .withType(TransactionType.INCOMING)
          }
          .withOrderState {
            OrderState.defaultInstance
              .withBalance(balance)
          }
          .withTimestamp(toTimestamp(timePoint))
          .withDomain(BillingDomain.AUTORU)

        val noWorkExpected = MockClock.empty ++ CabinetTest.empty ++ AmoIntegratorClientMock.empty
        val processor = noWorkExpected >>> BalanceIncomingProcessor.live

        assertM(BalanceIncomingProcessor.process(billingOperation))(isUnit)
          .provideLayer(TestEnvironment.live ++ processor)

      },
      testM("should ignore zero incoming amount") {
        val balance = 10L
        val incoming = 0L

        val billingOperation = BillingOperation.defaultInstance
          .withTransactionInfo {
            TransactionInfo.defaultInstance
              .withAmount(incoming)
              .withCustomerId(CustomerId(1, clientId = 1L, agencyId = 2L.some))
              .withType(TransactionType.INCOMING)
          }
          .withOrderState {
            OrderState.defaultInstance
              .withBalance(balance)
          }
          .withTimestamp(toTimestamp(timePoint))
          .withDomain(BillingDomain.AUTORU)

        val noWorkExpected = MockClock.empty ++ CabinetTest.empty ++ AmoIntegratorClientMock.empty
        val processor = noWorkExpected >>> BalanceIncomingProcessor.live

        assertM(BalanceIncomingProcessor.process(billingOperation))(isUnit)
          .provideLayer(TestEnvironment.live ++ processor)

      }
    )

  }
}
