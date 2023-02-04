package ru.yandex.vertis.billing.banker.tasks

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.PaymentRequestRecord
import ru.yandex.vertis.billing.banker.dao.gens.TrustPurchaseRecordGen
import ru.yandex.vertis.billing.banker.model.PaymentRequest.EmptyForm
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens._
import ru.yandex.vertis.billing.banker.payment.TrustEnvironmentProvider
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentHelper.{
  PaymentTimeout,
  UnableToComputeTimeout,
  UnexpectedStatus
}
import ru.yandex.vertis.billing.banker.tasks.TrustProcessPendingPaymentTaskSpec._
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.trust.exceptions.TrustException.GetBasketError
import ru.yandex.vertis.billing.trust.model._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.time.Instant
import scala.concurrent.Future

class TrustProcessPendingPaymentTaskSpec
  extends Matchers
  with AnyWordSpecLike
  with MockitoSupport
  with JdbcSpecTemplate
  with AsyncSpecBase
  with TrustEnvironmentProvider {

  implicit override protected val rc: RequestContext = AutomatedContext("requestId")

  def makeTask(
      getBasketResponse: BasketResponse) = {
    initTrustMock(mockBaskets, mockBuildReceiptUrl).baskets
      .update(getBasketResponse.purchaseToken, getBasketResponse)
    new TrustProcessPendingPaymentTask(purchaseDao, trustMock, paymentHelper)
  }

  private def createPaymentRequest(accountId: String, prId: PaymentRequestId, basketResponse: BasketResponse) = {
    val source = paymentRequestSourceGen(
      PaymentRequestSourceParams(amount = Some(basketResponse.amount.toLong * 100), account = Some(accountId))
    ).next
    paymentSystemDao.insertRequest(
      PaymentRequestRecord(
        id = prId,
        method = "trust_web_page",
        source = source,
        form = EmptyForm(prId)
      )
    )
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    createAccount(AccountId)
    createPaymentRequest(AccountId, PaymentRequestId, trivialBasketResponse).futureValue
  }

  "TrustProcessPendingPaymentTask" should {

    "mark Banker payment as processed when Trust payment is authorized" in {
      purchaseDao.insert(trivialPurchaseRecord).futureValue
      val task = makeTask(trivialBasketResponse)
      task.execute().futureValue
    }

    "fail on GetBasketError with UnableToGetBusket" in {
      purchaseDao.insert(trivialPurchaseRecord).futureValue
      val task = makeTask(trivialBasketResponse)
      stub(trustMock.getBasket(_: PurchaseToken)(_: Traced)) { case (_, _) =>
        Future.failed(GetBasketError("SomeErrMsg", None))
      }
      task.execute().failed.futureValue shouldBe an[GetBasketError]
    }

    "fail on started payments without startTs" in {
      purchaseDao.insert(trivialPurchaseRecord).futureValue
      val basketResponse = trivialBasketResponse.copy(paymentStatus = PaymentStatus.Started, startTs = None)
      val task = makeTask(basketResponse)
      task.execute().failed.futureValue shouldBe an[UnableToComputeTimeout]
    }

    "successfully skip started payments if timeout has not been reached" in {
      purchaseDao.insert(trivialPurchaseRecord).futureValue
      val basketResponse = trivialBasketResponse.copy(
        paymentStatus = PaymentStatus.Started,
        paymentTimeout = 600,
        startTs = Some(Instant.now().minusSeconds(60))
      )
      val task = makeTask(basketResponse)
      task.execute().futureValue
    }

    "fail on started payments if timeout has been reached" in {
      purchaseDao.insert(trivialPurchaseRecord).futureValue
      val basketResponse = trivialBasketResponse.copy(
        paymentStatus = PaymentStatus.Started,
        paymentTimeout = 60,
        startTs = Some(Instant.now().minusSeconds(600))
      )
      val task = makeTask(basketResponse)
      task.execute().failed.futureValue shouldBe an[PaymentTimeout]
    }

    "fail on unexpected status" in {
      purchaseDao.insert(trivialPurchaseRecord).futureValue
      val basketResponse = trivialBasketResponse.copy(paymentStatus = PaymentStatus.Refunded)
      val task = makeTask(basketResponse)
      task.execute().failed.futureValue shouldBe an[UnexpectedStatus]
    }

    "start dangling payments in Trust" in {
      purchaseDao.insert(trivialPurchaseRecord.copy(paymentStatus = PaymentStatus.NotStarted)).futureValue
      val basketResponse = trivialBasketResponse.copy(paymentStatus = PaymentStatus.NotStarted)
      val task = makeTask(basketResponse)
      task.execute().futureValue
    }

    "process dangling payments which are already completed in Trust" in {
      purchaseDao.insert(trivialPurchaseRecord.copy(paymentStatus = PaymentStatus.NotStarted)).futureValue
      val task = makeTask(trivialBasketResponse)
      task.execute().futureValue
    }
  }
}

object TrustProcessPendingPaymentTaskSpec {

  val NotifyUrl = "http://localhost:5002/api/1.x/service/autoru/trust"
  val AccountId = "someId"
  val PaymentRequestId = "somePaymentRequestId"
  val PurchaseToken = "someToken"

  val trivialPurchaseRecord =
    TrustPurchaseRecordGen.next.copy(
      paymentStatus = PaymentStatus.Started,
      prId = PaymentRequestId,
      purchaseToken = PurchaseToken
    )

  val trivialBasketResponse =
    BasketResponse(
      PurchaseToken,
      10,
      "RUB",
      Nil,
      0L,
      PaymentStatus.Authorized,
      101L,
      "web_payment",
      startTs = Some(Instant.now()),
      paymentTs = Some(Instant.now())
    )

}
