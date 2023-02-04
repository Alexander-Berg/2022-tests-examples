package ru.yandex.vertis.billing.banker.payment.impl

import org.joda.time.LocalDate
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.TrustExternalPurchaseDao.PurchaseRecord
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.{
  DuplicatePaymentIdException,
  DuplicateProductIdException,
  ProductNotFoundException
}
import ru.yandex.vertis.billing.banker.model.PaymentMethod.CardProperties.Brands
import ru.yandex.vertis.billing.banker.model.PaymentMethod.{
  CardProperties,
  EnabledCardProperties,
  YandexAccountProperties
}
import ru.yandex.vertis.billing.banker.model.PaymentRequest._
import ru.yandex.vertis.billing.banker.model.State.{AbstractNotificationSource, StateStatuses, Statuses}
import ru.yandex.vertis.billing.banker.model.TrustContext.{ApiPayment, WebPayment}
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens._
import ru.yandex.vertis.billing.banker.payment.TrustEnvironmentProvider
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentHelper.{ApiPaymentMode, WebPaymentMode}
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentSupport.{CardPaymentMethod, YandexAccountPaymentMethod}
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentSupportSpec._
import ru.yandex.vertis.billing.banker.payment.util.TrustMockProvider.TrustState
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.{ExternalServiceException, MethodFilter}
import ru.yandex.vertis.billing.banker.util.TrustConversions.ToTrustTaxType
import ru.yandex.vertis.billing.banker.util.UserContext
import ru.yandex.vertis.billing.receipt.model.TaxTypes
import ru.yandex.vertis.billing.trust.exceptions.TrustException.{
  CreateProductError,
  NoAuthError,
  TrustDeserializationError
}
import ru.yandex.vertis.billing.trust.model.PaymentStatus.Started
import ru.yandex.vertis.billing.trust.model._
import ru.yandex.vertis.tracing.Traced
import spray.json.{JsObject, JsString}
import sttp.client3.{basicRequest, SttpClientException, UriContext}

import java.net.SocketException
import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration._

class TrustPaymentSupportSpec extends AnyWordSpec with Matchers with AsyncSpecBase with TrustEnvironmentProvider {

  implicit override protected val rc =
    UserContext("trust-payment-support-test", "I am human. Trust me :)", Some(UserPassportUid))

  override def beforeEach(): Unit = {
    super.beforeEach()
    createAccount(UserAccountId)
    ()
  }

