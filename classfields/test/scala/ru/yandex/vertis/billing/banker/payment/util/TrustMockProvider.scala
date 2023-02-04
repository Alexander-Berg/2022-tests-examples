package ru.yandex.vertis.billing.banker.payment.util

import org.mockito.Mockito
import org.scalatest.{BeforeAndAfterEach, Suite}
import ru.yandex.vertis.billing.banker.model.gens._
import ru.yandex.vertis.billing.banker.model.notNull
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentSupport
import ru.yandex.vertis.billing.banker.payment.util.TrustMockProvider.TrustState
import ru.yandex.vertis.billing.trust.TrustApi
import ru.yandex.vertis.billing.trust.exceptions.TrustException._
import ru.yandex.vertis.billing.trust.model.PaymentStatus.{Authorized, Cleared, NotAuthorized, NotStarted, Started}
import ru.yandex.vertis.billing.trust.model._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TrustMockProvider extends MockitoSupport with BeforeAndAfterEach {
  this: Suite =>

  protected val trustMock: TrustApi = mock[TrustApi]

  implicit val trace: Traced = Traced.empty

  override def afterEach(): Unit = {
    Mockito.reset(trustMock)
    Mockito.clearInvocations(trustMock)
  }

  protected def initTrustMock(mocks: (TrustState => Unit)*): TrustState = TrustState()(mocks: _*)

  protected def mockBuildReceiptUrl(state: TrustState): Unit = {
    stub(trustMock.buildReceiptUrl(_: PurchaseToken)) { case purchaseToken =>
      s"receiptEndpoint/checks/$purchaseToken/receipts/$purchaseToken"
    }
    ()
  }

  protected def mockProducts(state: TrustState): Unit = {
    stub(trustMock.createProduct(_: CreateProductRequest)(_: Traced)) {
      case (CreateProductRequest(productId, name, fiscalTitle, fiscalNds), _) =>
        Future {
          state.products.put(productId, ProductResponse(productId, name, fiscalTitle, fiscalNds, None))
          ()
        }
    }
    stub(trustMock.getProductStatus(_: ProductId)(_: Traced)) { case (productId, _) =>
      Future {
        state.products.getOrElse(productId, throw ProductNotFoundError(productId, "product_not_found"))
      }
    }
    stub(trustMock.updateProduct(_: UpdateProductRequest)(_: Traced)) {
      case (UpdateProductRequest(id, name, fiscalTitle, fiscalNds), _) =>
        Future {
          state.products.updateWith(id) {
            case Some(_) => Some(ProductResponse(id, name, fiscalTitle, fiscalNds, None))
            case None => throw ProductNotFoundError(id, "product_not_found")
          }
          ()
        }
    }
    ()
  }

  protected def mockOrders(state: TrustState): Unit = {
    stub(trustMock.createOrder(_: CreateOrderRequest, _: PassportUid)(_: Traced)) {
      case (request, uid, _) if notNull(request) =>
        Future {
          val productId = request.productId
          val product = state.products.getOrElse(productId, throw ProductNotFoundError(productId, "product_not_found"))
          val orderId = idNoMoreLength().next
          val order = OrderResponse(
            productType = "app",
            uid = uid.toString,
            orderTs = s"${Instant.now().getEpochSecond}.000",
            currentQty = "0.00",
            orderId = orderId,
            productName = product.name,
            productId = productId,
            currentAmount = List.empty
          )
          state.orders.put(orderId, order)
          CreateOrderSuccess(orderId, productId)
        }
    }
    stub(trustMock.getOrderStatus(_: OrderId)(_: Traced)) { case (orderId, _) =>
      Future {
        state.orders.getOrElse(orderId, throw GetOrderError("order_not_found", None))
      }
    }
    ()
  }

  protected def mockBaskets(state: TrustState): Unit = {
    stub(trustMock.createBasket(_: CreateBasketRequest, _: PassportUid)(_: Traced)) { case (request, uid, _) =>
      Future {
        val trustPaymentId = hexString().next
        val purchaseToken = hexString().next
        val orders = request.orders.map { o =>
          val order = state.orders.getOrElse(o.orderId, throw CreateBasketError("order_not_found", None))
          val amount = o.price.*(o.qty)
          (order, amount)
        }
        val basket = BasketResponse(
          purchaseToken = purchaseToken,
          amount = orders.map(_._2).sum,
          currency = request.currency,
          orders = orders.map { case (order, amount) =>
            PaymentOrder(
              orderId = order.orderId,
              orderTs = Instant.now(),
              productId = order.productId,
              productType = order.productType,
              productName = order.productName,
              origAmount = amount,
              paidAmount = 0,
              currentQty = 0
            )
          },
          paymentTimeout = request.paymentTimeout,
          paymentStatus = NotStarted,
          yandexUid = uid,
          paymentMode = request.paymentMode,
          paymentMethodId = Some(request.paymethodId).filter(TrustPaymentSupport.isCardId),
          paymentMarkup = request.paymethodMarkup.map(BasketMarkup)
        )
        state.baskets.put(purchaseToken, basket)
        CreateBasketSuccess(trustPaymentId = trustPaymentId, purchaseToken = purchaseToken)
      }
    }
    stub(trustMock.getBasket(_: PurchaseToken)(_: Traced)) { case (purchaseToken, _) =>
      Future {
        state.baskets.getOrElse(purchaseToken, throw GetBasketError("payment_not_found", None))
      }
    }
    stub(trustMock.basketStartPayment(_: PurchaseToken)(_: Traced)) { case (purchaseToken, _) =>
      Future {
        state.baskets
          .updateWith(purchaseToken) {
            case Some(basket) if basket.paymentStatus == NotStarted && basket.paymentMode == "web_payment" =>
              Some(
                basket.copy(
                  paymentStatus = Started,
                  startTs = Some(Instant.now()),
                  paymentUrl = Some(s"https://trust-test.yandex.ru/web/payment?purchase_token=$purchaseToken")
                )
              )
            case Some(basket) if basket.paymentStatus == NotStarted =>
              Some(
                basket.copy(
                  paymentStatus = Started,
                  startTs = Some(Instant.now())
                )
              )
            case Some(basket) => Some(basket)
            case None => throw GetBasketError("payment_not_found", None)
          }
          .get
      }
    }
    stub(trustMock.clearBasket(_: PurchaseToken)(_: Traced)) { case (purchaseToken, _) =>
      Future {
        state.baskets
          .updateWith(purchaseToken) {
            case Some(basket) =>
              Some(
                basket.copy(
                  paymentStatus = Cleared,
                  clearTs = Some(Instant.now())
                )
              )
            case None => throw GetBasketError("payment_not_found", None)
          }
          .get
        ()
      }
    }
    stub(trustMock.markupBasket(_: PurchaseToken, _: BasketMarkup)(_: Traced)) { case (purchaseToken, markup, _) =>
      Future {
        state.baskets
          .updateWith(purchaseToken) {
            case Some(basket) =>
              Some(
                basket.copy(
                  paymentMarkup = Some(markup)
                )
              )
            case None => throw GetBasketError("payment_not_found", None)
          }
        ()
      }
    }
    ()
  }

  protected def mockRefunds(state: TrustState): Unit = {
    stub(trustMock.createRefund(_: CreateRefundRequest, _: PassportUid)(_: Traced)) { case (request, _, _) =>
      Future {
        val refundId = hexString().next
        val refund = RefundResponse(
          status = RefundStatus.WaitForNotification,
          statusDescription = "refund is in queue",
          fiscalReceiptUrl = None
        )

        state.baskets.updateWith(request.purchaseToken) {
          case Some(basket) =>
            Some(
              basket.copy(refunds =
                basket.refunds :+ BasketRefund(
                  trustRefundId = refundId,
                  amount = request.orders.map(_.deltaAmount).sum,
                  description = request.reasonDesc,
                  createTs = Instant.now(),
                  confirmTs = None
                )
              )
            )
          case None => throw GetBasketError("payment_not_found", None)
        }
        state.refunds.put(refundId, refund)
        refundId
      }
    }
    stub(trustMock.startRefund(_: TrustRefundId)(_: Traced)) { case (refundId, _) =>
      Future {
        state.refunds.getOrElse(refundId, throw RefundNotFoundError(refundId, "refund_not_found"))
        ()
      }
    }
    stub(trustMock.getRefund(_: TrustRefundId)(_: Traced)) { case (refundId, _) =>
      Future {
        state.refunds.getOrElse(refundId, throw RefundNotFoundError(refundId, "refund_not_found"))
      }
    }
    ()
  }

  protected def mockPlusBonus(state: TrustState): Unit = {
    stub(trustMock.createAccount(_: PassportUid, _: String)) { case (uid, currency) =>
      Future {
        val account = CreateAccountResponse(s"$uid-$currency", currency, s"$uid-$currency")
        state.accounts.updateWith(uid) {
          case None =>
            Some(List(account))
          case Some(accounts) =>
            Some((account +: accounts).distinct)
        }
        account
      }
    }
    stub(trustMock.getAccounts(_: PassportUid)) { case uid =>
      Future {
        state.accounts.view
          .mapValues(accountsResponse => AccountsResponse(accountsResponse.map(a => TrustAccount(a.id, a.currency))))
          .toMap
          .getOrElse(uid, throw GetAccountsError("account_not_found", Some("account_not_found")))
      }
    }
    stub(trustMock.createTopup(_: CreateTopupRequest, _: PassportUid)) {
      case (CreateTopupRequest(currency, amount, product, paymethodId), uid) =>
        Future {
          val token = hexString().next
          val trustPaymentId = hexString().next
          val topup = GetTopupSuccess(
            token,
            trustPaymentId,
            PaymentStatus.NotStarted,
            uid,
            amount,
            currency,
            None,
            Some(Instant.now()),
            None,
            None
          )
          state.topups.update((topup.purchaseToken, uid), topup)
          CreateTopupSuccess(token, trustPaymentId)
        }
    }
    stub(trustMock.getTopup(_: PurchaseToken, _: PassportUid)) { case (token, uid) =>
      Future {
        state.topups.getOrElse((token, uid), throw GetTopupError("topup_not_found", Some("topup_not_found")))
      }
    }
    ()
  }

  protected def succeedPayment(purchaseToken: PurchaseToken)(implicit state: TrustState): Unit = {
    state.baskets.updateWith(purchaseToken) {
      case Some(basket) if basket.paymentStatus == Started =>
        Some(
          basket.copy(
            paymentStatus = Authorized,
            paymentTs = Some(Instant.now())
          )
        )
      case _ => throw new IllegalStateException("Unexpected basket state")
    }
    ()
  }

  protected def cancelPayment(purchaseToken: PurchaseToken, errorCode: String)(implicit state: TrustState): Unit = {
    state.baskets.updateWith(purchaseToken) {
      case Some(basket) if basket.paymentStatus == Started =>
        Some(
          basket.copy(
            paymentStatus = NotAuthorized,
            cancelTs = Some(Instant.now()),
            authErrorCode = Some(errorCode)
          )
        )
      case _ => throw new IllegalStateException("Unexpected basket state")
    }
    ()
  }

  protected def succeedRefund(
      purchaseToken: PurchaseToken,
      refundId: TrustRefundId
    )(implicit state: TrustState): Unit = {
    state.baskets.updateWith(purchaseToken) {
      case Some(basket) =>
        val refunds = basket.refunds
        val currentRefund =
          refunds.find(_.trustRefundId == refundId).getOrElse(throw new IllegalStateException("Refund not found"))
        val isFullRefund = refunds.map(_.amount).sum == basket.amount
        Some(
          basket.copy(
            paymentStatus = if (isFullRefund) PaymentStatus.Refunded else basket.paymentStatus,
            refunds = refunds.filterNot(_.trustRefundId == refundId) :+ BasketRefund(
              trustRefundId = currentRefund.trustRefundId,
              amount = currentRefund.amount,
              description = currentRefund.description,
              createTs = currentRefund.createTs,
              confirmTs = Some(Instant.now())
            )
          )
        )
      case None => throw new IllegalStateException("Basket not found")
    }
    state.refunds.updateWith(refundId) {
      case Some(refund) if refund.status == RefundStatus.WaitForNotification =>
        Some(
          refund.copy(
            status = RefundStatus.Success,
            statusDescription = "refund sent to payment system",
            fiscalReceiptUrl = Some(readableString().next)
          )
        )
      case _ => throw new IllegalStateException("Unexpected refund state")
    }
    ()
  }

  protected def failRefund(refundId: TrustRefundId)(implicit state: TrustState): Unit = {
    state.refunds.updateWith(refundId) {
      case Some(refund) if refund.status == RefundStatus.WaitForNotification =>
        Some(
          refund.copy(
            status = RefundStatus.Failed,
            fiscalReceiptUrl = Some(readableString().next)
          )
        )
      case _ => throw new IllegalStateException("Unexpected refund state")
    }
    ()
  }
}

