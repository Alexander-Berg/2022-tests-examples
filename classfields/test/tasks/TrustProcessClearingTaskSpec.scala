package ru.yandex.vertis.billing.banker.tasks

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.PaymentRequestRecord
import ru.yandex.vertis.billing.banker.dao.TrustExternalPurchaseDao.PurchaseRecord
import ru.yandex.vertis.billing.banker.model.PaymentRequest.EmptyForm
import ru.yandex.vertis.billing.banker.model.PaymentRequestId
import ru.yandex.vertis.billing.banker.model.gens.{paymentRequestSourceGen, PaymentRequestSourceParams, Producer}
import ru.yandex.vertis.billing.banker.payment.TrustEnvironmentProvider
import ru.yandex.vertis.billing.banker.tasks.TrustProcessClearingTask.ClearingFailed
import ru.yandex.vertis.billing.banker.tasks.TrustProcessClearingTaskSpec._
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.trust.exceptions.TrustException.ClearBasketError
import ru.yandex.vertis.billing.trust.model.{BasketResponse, PaymentStatus, PurchaseToken}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration._

class TrustProcessClearingTaskSpec extends Matchers with AnyWordSpecLike with TrustEnvironmentProvider {

  implicit override protected val rc: RequestContext = AutomatedContext("requestId")

  val somePurchase =
    PurchaseRecord(PurchaseToken, PaymentRequestId, TrustPaymentId, PaymentStatus.Authorized, 101L, "web_payment")

  val someBasket = BasketResponse(PurchaseToken, 0.0, "RUB", Nil, 0L, PaymentStatus.Authorized, 101L, "web_payment")

  override def beforeEach(): Unit = {
    super.beforeEach()
    createAccount(AccountId)
    createPaymentRequest(AccountId, PaymentRequestId).futureValue
  }

  val task = new TrustProcessClearingTask(purchaseDao, paymentHelper)

  private def createPaymentRequest(accountId: String, prId: PaymentRequestId) = {
    val source = paymentRequestSourceGen(PaymentRequestSourceParams(account = Some(accountId))).next
    paymentSystemDao.insertRequest(
      PaymentRequestRecord(
        id = prId,
        method = "card",
        source = source,
        form = EmptyForm(prId)
      )
    )
  }

  "TrustProcessClearingTaskSpec" should {

    "clear payments and set `Cleared` statuses" in {
      val trustState = initTrustMock(mockBaskets)
      trustState.baskets.put(someBasket.purchaseToken, someBasket)
      val result = for {
        _ <- purchaseDao.insert(somePurchase)
        _ <- task.execute()
        res <- purchaseDao.getByToken(somePurchase.purchaseToken)
      } yield res

      val clearedPurchase = result.futureValue
      clearedPurchase.get.paymentStatus shouldBe PaymentStatus.Cleared
      clearedPurchase.get.clearTs should not be empty
    }

    "retry if clearing is done later" in {
      val trustState = initTrustMock(mockBaskets)
      stub(trustMock.clearBasket(_: PurchaseToken)(_: Traced)) { case (_, _) =>
        Future.successful(())
      }
      trustState.baskets.put(someBasket.purchaseToken, someBasket)
      val result = for {
        _ <- purchaseDao.insert(somePurchase)
        _ <- task.execute()
        res <- purchaseDao.getByToken(somePurchase.purchaseToken)
      } yield res

      val stillAuthorizedPurchase = result.futureValue
      stillAuthorizedPurchase.get.paymentStatus shouldBe PaymentStatus.Authorized

      trustState(mockBaskets)
      task.execute().futureValue
      val clearedPurchase = purchaseDao.getByToken(somePurchase.purchaseToken).futureValue
      clearedPurchase.get.paymentStatus shouldBe PaymentStatus.Cleared
    }

    "fail if clearing has not succeeded" in {
      val trustState = initTrustMock(mockBaskets)
      stub(trustMock.clearBasket(_: PurchaseToken)(_: Traced)) { case (_, _) =>
        Future.failed(ClearBasketError("SomeErrMsg", None))
      }
      trustState.baskets.put(someBasket.purchaseToken, someBasket)
      val result = for {
        _ <- purchaseDao.insert(somePurchase)
        _ <- task.execute()
        res <- purchaseDao.getByToken(somePurchase.purchaseToken)
      } yield res

      val exception = result.failed.futureValue
      exception shouldBe an[ClearingFailed]
    }
  }
}

object TrustProcessClearingTaskSpec {

  val NotifyUrl = "http://localhost:5002/api/1.x/service/autoru/trust"
  val PaymentTimeout = 30.minutes

  val AccountId = "someId"
  val PurchaseToken = "someToken"
  val TrustPaymentId = "someTrustPaymentId"
  val PaymentRequestId = "somePaymentRequestId"
}