  "TrustPaymentSupport / operations with payments" should {

    "correctly handle payment request via trust_web_page" in {
      // Arrange: init mocks, setup data, prepare requests
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val source = buildRequestSourceForTrustWebPage()

      // Act: an action upon the system
      val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue

      // Assert: check response, check system state, verify mock invocations
      val purchaseOpt = purchaseDao.getById(form.id).futureValue
      purchaseOpt should not be empty
      val purchase = purchaseOpt.get
      purchase.trustPaymentId should not be empty
      purchase.paymentStatus shouldBe Started
      purchase.yandexUid shouldBe UserPassportUid
      purchase.paymentMode shouldBe WebPaymentMode
      purchase.startTs should not be empty

      assert(form.isInstanceOf[TrustWebForm])
      form.asInstanceOf[TrustWebForm].purchaseToken shouldBe purchase.purchaseToken
      form.asInstanceOf[TrustWebForm].url should not be empty

      Mockito.verify(trustMock, Mockito.times(2)).createOrder(?, ?)(?)
      Mockito.verify(trustMock, Mockito.times(1)).createBasket(?, ?)(?)
      Mockito.verify(trustMock, Mockito.times(1)).basketStartPayment(?)(?)
    }

    "handle the same payment request idempotently" in {
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val externalId = "user:4227-test-transaction-1"
      val source = buildRequestSourceForTrustWebPage(externalId = Some(externalId))

      val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue
      val theSameForm = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue

      form shouldBe theSameForm
    }

    "start a new payment for the same external transaction when previous payment is failed" in {
      implicit val trustState: TrustState = initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val externalId = "user:4227-test-transaction-2"
      val source = buildRequestSourceForTrustWebPage(externalId = Some(externalId))
      val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue
      val purchaseToken = form.asInstanceOf[TrustWebForm].purchaseToken
      failPayment(purchaseToken)

      val anotherForm = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue

      val anotherPurchaseToken = anotherForm.asInstanceOf[TrustWebForm].purchaseToken
      val result = purchaseDao.getByExternalId(externalId).futureValue
      result.map(_.purchaseToken) should contain theSameElementsAs List(purchaseToken, anotherPurchaseToken)
    }

    "fail to make a duplicate payment for the same external transaction when previous payment is completed" in {
      implicit val trustState: TrustState = initTrustMock(mockProducts, mockOrders, mockBaskets, mockBuildReceiptUrl)
      setupProducts(ProductOne, ProductTwo)
      val externalId = "user:4227-test-transaction-3"
      val source = buildRequestSourceForTrustWebPage(externalId = Some(externalId))
      val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue
      val purchaseToken = form.asInstanceOf[TrustWebForm].purchaseToken
      authorizePayment(purchaseToken)

      val exception = paymentSupport.request(source.account, CardPaymentMethod, source).failed.futureValue

      exception shouldBe an[DuplicatePaymentIdException]
    }

    "fail when product doesn't exist in Trust" in {
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      val source = buildRequestSourceForTrustWebPage()

      val exception = paymentSupport.request(source.account, CardPaymentMethod, source).failed.futureValue

      exception should
        equal(ProductNotFoundException("offer_raise")).or(equal(ProductNotFoundException("car_report")))

    }

    "process notifications about successful payments" in {
      implicit val trustState: TrustState = initTrustMock(mockBuildReceiptUrl, mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val purchase = setupPurchase()
      val notification = buildSuccessTrustNotification(purchase.purchaseToken)
      succeedPayment(purchase.purchaseToken)

      paymentSupport.parse(AbstractNotificationSource(notification)).futureValue

      val payment = paymentSupport.getPaymentRequest(UserAccountId, purchase.prId).futureValue
      payment.state should not be empty
      val state = payment.state.get
      state.id shouldBe purchase.prId
      state.account shouldBe UserAccountId
      state.amount shouldBe TotalAmount
      state.rawData shouldBe Raw.Empty
      state.status shouldBe Statuses.Processed
      state.stateStatus shouldBe StateStatuses.Valid
      state.invoiceId shouldBe Some(purchase.trustPaymentId)
      val authorizedPurchase = purchaseDao.getByToken(purchase.purchaseToken).futureValue
      authorizedPurchase.map(_.paymentStatus) shouldBe Some(PaymentStatus.Authorized)
      authorizedPurchase.map(_.paymentTs) should not be empty
      authorizedPurchase.map(_.receiptUrl) should not be empty
    }

    "process pending request on promotePendingPayment" in {
      implicit val trustState: TrustState = initTrustMock(mockBuildReceiptUrl, mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val purchase = setupPurchase()
      succeedPayment(purchase.purchaseToken)

      val pendingPayment = paymentSupport.getPaymentRequest(UserAccountId, purchase.prId).futureValue

      paymentSupport.syncPendingPayment(pendingPayment).futureValue

      val payment = paymentSupport.getPaymentRequest(UserAccountId, purchase.prId).futureValue
      payment.state should not be empty
      val state = payment.state.get
      state.id shouldBe purchase.prId
      state.account shouldBe UserAccountId
      state.amount shouldBe TotalAmount
      state.rawData shouldBe Raw.Empty
      state.status shouldBe Statuses.Processed
      state.stateStatus shouldBe StateStatuses.Valid
      state.invoiceId shouldBe Some(purchase.trustPaymentId)
      val authorizedPurchase = purchaseDao.getByToken(purchase.purchaseToken).futureValue
      authorizedPurchase.map(_.paymentStatus) shouldBe Some(PaymentStatus.Authorized)
      authorizedPurchase.map(_.paymentTs) should not be empty
      authorizedPurchase.map(_.receiptUrl) should not be empty
    }

    "process notifications about failed payments" in {
      implicit val trustState: TrustState = initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val purchase = setupPurchase()
      val notification = buildCancelledTrustNotification(purchase.purchaseToken)
      cancelPayment(purchase.purchaseToken, notification.statusCode)

      paymentSupport.parse(AbstractNotificationSource(notification)).futureValue

      val payment = paymentSupport.getPaymentRequest(UserAccountId, purchase.prId).futureValue
      payment.state should not be empty
      val state = payment.state.get
      state.id shouldBe purchase.prId
      state.account shouldBe UserAccountId
      state.amount shouldBe TotalAmount
      state.rawData shouldBe Raw.Empty
      state.status shouldBe Statuses.Processed
      state.stateStatus shouldBe StateStatuses.Cancelled
      state.invoiceId shouldBe Some(purchase.trustPaymentId)
      val cancelledPurchase = purchaseDao.getByToken(purchase.purchaseToken).futureValue
      cancelledPurchase.map(_.paymentStatus) shouldBe Some(PaymentStatus.NotAuthorized)
      cancelledPurchase.map(_.cancelTs) should not be empty
      cancelledPurchase.map(_.authErrorCode) should not be empty
    }
  }

  "TrustPaymentSupport / operations with refunds" should {

    "support full refunds for goods" in {
      implicit val trustState: TrustState =
        initTrustMock(mockProducts, mockOrders, mockBaskets, mockRefunds, mockBuildReceiptUrl)
      setupProducts(ProductOne, ProductTwo)
      val purchase = setupPurchase()
      authorizePayment(purchase.purchaseToken)
      clearPayment(purchase)

      paymentSupport.fullRefund(UserAccountId, purchase.prId, Some("full refund"), None).futureValue
      val refundRecord = getSingleRefundRecordFor(purchase.prId)
      succeedRefund(refundRecord.purchaseToken, refundRecord.trustRefundId)
      val notification = buildSuccessRefundTrustNotification(purchase.purchaseToken, refundRecord.trustRefundId)
      paymentSupport.parse(AbstractNotificationSource(notification)).futureValue

      val refundRequest = paymentSupport.getRefundRequest(UserAccountId, refundRecord.refundPrId).futureValue
      refundRequest.state.map(_.stateStatus) shouldBe Some(State.StateStatuses.Valid)
      refundRequest.state.map(_.status) shouldBe Some(State.Statuses.Processed)
      val processedRefundRecord = purchaseDao.getRefundByPrId(refundRequest.id).futureValue
      processedRefundRecord.map(_.refundStatus) shouldBe Some(RefundStatus.Success)
      processedRefundRecord.flatMap(_.fiscalReceiptUrl) should not be empty
      val payment = paymentSupport.getPaymentRequest(UserAccountId, purchase.prId).futureValue
      payment.state.map(_.stateStatus) shouldBe Some(State.StateStatuses.Refunded)

      // purchase is refunded when the refund is full
      val refundedPurchase = purchaseDao.getByToken(purchase.purchaseToken).futureValue
      refundedPurchase.map(_.paymentStatus) shouldBe Some(PaymentStatus.Refunded)
      refundedPurchase.map(_.refundTs) should not be empty
    }

    "support partial refunds for wallet" in {
      implicit val trustState: TrustState =
        initTrustMock(mockProducts, mockOrders, mockBaskets, mockRefunds, mockBuildReceiptUrl)
      setupProducts(ProductWallet)
      val purchase = setupWallet()
      authorizePayment(purchase.purchaseToken)
      clearPayment(purchase)
      val refundAmount = 50L
      val source = RefundPaymentRequest.SourceData(
        comment = "Refund from TrustPaymentSupportSpec",
        reason = None,
        jsonPayload = None,
        receipt = Some(
          ReceiptData(
            goods = List(ReceiptGood(ProductWallet.name, 1, refundAmount)),
            email = Some(UserEmail),
            phone = Some(UserPhone)
          )
        )
      )

      paymentSupport.refund(UserAccountId, purchase.prId, refundAmount, source).futureValue
      val refundRecord = getSingleRefundRecordFor(purchase.prId)
      succeedRefund(refundRecord.purchaseToken, refundRecord.trustRefundId)
      val notification = buildSuccessRefundTrustNotification(purchase.purchaseToken, refundRecord.trustRefundId)
      paymentSupport.parse(AbstractNotificationSource(notification)).futureValue

      val refundRequest = paymentSupport.getRefundRequest(UserAccountId, refundRecord.refundPrId).futureValue
      refundRequest.state.map(_.stateStatus) shouldBe Some(State.StateStatuses.Valid)
      refundRequest.state.map(_.status) shouldBe Some(State.Statuses.Processed)
      val processedRefundRecord = purchaseDao.getRefundByPrId(refundRequest.id).futureValue
      processedRefundRecord.map(_.refundStatus) shouldBe Some(RefundStatus.Success)
      processedRefundRecord.flatMap(_.fiscalReceiptUrl) should not be empty
      val payment = paymentSupport.getPaymentRequest(UserAccountId, purchase.prId).futureValue
      payment.state.map(_.stateStatus) shouldBe Some(State.StateStatuses.PartlyRefunded)

      // purchase is still cleared when the refund is partial
      val refundedPurchase = purchaseDao.getByToken(purchase.purchaseToken).futureValue
      refundedPurchase.map(_.paymentStatus) shouldBe Some(PaymentStatus.Cleared)
      refundedPurchase.flatMap(_.refundTs) shouldBe empty
    }

    "cancel refund on failed status" in {
      implicit val trustState: TrustState =
        initTrustMock(mockProducts, mockOrders, mockBaskets, mockRefunds, mockBuildReceiptUrl)
      setupProducts(ProductOne, ProductTwo)
      val purchase = setupPurchase()
      authorizePayment(purchase.purchaseToken)
      clearPayment(purchase)

      paymentSupport.fullRefund(UserAccountId, purchase.prId, Some("full refund"), None).futureValue
      val refundRecord = getSingleRefundRecordFor(purchase.prId)
      failRefund(refundRecord.trustRefundId)
      val notification = buildCancelledRefundTrustNotification(purchase.purchaseToken, refundRecord.trustRefundId)
      paymentSupport.parse(AbstractNotificationSource(notification)).futureValue

      val refundRequest = paymentSupport.getRefundRequest(UserAccountId, refundRecord.refundPrId).futureValue
      refundRequest.state.map(_.stateStatus) shouldBe Some(State.StateStatuses.Cancelled)
      refundRequest.state.map(_.status) shouldBe Some(State.Statuses.Processed)
      val processedRefundRecord = purchaseDao.getRefundByPrId(refundRequest.id).futureValue
      processedRefundRecord.map(_.refundStatus) shouldBe Some(RefundStatus.Failed)
    }
  }

  "TrustPaymentSupport / operations with products" should {

    "successfully create a product" in {
      initTrustMock(mockProducts)

      val createdProduct = paymentSupport.createProduct(ProductOne).futureValue

      createdProduct shouldBe ProductOne
    }

    "fail to create a product which already exists" in {
      initTrustMock(mockProducts)
      setupProducts(ProductOne)

      val exception = paymentSupport.createProduct(ProductOne).failed.futureValue

      exception shouldBe DuplicateProductIdException("offer_raise")
    }

    "successfully fetch a product" in {
      initTrustMock(mockProducts)
      setupProducts(ProductOne)

      val product = paymentSupport.getProduct("offer_raise").futureValue

      product shouldBe ProductOne
    }

    "fail to fetch an un-existing product" in {
      initTrustMock(mockProducts)

      val exception = paymentSupport.getProduct("offer_raise").failed.futureValue

      exception shouldBe ProductNotFoundException("offer_raise")
    }

    "successfully update a product" in {
      initTrustMock(mockProducts)
      setupProducts(ProductOne)
      val patch = Product.Patch(fiscalTitle = Some("Поднятие карточки объявления"))

      val updatedProduct = paymentSupport.updateProduct("offer_raise", patch).futureValue

      updatedProduct.fiscalTitle shouldBe "Поднятие карточки объявления"
    }

    "fail to update an un-existing product" in {
      initTrustMock(mockProducts)
      val patch = Product.Patch(fiscalTitle = Some("Поднятие карточки объявления"))

      val exception = paymentSupport.updateProduct("offer_raise", patch).failed.futureValue

      exception shouldBe ProductNotFoundException("offer_raise")
    }
  }

  "TrustPaymentSupport / general exception handling" should {

    "handle unexpected responses from Trust" in {
      initTrustMock { _ =>
        stub(trustMock.getProductStatus(_: ProductId)(_: Traced)) { case (_, _) =>
          Future.failed(TrustDeserializationError("unable to parse", "{\"bad_field\": true}"))
        }
        ()
      }

      val exception = paymentSupport.getProduct("offer_raise").failed.futureValue

      assert(exception.isInstanceOf[ExternalServiceException])
      exception.asInstanceOf[ExternalServiceException].code shouldBe ExternalServiceException.Codes.UnexpectedResponse
    }

    "handle api errors from Trust" in {
      initTrustMock { _ =>
        stub(trustMock.getProductStatus(_: ProductId)(_: Traced)) { case (_, _) =>
          Future.failed(CreateProductError("unsupported_nds", None))
        }
        ()
      }

      val exception = paymentSupport.getProduct("offer_raise").failed.futureValue

      assert(exception.isInstanceOf[ExternalServiceException])
      exception.asInstanceOf[ExternalServiceException].code shouldBe ExternalServiceException.Codes.Unknown
    }

    "handle connection errors" in {
      // просто чтоб ConnectException(...) скомпилился
      val testRequest = basicRequest.get(uri"http://example.com")
      val connectionError =
        new SttpClientException.ConnectException(testRequest, new SocketException("Failed to connect"))
      initTrustMock { _ =>
        stub(trustMock.getProductStatus(_: ProductId)(_: Traced)) { case (_, _) =>
          Future.failed(connectionError)
        }
        ()
      }

      val exception = paymentSupport.getProduct("offer_raise").failed.futureValue

      assert(exception.isInstanceOf[ExternalServiceException])
      exception.asInstanceOf[ExternalServiceException].code shouldBe ExternalServiceException.Codes.IO
    }
  }

  "TrustPaymentSupport / operations with payment methods" should {

    "not access trust api when trust methods are turned off" in {
      downtimePaymentSystemService.disable(CardPaymentMethod).futureValue
      downtimePaymentSystemService.disable(YandexAccountPaymentMethod).futureValue

      val result = paymentSupport.getMethods(UserAccountId, MethodFilter.All).futureValue

      result shouldBe empty
      Mockito.verify(trustMock, Mockito.never()).getPaymentMethods(?)(?)
    }

    "get bound, enabled and regular methods" in {
      downtimePaymentSystemService.enable(YandexAccountPaymentMethod).futureValue
      val enabled = EnabledCardMethod(
        currency = "RUB",
        firmId = 12,
        paymentSystems = List("MIR", "Maestro", "MasterCard", "VISA", "VISA_ELECTRON")
      )
      val boundCard = BoundCardMethod(
        id = "card-x75ab11a2dbdd17dba2e35443",
        cardMask = "510000****1253",
        system = "MasterCard",
        regionId = 225,
        bindingTs = Instant.ofEpochMilli(1644577184056L),
        expirationYear = 2025,
        expirationMonth = 12,
        holder = "CARD HOLDER",
        expired = false,
        cardBank = Some("TINKOFF")
      )
      val yandexAccount = BoundYandexAccountMethod(
        id = "yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
        account = "w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
        balance = 9992500,
        currency = "RUB"
      )
      initTrustMock { _ =>
        stub(trustMock.getPaymentMethods(_: PassportUid)(_: Traced)) { case (_, _) =>
          Future.successful(
            PaymentMethodsResponse(
              enabledPaymentMethods = List(enabled),
              boundPaymentMethods = List(boundCard, yandexAccount)
            )
          )
        }
        stub(trustMock.getAccounts(_: PassportUid)(_: Traced)) { case (_, _) =>
          Future.successful(
            AccountsResponse(
              List(
                TrustAccount(
                  id = "w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
                  currency = "RUB"
                )
              )
            )
          )
        }
        ()
      }

      val result = paymentSupport.getMethods(UserAccountId, MethodFilter.All).futureValue

      result should contain theSameElementsAs List(
        PaymentMethod(
          ps = PaymentSystemIds.Trust,
          id = "card-x75ab11a2dbdd17dba2e35443",
          editable = false,
          preferred = Some(false),
          properties = Some(
            CardProperties(
              cddPanMask = "510000|1253",
              brand = Some(Brands.Mastercard),
              expireAt = Some(LocalDate.parse("2026-01")),
              invoiceId = None,
              cardBank = Some("TINKOFF")
            )
          ),
          restriction = None
        ),
        PaymentMethod(
          ps = PaymentSystemIds.Trust,
          id = "yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
          properties = Some(
            YandexAccountProperties(
              balance = 9992500,
              currency = "RUB"
            )
          )
        ),
        PaymentMethod(
          ps = PaymentSystemIds.Trust,
          id = CardPaymentMethod,
          editable = false,
          preferred = None,
          properties = Some(
            EnabledCardProperties(
              currency = "RUB",
              firmId = 12,
              paymentSystems = List("MIR", "Maestro", "MasterCard", "VISA", "VISA_ELECTRON")
            )
          ),
          restriction = None
        )
      )
    }

    "get freshly created bound yandex account method" in {
      downtimePaymentSystemService.disable(CardPaymentMethod).futureValue
      val yandexAccount = BoundYandexAccountMethod(
        id = "yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
        account = "w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
        balance = 9992500,
        currency = "RUB"
      )
      initTrustMock { _ =>
        stub(trustMock.getPaymentMethods(_: PassportUid)(_: Traced)) { case (_, _) =>
          Future.successful(
            PaymentMethodsResponse(
              enabledPaymentMethods = List.empty,
              boundPaymentMethods = List(yandexAccount)
            )
          )
        }
        stub(trustMock.getAccounts(_: PassportUid)(_: Traced)) { case (_, _) =>
          Future.successful(
            AccountsResponse(
              List(
                TrustAccount(
                  id = "w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
                  currency = "RUR"
                )
              )
            )
          )
        }
        stub(trustMock.createAccount(_: PassportUid, _: String)(_: Traced)) { case (_, _, _) =>
          Future.successful(
            CreateAccountResponse(
              id = "w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
              currency = "RUB",
              paymentMethodId = "yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915"
            )
          )
        }
        ()
      }

      val result = paymentSupport.getMethods(UserAccountId, MethodFilter.All).futureValue

      result should contain theSameElementsAs List(
        PaymentMethod(
          ps = PaymentSystemIds.Trust,
          id = "yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
          properties = Some(
            YandexAccountProperties(
              balance = 9992500,
              currency = "RUB"
            )
          )
        )
      )

      Mockito.verify(trustMock, Mockito.times(1)).getAccounts(?)(?)
      Mockito.verify(trustMock, Mockito.times(1)).createAccount(?, ?)(?)
    }

    "return no methods on incorrect yandex uid" in {
      downtimePaymentSystemService.disable(YandexAccountPaymentMethod).futureValue
      initTrustMock { _ =>
        stub(trustMock.getPaymentMethods(_: PassportUid)(_: Traced)) { case (_, _) =>
          Future.failed(NoAuthError("no_auth", Some("passport not found")))
        }
        ()
      }

      val result = paymentSupport.getMethods(UserAccountId, MethodFilter.All).futureValue
      result shouldBe empty
    }

    "select preferred card" in {
      downtimePaymentSystemService.disable(YandexAccountPaymentMethod).futureValue
      val cardId = TrustCardIdGen.next
      val patch = PaymentMethod.Patch(preferred = Some(true))
      val bound = BoundCardMethod(
        id = cardId,
        cardMask = "510000****1253",
        system = "MasterCard",
        regionId = 225,
        bindingTs = Instant.ofEpochMilli(1644577184056L),
        expirationYear = 2025,
        expirationMonth = 12,
        holder = "CARD HOLDER",
        expired = false,
        cardBank = None
      )
      initTrustMock { _ =>
        stub(trustMock.getPaymentMethods(_: PassportUid)(_: Traced)) { case (_, _) =>
          Future.successful(
            PaymentMethodsResponse(
              enabledPaymentMethods = List.empty,
              boundPaymentMethods = List(bound)
            )
          )
        }
        ()
      }

      paymentSupport.updateMethod(UserAccountId, cardId, patch).futureValue

      val methods = paymentSupport.getMethods(UserAccountId, MethodFilter.All).futureValue
      val boundCardMethod = methods.find(_.id == cardId)
      boundCardMethod.flatMap(_.preferred) shouldBe Some(true)
    }

    "not allow to update other methods" in {
      val methodId = "trust_web_page"
      val patch = PaymentMethod.Patch(preferred = Some(true))

      val exception = paymentSupport.updateMethod(UserAccountId, methodId, patch).failed.futureValue

      assert(exception.isInstanceOf[IllegalArgumentException])
    }
  }

  "TrustPaymentSupport / operations with recurrent payments" should {

    "correctly handle recurrent payment request" in {

      def hasAfsParam(request: CreateBasketRequest): Boolean =
        request.afsParams.exists(_.get("request").contains("mit"))

      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val source = buildRequestSourceForApiPayment()

      val form = paymentSupport.request(source.account, BoundCardId, source).futureValue

      val purchaseOpt = purchaseDao.getById(form.id).futureValue
      purchaseOpt should not be empty
      val purchase = purchaseOpt.get
      purchase.trustPaymentId should not be empty
      purchase.paymentStatus shouldBe Started
      purchase.yandexUid shouldBe UserPassportUid
      purchase.paymentMode shouldBe ApiPaymentMode
      purchase.paymentMethodId shouldBe Some(BoundCardId)
      purchase.startTs should not be empty

      assert(form.isInstanceOf[TrustWebForm])
      form.asInstanceOf[TrustWebForm].purchaseToken shouldBe purchase.purchaseToken
      form.asInstanceOf[TrustWebForm].url shouldBe empty

      Mockito.verify(trustMock, Mockito.times(1)).createOrder(?, ?)(?)
      Mockito
        .verify(trustMock, Mockito.times(1))
        .createBasket(ArgumentMatchers.argThat(hasAfsParam), ?)(?)
      Mockito.verify(trustMock, Mockito.times(1)).basketStartPayment(?)(?)
    }

    "fail on incorrect method id" in {
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val source = buildRequestSourceForApiPayment()

      val exception = paymentSupport.request(source.account, "card-xxx", source).failed.futureValue

      assert(exception.isInstanceOf[IllegalArgumentException])
    }
  }

  "TrustPaymentSupport / operations with composite payments" should {

    "correctly handle composite payment with plus bonus and card" in {
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val withdrawAmount = TotalAmount - 100
      val source = buildRequestSourceForTrustWebPage(bonus = Some(PlusWithdraw(withdrawAmount)))

      val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue

      val purchaseOpt = purchaseDao.getById(form.id).futureValue
      purchaseOpt.flatMap(_.plusWithdrawAmount) shouldBe Some(withdrawAmount)

      val basket = trustMock.getBasket(form.asInstanceOf[TrustWebForm].purchaseToken).futureValue
      val orderOne = basket.orders.find(_.productId == ProductOne.id).get
      val orderTwo = basket.orders.find(_.productId == ProductTwo.id).get
      val expectedMarkup = Map(
        orderOne.orderId -> OrderMarkup(yandexAccount = BigDecimal(10.00), card = BigDecimal(0.00)),
        orderTwo.orderId -> OrderMarkup(yandexAccount = BigDecimal(4.00), card = BigDecimal(1.00))
      )
      basket.paymentMarkup shouldBe Some(BasketMarkup(expectedMarkup))
    }

    "verify withdraw amount is less or equal total amount" in {
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val source = buildRequestSourceForTrustWebPage(bonus = Some(PlusWithdraw(TotalAmount + 1)))

      val exception = paymentSupport.request(source.account, CardPaymentMethod, source).failed.futureValue

      exception shouldBe an[IllegalArgumentException]
    }

    "markup basket for withdraw amount" in {
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val topupAmount = TotalAmount - 100
      val source = buildRequestSourceForTrustWebPage(bonus = Some(PlusTopup(topupAmount)))
      val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue

      val withdrawAmount = 100
      val request = PaymentMarkupRequest(PlusWithdraw(withdrawAmount))
      paymentSupport.markupPayment(source.account, form.id, request).futureValue

      val purchaseOpt = purchaseDao.getById(form.id).futureValue
      purchaseOpt.flatMap(_.plusWithdrawAmount) shouldBe Some(withdrawAmount)

      val basket = trustMock.getBasket(form.asInstanceOf[TrustWebForm].purchaseToken).futureValue
      val orderOne = basket.orders.find(_.productId == ProductOne.id).get
      val orderTwo = basket.orders.find(_.productId == ProductTwo.id).get
      val expectedMarkup = Map(
        orderOne.orderId -> OrderMarkup(yandexAccount = BigDecimal(1.00), card = BigDecimal(9.00)),
        orderTwo.orderId -> OrderMarkup(yandexAccount = BigDecimal(0.00), card = BigDecimal(5.00))
      )
      basket.paymentMarkup shouldBe Some(BasketMarkup(expectedMarkup))
    }

    "clear basket markup for topup" in {
      initTrustMock(mockProducts, mockOrders, mockBaskets)
      setupProducts(ProductOne, ProductTwo)
      val withdrawAmount = TotalAmount - 100
      val source = buildRequestSourceForTrustWebPage(bonus = Some(PlusWithdraw(withdrawAmount)))
      val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue

      val topupAmount = 100
      val request = PaymentMarkupRequest(PlusTopup(topupAmount))
      paymentSupport.markupPayment(source.account, form.id, request).futureValue

      val purchaseOpt = purchaseDao.getById(form.id).futureValue
      purchaseOpt.flatMap(_.plusTopupAmount) shouldBe Some(topupAmount)

      val basket = trustMock.getBasket(form.asInstanceOf[TrustWebForm].purchaseToken).futureValue
      val orderOne = basket.orders.find(_.productId == ProductOne.id).get
      val orderTwo = basket.orders.find(_.productId == ProductTwo.id).get
      val expectedMarkup = Map(
        orderOne.orderId -> OrderMarkup(yandexAccount = BigDecimal(0.00), card = BigDecimal(10.00)),
        orderTwo.orderId -> OrderMarkup(yandexAccount = BigDecimal(0.00), card = BigDecimal(5.00))
      )
      basket.paymentMarkup shouldBe Some(BasketMarkup(expectedMarkup))
    }
  }

  private def setupProducts(products: Product*) = Future
    .traverse(products) { case Product(id, name, fiscalTitle, fiscalNds) =>
      trustMock.createProduct(CreateProductRequest(id, name, fiscalTitle, ToTrustTaxType(fiscalNds)))
    }
    .futureValue

  private def setupPurchase() = {
    val source = buildRequestSourceForTrustWebPage()
    val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue
    purchaseDao.getById(form.id).futureValue.get
  }

  private def setupWallet() = {
    val source = buildWalletRequestSourceForTrustWebPage()
    val form = paymentSupport.request(source.account, CardPaymentMethod, source).futureValue
    purchaseDao.getById(form.id).futureValue.get
  }

  private def authorizePayment(token: PurchaseToken)(implicit trustState: TrustState) = {
    val notification = buildSuccessTrustNotification(token)
    succeedPayment(token)
    paymentSupport.parse(AbstractNotificationSource(notification)).futureValue
  }

  private def failPayment(token: PurchaseToken)(implicit trustState: TrustState) = {
    val notification = buildCancelledTrustNotification(token)
    cancelPayment(token, "not_enough_funds")
    paymentSupport.parse(AbstractNotificationSource(notification)).futureValue
  }

  private def clearPayment(purchase: PurchaseRecord): Unit =
    paymentHelper.clearPayment(purchase.purchaseToken).map(_ => ()).futureValue

  private def buildRequestSourceForTrustWebPage(
      externalId: Option[ExternalTransactionId] = None,
      bonus: Option[PlusBonus] = None) =
    paymentRequestSourceGen().next
      .copy(
        account = UserAccountId,
        amount = TotalAmount,
        optReceiptData = Some(
          ReceiptData(
            goods = List(
              ReceiptGood(ProductOne.name, 1, 1000),
              ReceiptGood(ProductTwo.name, 1, 500)
            ),
            email = Some(UserEmail),
            phone = Some(UserPhone)
          )
        ),
        payload = Payload.Json(
          JsObject(
            Map(
              "domain" -> JsString("autoru"),
              "transaction" -> JsString(externalId.getOrElse(RequestIdGen.next))
            )
          )
        ),
        options = Options(id = externalId, defaultURL = Some(DefaultUrl)),
        context = Context(Targets.Purchase),
        payGateContext = Some(
          TrustContext(
            paymentMethodData = WebPayment,
            orders = List(
              TrustContext.Order(ProductOne.id, 1, 1000),
              TrustContext.Order(ProductTwo.id, 1, 500)
            ),
            bonus = bonus
          )
        ),
        yandexUid = Some(UserPassportUid)
      )

  private def buildWalletRequestSourceForTrustWebPage() =
    paymentRequestSourceGen().next
      .copy(
        account = UserAccountId,
        amount = TotalAmount,
        optReceiptData = Some(
          ReceiptData(
            goods = List(ReceiptGood(ProductWallet.name, 1, TotalAmount)),
            email = Some(UserEmail),
            phone = Some(UserPhone)
          )
        ),
        payload = Payload.Empty,
        options = Options(defaultURL = Some(DefaultUrl)),
        context = Context(Targets.Wallet),
        payGateContext = Some(
          TrustContext(
            paymentMethodData = WebPayment,
            orders = List(TrustContext.Order(ProductWallet.id, 1, TotalAmount)),
            bonus = None
          )
        ),
        yandexUid = Some(UserPassportUid)
      )

  private def buildRequestSourceForApiPayment(externalId: Option[ExternalTransactionId] = None) =
    paymentRequestSourceGen().next
      .copy(
        account = UserAccountId,
        amount = 1000,
        optReceiptData = Some(
          ReceiptData(
            goods = List(
              ReceiptGood(ProductOne.name, 1, 1000)
            ),
            email = Some(UserEmail),
            phone = Some(UserPhone)
          )
        ),
        payload = Payload.Json(
          JsObject(
            Map(
              "domain" -> JsString("autoru"),
              "transaction" -> JsString(externalId.getOrElse(RequestIdGen.next))
            )
          )
        ),
        options = Options(id = externalId, defaultURL = Some(DefaultUrl)),
        context = Context(Targets.Purchase),
        payGateContext = Some(
          TrustContext(
            paymentMethodData = ApiPayment,
            orders = List(
              TrustContext.Order(ProductOne.id, 1, 1000)
            ),
            bonus = None
          )
        ),
        yandexUid = Some(UserPassportUid)
      )

  private def buildSuccessTrustNotification(token: PurchaseToken) = PaymentTrustNotification(
    status = PaymentNotificationStatus.Success,
    statusCode = "success",
    trustPaymentId = hexString().next,
    purchaseToken = token,
    serviceId = 1178,
    serviceOrderId = "179479854",
    subsUntilTs = None,
    bindingResult = Some(BindingResult.Success),
    paymentMethod = "card-x75ab11a2dbdd17dba2e35443",
    userPhone = UserPhone,
    userEmail = UserEmail,
    rrn = "359800",
    subsStage = None
  )

  private def buildCancelledTrustNotification(token: PurchaseToken) = PaymentTrustNotification(
    status = PaymentNotificationStatus.Cancelled,
    statusCode = "not_enough_funds",
    trustPaymentId = hexString().next,
    purchaseToken = token,
    serviceId = 1178,
    serviceOrderId = "179479854",
    subsUntilTs = None,
    bindingResult = Some(BindingResult.Failure),
    paymentMethod = "card-x75ab11a2dbdd17dba2e35443",
    userPhone = UserPhone,
    userEmail = UserEmail,
    rrn = "None",
    subsStage = None
  )

  private def buildSuccessRefundTrustNotification(
      token: PurchaseToken,
      refundId: TrustRefundId) = RefundTrustNotification(
    status = RefundNotificationStatus.Success,
    statusCode = None,
    trustPaymentId = hexString().next,
    trustRefundId = refundId,
    purchaseToken = token
  )

  private def buildCancelledRefundTrustNotification(
      token: PurchaseToken,
      refundId: TrustRefundId) = RefundTrustNotification(
    status = RefundNotificationStatus.Failed,
    statusCode = None,
    trustPaymentId = hexString().next,
    trustRefundId = refundId,
    purchaseToken = token
  )

  private def getSingleRefundRecordFor(prId: PaymentRequestId) = {
    val refunds = paymentSupport.getRefundRequestsFor(UserAccountId, prId).futureValue
    refunds.size shouldBe 1
    val refundRecord = purchaseDao.getRefundByPrId(refunds.head.id).futureValue
    refundRecord should not be empty
    refundRecord.get
  }
}

object TrustPaymentSupportSpec {

  val UserAccountId = "user:4227"
  val UserPassportUid = 22296196L
  val UserEmail = "banker@yandex.ru"
  val UserPhone = "+79991112233"
  val BoundCardId = "card-x75ab11a2dbdd17dba2e35443"

  val TotalAmount = 1500
  val DefaultUrl = "http://auto.ru"

  val ProductOne = Product("offer_raise", "Offer raise", "Поднятие объявления", TaxTypes.Nds20)
  val ProductTwo = Product("car_report", "Car report", "Отчёт об автомобиле", TaxTypes.Nds20)
  val ProductWallet = Product("wallet", "Wallet", "Пополнение кошелька", TaxTypes.`Nds20/120`)
}