object TrustMockProvider {

  case class TrustState() {
    import scala.jdk.CollectionConverters._

    val products: scala.collection.concurrent.Map[ProductId, ProductResponse] =
      new ConcurrentHashMap[ProductId, ProductResponse]().asScala

    val orders: scala.collection.concurrent.Map[OrderId, OrderResponse] =
      new ConcurrentHashMap[OrderId, OrderResponse]().asScala

    val baskets: scala.collection.concurrent.Map[PurchaseToken, BasketResponse] =
      new ConcurrentHashMap[PurchaseToken, BasketResponse]().asScala

    val refunds: scala.collection.concurrent.Map[TrustRefundId, RefundResponse] =
      new ConcurrentHashMap[TrustRefundId, RefundResponse]().asScala

    val accounts: scala.collection.concurrent.Map[PassportUid, List[CreateAccountResponse]] =
      new ConcurrentHashMap[PassportUid, List[CreateAccountResponse]]().asScala

    val topups: scala.collection.concurrent.Map[(PurchaseToken, PassportUid), GetTopupSuccess] =
      new ConcurrentHashMap[(PurchaseToken, PassportUid), GetTopupSuccess]().asScala

    def apply(mocks: (TrustState => Unit)*): TrustState = {
      mocks.foreach(_.apply(this))
      this
    }
  }
}
