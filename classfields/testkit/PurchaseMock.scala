package ru.yandex.vertis.billing.shop.billing_gates.trust.testkit

import ru.yandex.vertis.billing.shop.billing_gates.trust.model.PaymentMethod.BankCard
import ru.yandex.vertis.billing.shop.billing_gates.trust.model._
import ru.yandex.vertis.billing.shop.billing_gates.trust.testkit.PurchaseMock.basketResponseSuccess
import ru.yandex.vertis.billing.shop.model._
import ru.yandex.vertis.billing.shop.model.trust.{PurchaseResult, RefundResult}

import scala.util.Random

case class PurchaseMock(
    var token: String,
    var refundId: String) {

  var createBasket: Either[TrustClientError, PurchaseToken] =
    Right(PurchaseToken(token)).withLeft[TrustClientError]

  var hold: Either[TrustClientError, BasketResponse] =
    Right(basketResponseSuccess(token, PaymentStatus.Authorized)).withLeft[TrustClientError]

  var clear: Either[TrustClientError, ClearResult] =
    Right(ClearResult(ResponseStatus.Success)).withLeft[TrustClientError]

  var createRefund: Either[TrustClientError, RefundId] =
    Right(RefundId(refundId)).withLeft[TrustClientError]

  var performRefund: Either[TrustClientError, RefundResponse] =
    Right(RefundResponse(RefundStatus.Success, None)).withLeft[TrustClientError]

  def setHold(status: PaymentStatus, urlPresent: Boolean = false) = {
    hold = Right(basketResponseSuccess(token, status, urlPresent)).withLeft[TrustClientError]
    this
  }

  def setClear(status: ResponseStatus) = {
    clear = Right(ClearResult(status)).withLeft[TrustClientError]
    this
  }

  def setPerformRefund(status: RefundStatus) = {
    performRefund = Right(
      RefundResponse(status, None)
    ).withLeft[TrustClientError]
    this
  }

  def failBasket = {
    createBasket = Left(TrustSttpError(s"Creation of basket with purchase id $token failed", new RuntimeException))
    this
  }

  def failHold = {
    hold = Left(TrustSttpError(s"Failed to start payment for purchase with token $token ", new RuntimeException))
    this
  }

  def failClear = {
    clear = Left(TrustSttpError(s"Failed to clear payment for purchase with token $token", new RuntimeException))
    this
  }

  def failCreateRefund = {
    createRefund = Left(TrustSttpError(s"Failed to create refund for purchase with token $token", new RuntimeException))
    this
  }

  def failPerformRefund = {
    performRefund = Left(
      TrustSttpError(s"Failed to perform refund for purchase with token $token", new RuntimeException)
    )
    this
  }

  def failFromCreateBasket = {
    failBasket
    failHold
    failClear
    failCreateRefund
    failPerformRefund
    this
  }

  def failFromHold = {
    failHold
    failClear
    failCreateRefund
    failPerformRefund
    this
  }

  def failFromClear = {
    failClear
    failCreateRefund
    failPerformRefund
    this
  }

  def failFromCreateRefund = {
    failCreateRefund
    failPerformRefund
    this
  }

  def createRandomPurchaseTest() = {
    randomizeCreateBasket()
    randomizeHold()
    randomizeClear()

  }

  def randomizeCreateBasket() = {
    Random.between(0, 2) match {
      case 0 => failFromCreateBasket
      case 1 => ()
    }
  }

  def randomizeHold() =
    createBasket match {
      case Left(value) => ()
      case Right(value) =>
        Random.between(1, 7) match {
          // case 0 => setHold(PaymentStatus.NotStarted) //состояние NotStarted невозможно после hold'а
          case 1 => setHold(PaymentStatus.Started)
          case 2 => setHold(PaymentStatus.Authorized)
          case 3 => setHold(PaymentStatus.NotAuthorized)
          case 4 => setHold(PaymentStatus.Canceled)
          case 5 => setHold(PaymentStatus.Cleared)
          case 6 => setHold(PaymentStatus.Refunded)
        }
    }

  def randomizeClear() = {
    hold match {
      case Left(value) => ()
      case Right(value) =>
        value.paymentStatus match {
          case PaymentStatus.Cleared => setClear(ResponseStatus.Success)
          case PaymentStatus.Authorized =>
            Random.between(0, 3) match {
              case 0 => setClear(ResponseStatus.Success)
              case 1 => setClear(ResponseStatus.Error)
              case 2 => failFromClear
            }
          case _ => setClear(ResponseStatus.Error)
        }

    }

  }

}

object PurchaseMock {

  def basketResponseSuccess(token: String, status: PaymentStatus, urlPresent: Boolean = false) = BasketResponse(
    purchaseToken = token,
    orders = List(
      PaymentOrder(
        orderId = Random.between(1L, 1000L),
        orderTs = None,
        productId = "raise_1",
        productType = "raise",
        productName = "raise",
        origAmount = 1L,
        paidAmount = 1L,
        currentQty = 1.0
      )
    ),
    amount = 1L,
    currency = "RUB",
    paymentTimeout = 1200.00,
    paymentStatus = status,
    startTs = None,
    paymentUrl = if (urlPresent) Some("http://o.yandex.ru") else None
  )

  def purchaseResult(token: String, status: TrustWebHookPaymentStatus) = PurchaseResult(
    purchaseToken = token,
    status = status,
    statusCode = "success",
    trustPaymentId = token,
    serviceId = "666",
    serviceOrderId = "666",
    paymentMethod = BankCard(Random.nextLong(1000000000).toString),
    rrn = "na",
    bindingResult = None,
    userEmail = None,
    subsUntilTs = None
  )

  def refundResult(token: String, status: TrustWebHookRefundStatus) = RefundResult(
    purchaseToken = token,
    status = status,
    statusCode = "success",
    trustPaymentId = token,
    trustRefundId = token
  )

}
