package ru.yandex.vertis.billing.trust

import io.circe._
import io.circe.parser._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.AsyncSpecBase
import ru.yandex.vertis.billing.trust.TrustApiSpec._
import ru.yandex.vertis.billing.trust.exceptions.TrustException.{CreateTopupError, _}
import ru.yandex.vertis.billing.trust.impl.TrustApiImpl.PRODUCT_NOT_FOUND_ERROR_CODE
import ru.yandex.vertis.billing.trust.model._
import sttp.client3.Request
import sttp.client3.testing.{SttpBackendStub, _}
import sttp.model.{Method, StatusCode}

import java.time.Instant
import scala.concurrent.Future

trait TrustApiSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  protected val productType = "prod"
  protected val productName = "Wallet"
  protected val productFiscalTitle = "Пополнение кошелька"
  protected val productFiscalNds = TaxType.Nds18

  protected val orderPrice = BigDecimal("10.00")
  protected val orderQty = 1

  protected val refundPurchaseToken = "purchaseToken"
  protected val refundComment = "Refund from Banker API"
  protected val refundDelta = BigDecimal("1.0")

  protected def trustApi: TrustApi

  protected def makeTrustApi(backend: SttpBackendStub[Future, Any]): TrustApi

  private val sttpBackend = SttpBackendStub.asynchronousFuture

  implicit class RichRequest[A, B](request: Request[A, B]) {
    def pathEndsWith(suffix: String): Boolean = request.uri.path.mkString("/").endsWith(suffix)
    def bodyAsJson: Json = parse(request.forceBodyAsString).toTry.get
  }

  implicit class RichString(string: String) {
    def parseJson: Json = parse(string).toTry.get
  }

  "TrustApi / createProduct" should {

    "successfully create new product" in {
      createProduct(CreateProductSuccessId).futureValue
    }

    "fail with unknown trust logical error" in {
      val result = createProduct(CreateProductUnknownFailureId)
      result.failed.futureValue shouldBe CreateProductError("unknown_error", None)
    }

    "fail with trust logical error" in {
      val result = createProduct(CreateProductConcreteFailureId)
      result.failed.futureValue shouldBe CreateProductError("partner_not_found", Some("partner not found"))
    }

    "fail with deserialization error" in {
      val result = createProduct(CreateProductDeserializationErrorId)
      result.failed.futureValue should matchPattern {
        case TrustDeserializationError(_, "{\"bad_field\": \"something wrong\"}") =>
      }
    }
  }

  "TrustApi / updateProduct" should {

    "successfully updateProduct new product" in {
      updateProduct(UpdateProductSuccessId).futureValue
    }

    "fail with unknown trust logical error" in {
      val result = updateProduct(UpdateProductUnknownFailureId)
      result.failed.futureValue shouldBe UpdateProductError("unknown_error", None)
    }

    "fail with trust logical error" in {
      val result = updateProduct(UpdateProductConcreteFailureId)
      result.failed.futureValue shouldBe UpdateProductError("partner_not_found", Some("partner not found"))
    }

    "fail with deserialization error" in {
      val result = updateProduct(UpdateProductDeserializationErrorId)
      result.failed.futureValue should matchPattern {
        case TrustDeserializationError(_, "{\"bad_field\": \"something wrong\"}") =>
      }
    }
  }

  "TrustApi / getProductStatus" should {

    "successfully retrieve existing product" in {
      val result = getProductStatus(GetProductSuccessId)
      result.futureValue shouldBe ProductResponse(
        productId = "get_product_success",
        productType = Some(productType),
        name = productName,
        fiscalTitle = productFiscalTitle,
        fiscalNds = productFiscalNds
      )
    }

    "fail with product not found error" in {
      val result = getProductStatus(GetProductNotFoundFailureId)
      result.failed.futureValue shouldBe ProductNotFoundError(GetProductNotFoundFailureId, PRODUCT_NOT_FOUND_ERROR_CODE)
    }

    "fail with unknown get product error" in {
      val result = getProductStatus(GetProductUnknownFailureId)
      result.failed.futureValue shouldBe GetProductError("unknown_error", Some("invalid_request"))
    }

    "fail when some field is missing in a response" in {
      val result = getProductStatus(GetProductFieldMissingFailureId)
      result.failed.futureValue shouldBe TrustFieldExpectedError("product_id")
    }
  }

  "TrustApi / createOrder" should {
    "succeed on correct input" in {
      val result = createOrder(CreateOrderSuccessId)
      result.futureValue shouldBe CreateOrderSuccess("3172585390", CreateOrderSuccessId)
    }

    "fail on concrete error" in {
      val result = createOrder(CreateOrderConcreteFailureId)
      result.failed.futureValue shouldBe ProductNotFoundError(
        CreateOrderConcreteFailureId,
        PRODUCT_NOT_FOUND_ERROR_CODE
      )
    }

    "fail on unknown error" in {
      val result = createOrder(CreateOrderUnknownFailureId)
      result.failed.futureValue shouldBe CreateOrderError(
        "unknown_error",
        Some("AttributeError: 'NoneType' object has no attribute 'order_check_access'")
      )
    }

    "fail on deserialization error" in {
      val result = createOrder(CreateOrderDeserializationErrorId)
      result.failed.futureValue should matchPattern {
        case TrustDeserializationError(_, "{\"bad_field\": \"something wrong\"}") =>
      }
    }
  }

  "TrustApi / getOrderStatus" should {
    "succeed on correct input" in {
      val result = getOrderStatus(GetOrderStatusSuccessId)
      result.futureValue shouldBe OrderResponse(
        "app",
        "3000248777",
        "1482243456.000",
        "0.00",
        GetOrderStatusSuccessId,
        "skydreamer_partner",
        "735706664784558",
        List(List("RUB", "100.00"))
      )
    }

    "fail on concrete error" in {
      val result = getOrderStatus(GetOrderStatusConcreteFailureId)
      result.failed.futureValue shouldBe GetOrderError(
        "order_not_found",
        Some(s"Unable to find order for orderId $GetOrderStatusConcreteFailureId'")
      )
    }

    "fail on unknown error" in {
      val result = getOrderStatus(GetOrderStatusUnknownFailureId)
      result.failed.futureValue shouldBe GetOrderError(
        "unknown_error",
        Some("AttributeError: 'NoneType' object has no attribute 'order_check_access'")
      )
    }
    "fail on deserialization error" in {
      val result = getOrderStatus(GetOrderStatusDeserializationErrorId)
      result.failed.futureValue should matchPattern {
        case TrustDeserializationError(_, "{\"bad_field\": \"something wrong\"}") =>
      }
    }

  }

  "TrustApi / createBasket" should {

    "successfully create new basket" in {
      val result = createBasket(CreateBasketSuccessId)
      result.futureValue shouldBe CreateBasketSuccess(
        trustPaymentId = "test_trust_payment_id",
        purchaseToken = "test_purchase_token"
      )
    }

    "fail on error" in {
      val result = createBasket(CreateBasketFailureId)
      result.failed.futureValue shouldBe CreateBasketError("order_not_found", Some("order not found"))
    }

    "fail on deserialization error" in {
      val result = createBasket(CreateBasketDeserializationErrorId)
      result.failed.futureValue should matchPattern {
        case TrustDeserializationError(_, "{\"bad_field\": \"something wrong\"}") =>
      }
    }
  }

  "TrustApi / getBasket" should {

    "successfully get new basket" in {
      val result = getBasket(GetBasketSuccessNotStartedId)
      result.futureValue shouldBe BasketResponse(
        purchaseToken = "test_purchase_token",
        amount = 99.50,
        currency = "RUB",
        orders = List(
          PaymentOrder(
            orderId = "177829875",
            orderTs = Instant.ofEpochMilli(1643646814085L),
            productId = "test_product",
            productType = "app",
            productName = "Test product",
            origAmount = 99.5,
            paidAmount = 0.0,
            currentQty = 0.0
          )
        ),
        paymentTimeout = 1200000,
        paymentStatus = PaymentStatus.NotStarted,
        yandexUid = 21123456L,
        paymentMode = "web_payment"
      )
    }

    "successfully get basket started" in {
      val basket = getBasket(GetBasketSuccessStartedId).futureValue
      basket.paymentStatus shouldBe PaymentStatus.Started
      basket.paymentUrl shouldBe Some("https://trust-test.yandex.ru/web/payment?purchase_token=test_purchase_token")
      basket.startTs shouldBe Some(Instant.ofEpochMilli(1644595207397L))
    }

    "successfully get basket started with 3ds" in {
      val basket = getBasket(GetBasketSuccess3dsStartedId).futureValue
      basket.paymentStatus shouldBe PaymentStatus.`3dsStarted`
      basket.paymentUrl shouldBe Some("https://trust-test.yandex.ru/web/payment?purchase_token=test_purchase_token")
      basket.startTs shouldBe Some(Instant.ofEpochMilli(1644595207397L))
    }

    "successfully get basket authorized" in {
      val basket = getBasket(GetBasketSuccessAuthorizedId).futureValue
      basket.paymentStatus shouldBe PaymentStatus.Authorized
      basket.paymentTs shouldBe Some(Instant.ofEpochMilli(1644845241000L))
    }

    "successfully get basket not authorized" in {
      val basket = getBasket(GetBasketSuccessNotAuthorizedId).futureValue
      basket.paymentStatus shouldBe PaymentStatus.NotAuthorized
      basket.cancelTs shouldBe Some(Instant.ofEpochMilli(1644845462000L))
      basket.authErrorCode shouldBe Some("not_enough_funds")
    }

    "successfully get basket cleared" in {
      val basket = getBasket(GetBasketSuccessClearedId).futureValue
      basket.paymentStatus shouldBe PaymentStatus.Cleared
      basket.clearTs shouldBe Some(Instant.ofEpochMilli(1645175534766L))
    }

    "successfully get basket refunded" in {
      val basket = getBasket(GetBasketSuccessRefundedId).futureValue
      basket.paymentStatus shouldBe PaymentStatus.Refunded
      basket.refunds shouldBe List(
        BasketRefund(
          trustRefundId = "624d74bc910d39255ef6817d",
          amount = BigDecimal("100.00"),
          description = "Refund from localhost. 21123456 performed this refund",
          createTs = Instant.ofEpochMilli(1649243324052L),
          confirmTs = Some(Instant.ofEpochMilli(1649254790000L))
        )
      )
    }

    "successfully get basket with markup" in {
      val basket = getBasket(GetBasketSuccessWithMarkupId).futureValue
      basket.paymentMarkup shouldBe Some(
        BasketMarkup(
          Map(
            "214508982" -> OrderMarkup(yandexAccount = 10, card = 90),
            "214509135" -> OrderMarkup(yandexAccount = 5, card = 45)
          )
        )
      )
    }

    "fail on error" in {
      val result = getBasket(GetBasketConcreteFailureId)
      result.failed.futureValue shouldBe GetBasketError("order_not_found", Some("order not found"))
    }

    "fail on deserialization error" in {
      val result = getBasket(GetBasketDeserializationErrorId)
      result.failed.futureValue should matchPattern {
        case TrustDeserializationError(_, "{\"bad_field\": \"something wrong\"}") =>
      }
    }

  }

  "TrustApi / startBasket" should {

    "successfully start new basket" in {
      val result = startBasket(StartBasketSuccessId)
      result.futureValue shouldBe BasketResponse(
        purchaseToken = "test_purchase_token",
        amount = 99.50,
        currency = "RUB",
        orders = List(
          PaymentOrder(
            orderId = "177829875",
            orderTs = Instant.ofEpochMilli(1643646814085L),
            productId = "test_product",
            productType = "app",
            productName = "Test product",
            origAmount = 99.5,
            paidAmount = 0.0,
            currentQty = 0.0
          )
        ),
        paymentTimeout = 1200000,
        paymentStatus = PaymentStatus.Started,
        paymentUrl = Some("some url"),
        startTs = Some(Instant.ofEpochMilli(1645091782989L)),
        yandexUid = 21123456L,
        paymentMode = "web_payment"
      )
    }

    "fail on error" in {
      val result = startBasket(StartBasketFailureId)
      result.failed.futureValue shouldBe StartBasketError("order_not_found", Some("order not found"))
    }

    "fail on deserialization error" in {
      val result = startBasket(StartBasketDeserializationErrorId)
      result.failed.futureValue should matchPattern {
        case TrustDeserializationError(_, "{\"bad_field\": \"something wrong\"}") =>
      }
    }
  }

  "TrustApi / clearBasket" should {

    "successfully clear basket" in {
      val result = trustApi.clearBasket(ClearBasketSuccessId)
      result.futureValue.shouldBe(())
    }

    "fail to clear basket" in {
      val result = trustApi.clearBasket(ClearBasketFailureId)
      result.failed.futureValue shouldBe ClearBasketError("invalid_state", None)
    }
  }

  "TrustApi / unholdBasket" should {

    "successfully unhold basket" in {
      val result = trustApi.unholdBasket(UnholdBasketSuccessId)
      result.futureValue.shouldBe(())
    }

    "fail to unhold basket" in {
      val result = trustApi.unholdBasket(UnholdBasketFailureId)
      result.failed.futureValue shouldBe UnholdBasketError("invalid_state", None)
    }
  }

  "TrustApi / createRefund" should {

    "successfully create new refund" in {
      val result = createRefund(CreateRefundSuccessOrderId)
      result.futureValue shouldBe "create_refund_success_id"
    }

    "fail on error" in {
      val result = createRefund(CreateRefundFailureOrderId)
      result.failed.futureValue shouldBe CreateRefundError("unknown_error", Some("PaymentOrderNotFound"))
    }
  }

  "TrustApi / startRefund" should {

    "successfully start a refund" in {
      val result = trustApi.startRefund(StartRefundSuccessId)
      result.futureValue shouldBe ()
    }

    "fail to find refund" in {
      val result = trustApi.startRefund(StartRefundNotFoundId)
      result.failed.futureValue shouldBe RefundNotFoundError("start_refund_not_found_id", "trust_refund_id_not_found")
    }

    "fail on error" in {
      val result = trustApi.startRefund(StartRefundFailureId)
      result.failed.futureValue shouldBe StartRefundError("unknown_failure", None)
    }
  }

  "TrustApi / getRefund" should {

    "successfully get refund in completed status" in {
      val refund = trustApi.getRefund(GetRefundSuccessId).futureValue
      refund.status shouldBe RefundStatus.Success
      refund.statusDescription shouldBe "refund sent to payment system"
      refund.fiscalReceiptUrl should not be empty
    }

    "successfully get refund in failed status" in {
      val refund = trustApi.getRefund(GetRefundFailedId).futureValue
      refund.status shouldBe RefundStatus.Failed
      refund.statusDescription shouldBe "failed to refund"
      refund.fiscalReceiptUrl shouldBe empty
    }

    "fail to find refund" in {
      val result = trustApi.getRefund(GetRefundNotFoundId)
      result.failed.futureValue shouldBe RefundNotFoundError("get_refund_not_found_id", "trust_refund_id_not_found")
    }
  }

  "TrustApi / getPaymentMethods" should {

    "successfully retrieve payment methods" in {
      val expectedEnabledPaymentMethods = List(
        EnabledCardMethod(
          currency = "RUB",
          firmId = 12,
          paymentSystems = List(
            "MIR",
            "Maestro",
            "MasterCard",
            "VISA",
            "VISA_ELECTRON"
          )
        ),
        EnabledCompositeMethod(
          currency = "RUB",
          firmId = 12
        ),
        EnabledYandexAccountTopupMethod(
          currency = "RUB",
          firmId = 12
        ),
        EnabledYandexAccountWithdrawMethod(
          currency = "RUB",
          firmId = 12
        )
      )
      val expectedBoundPaymentMethods = List(
        BoundCardMethod(
          id = "card-x75ab11a2dbdd17dba2e35443",
          cardMask = "510000****1253",
          system = "MasterCard",
          regionId = 225,
          bindingTs = Instant.ofEpochMilli(1644577184056L),
          expirationYear = 2025,
          expirationMonth = 4,
          holder = "CARD HOLDER",
          expired = false,
          cardBank = Some("RBS BANK (ROMANIA), S.A.")
        ),
        BoundYandexAccountMethod(
          id = "yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
          account = "w/30b153cc-8e30-58e2-8d1a-1095bc49b915",
          balance = 9992500,
          currency = "RUB"
        )
      )
      val result = trustApi.getPaymentMethods(GetPaymentMethodsSuccessYandexUid.toLong).futureValue
      result.enabledPaymentMethods shouldBe expectedEnabledPaymentMethods
      result.boundPaymentMethods shouldBe expectedBoundPaymentMethods
    }

    "fail with auth error when yandex uid is invalid" in {
      val result = trustApi.getPaymentMethods(GetPaymentMethodsNoAuthYandexUid.toLong)
      result.failed.futureValue shouldBe NoAuthError("no_auth", Some("passport not found"))
    }

    "wrap Tvm error" in {
      val result = trustApi.getPaymentMethods(GetPaymentMethodTvmErrorUid.toLong)
      result.failed.futureValue shouldBe TrustTvmAuthException("{'status': <Status.Malformed: 5>}")
    }

    "fail with unknown technical error" in {
      val result = trustApi.getPaymentMethods(GetPaymentMethodsFailureYandexUid.toLong)
      result.failed.futureValue shouldBe GetPaymentMethodsError("technical_error", Some("Server internal error"))
    }
  }

  "TrustApi / getAccounts" should {

    "successfully retrieve accounts" in {
      val expected = List(
        TrustAccount("1", "RUB"),
        TrustAccount("2", "RUR")
      )
      val result = trustApi.getAccounts(GetAccountsSuccessYandexUid.toLong).futureValue
      result.accounts shouldBe expected
    }

    "successfully retrieve empty accounts list" in {
      val result = trustApi.getAccounts(GetAccountsSuccessEmptyYandexUid.toLong).futureValue
      result.accounts.size shouldBe 0
    }

    "fail with unknown technical error" in {
      val result = trustApi.getAccounts(GetAccountsFailureYandexUid.toLong).failed.futureValue
      result shouldBe GetAccountsError("technical_error", Some("Server internal error"))
    }
  }

  "TrustApi / createAccount" should {

    "successfully create account" in {
      val expected = CreateAccountResponse("1", "RUB", "yandex_account-1")
      val result = trustApi.createAccount(CreateAccountSuccessYandexUid.toLong, "RUB").futureValue
      result shouldBe expected
    }

    "successfully retrieve already created account" in {
      val expected = CreateAccountResponse("2", "RUR", "yandex_account-2")
      val result = trustApi.createAccount(CreateAccountSuccessAlreadyExistsYandexUid.toLong, "RUR").futureValue
      result shouldBe expected
    }

    "fail with unknown technical error" in {
      val result = trustApi.createAccount(CreateAccountFailureYandexUid.toLong, "RUB").failed.futureValue
      result shouldBe CreateAccountError("technical_error", Some("Server internal error"))
    }
  }

  "TrustApi / createTopup" should {

    "successfully create topup" in {
      val trustBackend =
        sttpBackend
          .whenRequestMatches { req =>
            req.pathEndsWith("trust-payments/v2/topup/") &&
            req.uri.params.get("show_trust_payment_id").contains("true") &&
            req.method == Method.POST &&
            req.header("X-Uid").contains("222") &&
            req.bodyAsJson == """{
                "currency": "RUB",
                "amount": 1000,
                "product_id": "some_product",
                "paymethod_id": "yandex_account-w/111"
            }""".parseJson
          }
          .thenRespond("""{
            "status": "success",
            "status_code": "payment_created",
            "purchase_token": "93df8497533e7fa941bbf002315640fc",
            "trust_payment_id": "62960107910d391ca253cee4"
          }""")

      makeTrustApi(trustBackend)
        .createTopup(CreateTopupRequest("RUB", 1000, "some_product", "yandex_account-w/111"), uid = 222)
        .futureValue shouldBe CreateTopupSuccess(
        purchaseToken = "93df8497533e7fa941bbf002315640fc",
        trustPaymentId = "62960107910d391ca253cee4"
      )
    }

    "fail on error response" in {
      val trustBackend =
        sttpBackend.whenAnyRequest
          .thenRespondWithCode(
            StatusCode.NotFound,
            """{
              "status": "error",
              "status_code": "Not found",
              "status_desc": "Product no found"
            }"""
          )

      makeTrustApi(trustBackend)
        .createTopup(CreateTopupRequest("RUB", 1000, "some_product", "yandex_account-w/111"), uid = 222)
        .failed
        .futureValue shouldBe CreateTopupError("Not found", Some("Product no found"))
    }

    "fail with unknown technical error" in {
      val trustBackend = sttpBackend.whenAnyRequest.thenRespond("unexpected response")

      makeTrustApi(trustBackend)
        .createTopup(CreateTopupRequest("RUB", 1000, "some_product", "yandex_account-w/111"), uid = 222)
        .failed
        .futureValue should matchPattern {
        case ex: TrustDeserializationError if ex.body.contains("unexpected response") =>
      }
    }
  }

  "TrustApi / startTopup" should {

    "successfully start topup" in {
      val trustBackend =
        sttpBackend
          .whenRequestMatches { req =>
            req.pathEndsWith("trust-payments/v2/topup/a6a9ae143205813a32c9c59167b6409e/start") &&
            req.method == Method.POST &&
            req.header("X-Uid").contains("222")
          }
          .thenRespond("""{
            "status": "success"
          }""")

      makeTrustApi(trustBackend)
        .startTopup("a6a9ae143205813a32c9c59167b6409e", uid = 222)
        .futureValue
    }

    "fail on error response" in {
      val trustBackend =
        sttpBackend.whenAnyRequest
          .thenRespondWithCode(
            StatusCode.NotFound,
            """{
              "status": "error",
              "status_code": "Not found",
              "status_desc": "Purchase token no found"
            }"""
          )

      makeTrustApi(trustBackend)
        .startTopup("a6a9ae143205813a32c9c59167b6409e", uid = 222)
        .failed
        .futureValue shouldBe StartTopupError("Not found", Some("Purchase token no found"))
    }

    "fail with unknown technical error" in {
      val trustBackend = sttpBackend.whenAnyRequest.thenRespond("unexpected response")

      makeTrustApi(trustBackend)
        .startTopup("a6a9ae143205813a32c9c59167b6409e", uid = 222)
        .failed
        .futureValue should matchPattern {
        case ex: TrustDeserializationError if ex.body.contains("unexpected response") =>
      }
    }
  }

  "TrustApi / getTopup" should {

    "successfully get topup" in {
      val trustBackend =
        sttpBackend
          .whenRequestMatches { req =>
            req.pathEndsWith("trust-payments/v2/topup/a6a9ae143205813a32c9c59167b6409e") &&
            req.uri.params.get("show_trust_payment_id").contains("true") &&
            req.method == Method.GET &&
            req.header("X-Uid").contains("222")
          }
          .thenRespond("""{
            "status": "success",
            "trust_payment_id": "62960107910d391ca253cee4",
            "uid": "4090064790",
            "amount": "1000.00",
            "currency": "RUB",
            "payment_status": "cleared",
            "purchase_token": "a6a9ae143205813a32c9c59167b6409e",
            "start_ts": "1653955555.000",
            "clear_ts": "1653977777.000",
            "payment_ts": "1653944444.000",
            "cancel_ts": "1653988888.000"
          }""")

      makeTrustApi(trustBackend)
        .getTopup("a6a9ae143205813a32c9c59167b6409e", uid = 222)
        .futureValue shouldBe
        GetTopupSuccess(
          purchaseToken = "a6a9ae143205813a32c9c59167b6409e",
          trustPaymentId = "62960107910d391ca253cee4",
          paymentStatus = PaymentStatus.Cleared,
          yandexUid = 4090064790L,
          amount = BigDecimal("1000.00"),
          currency = "RUB",
          paymentTs = Some(Instant.ofEpochMilli(1653944444000L)),
          startTs = Some(Instant.ofEpochMilli(1653955555000L)),
          clearTs = Some(Instant.ofEpochMilli(1653977777000L)),
          cancelTs = Some(Instant.ofEpochMilli(1653988888000L))
        )
    }

    "fail on error response" in {
      val trustBackend =
        sttpBackend.whenAnyRequest
          .thenRespondWithCode(
            StatusCode.NotFound,
            """{
              "status": "error",
              "status_code": "Not found",
              "status_desc": "Purchase token no found"
            }"""
          )

      makeTrustApi(trustBackend)
        .getTopup("a6a9ae143205813a32c9c59167b6409e", uid = 222)
        .failed
        .futureValue shouldBe GetTopupError("Not found", Some("Purchase token no found"))
    }

    "fail with unknown technical error" in {
      val trustBackend = sttpBackend.whenAnyRequest.thenRespond("unexpected response")

      makeTrustApi(trustBackend)
        .getTopup("a6a9ae143205813a32c9c59167b6409e", uid = 222)
        .failed
        .futureValue should matchPattern {
        case ex: TrustDeserializationError if ex.body.contains("unexpected response") =>
      }
    }
  }

  "TrustApi / markupBasket" should {

    "successfully markup basket" in {
      val request = BasketMarkup(Map("1" -> OrderMarkup(10, 90), "2" -> OrderMarkup(90, 10)))
      val result = trustApi.markupBasket(BasketMarkupSuccessId, request)
      result.futureValue shouldBe ()
    }

    "fail with unknown error" in {
      val request = BasketMarkup(Map("1" -> OrderMarkup(10, 90), "2" -> OrderMarkup(90, 10)))
      val result = trustApi.markupBasket(BasketMarkupFailureId, request).failed.futureValue
      result shouldBe BasketMarkupError("unknown_error", Some("PaymentOrderNotFound"))
    }
  }

  "TrustApi / buildReceiptUrl" should {
    "get correct receipt url" in {
      val purchaseToken = "somePurchaseToken"
      trustApi.buildReceiptUrl(
        purchaseToken
      ) shouldBe s"$ReceiptEndpoint/checks/$purchaseToken/receipts/$purchaseToken"
    }
  }

  private def createProduct(id: ProductId) = trustApi.createProduct(
    CreateProductRequest(id, productName, productFiscalTitle, productFiscalNds)
  )

  private def updateProduct(id: ProductId) = trustApi.updateProduct(
    UpdateProductRequest(id, productName, productFiscalTitle, productFiscalNds)
  )

  private def getProductStatus(id: ProductId) = trustApi.getProductStatus(id)

  private def createOrder(id: ProductId) = trustApi.createOrder(
    CreateOrderRequest(id, orderQty, orderPrice),
    uid = 10L
  )

  private def getOrderStatus(id: OrderId) = trustApi.getOrderStatus(id)

  private def createBasket(orderId: OrderId) = trustApi.createBasket(
    CreateBasketRequest(
      List(BasketOrder(orderId, orderQty, orderPrice.toDouble)),
      currency = "RUB",
      returnPath = Some("http://yandex.ru"),
      backUrl = "banker_notification_url",
      paymentMode = "web_payment",
      paymentTimeout = 1200,
      paymethodId = "trust_web_page",
      templateTag = "desktop/form",
      fiscalTaxationType = "OSN",
      fiscalTitle = None,
      fiscalType = Some("full_payment_w_delivery"),
      userEmail = Some("test@yandex.ru"),
      userPhone = Some("+79112223344"),
      developerPayload = None,
      paymethodMarkup = Some(Map(orderId -> OrderMarkup(yandexAccount = 5.50, card = 4.50))),
      showTrustPaymentId = true,
      afsParams = Some(Map("x" -> "y"))
    ),
    uid = 10L
  )

  private def getBasket(purchaseToken: String) =
    trustApi.getBasket(purchaseToken)

  private def startBasket(purchaseToken: String) =
    trustApi.basketStartPayment(purchaseToken)

  private def createRefund(orderId: OrderId) = trustApi.createRefund(
    createRefundRequest = CreateRefundRequest(
      purchaseToken = refundPurchaseToken,
      reasonDesc = refundComment,
      orders = List(RefundOrder(orderId, refundDelta))
    ),
    uid = 10L
  )
}

object TrustApiSpec {

  val CreateProductSuccessId = "create_product_success_id"
  val CreateProductUnknownFailureId = "create_product_unknown_failure_id"
  val CreateProductConcreteFailureId = "create_product_concrete_failure_id"
  val CreateProductDeserializationErrorId = "create_product_deserialization_error_id"

  val UpdateProductSuccessId = "update_product_success_id"
  val UpdateProductUnknownFailureId = "update_product_unknown_failure_id"
  val UpdateProductConcreteFailureId = "update_product_concrete_failure_id"
  val UpdateProductDeserializationErrorId = "update_product_deserialization_error_id"

  val GetProductSuccessId = "get_product_success_id"
  val GetProductNotFoundFailureId = "get_product_not_found_failure_id"
  val GetProductUnknownFailureId = "get_product_unknown_failure_id"
  val GetProductFieldMissingFailureId = "get_product_field_missing_failure_id"

  val CreateOrderSuccessId = "create_order_success_id"
  val CreateOrderUnknownFailureId = "create_order_unknown_failure_id"
  val CreateOrderConcreteFailureId = "create_order_concrete_failure_id"
  val CreateOrderDeserializationErrorId = "create_order_deserialization_error_id"

  val GetOrderStatusSuccessId = "get_order_status_success_id"
  val GetOrderStatusUnknownFailureId = "get_order_status_unknown_failure_id"
  val GetOrderStatusConcreteFailureId = "get_order_status_concrete_failure_id"
  val GetOrderStatusDeserializationErrorId = "get_order_status_deserialization_error_id"

  val CreateBasketSuccessId = "create_basket_success_id"
  val CreateBasketFailureId = "create_basket_concrete_failure_id"
  val CreateBasketDeserializationErrorId = "create_basket_deserialization_error_id"

  val GetBasketSuccessNotStartedId = "get_basket_success_not_started_id"
  val GetBasketSuccessStartedId = "get_basket_success_started_id"
  val GetBasketSuccess3dsStartedId = "get_basket_success_3ds_started_id"
  val GetBasketSuccessAuthorizedId = "get_basket_success_authorized_id"
  val GetBasketSuccessNotAuthorizedId = "get_basket_success_not_authorized_id"
  val GetBasketSuccessClearedId = "get_basket_success_cleared_id"
  val GetBasketSuccessRefundedId = "get_basket_success_refunded_id"
  val GetBasketSuccessWithMarkupId = "get_basket_with_markup_id"
  val GetBasketUnknownFailureId = "get_basket_unknown_failure_id"
  val GetBasketConcreteFailureId = "get_basket_concrete_failure_id"
  val GetBasketDeserializationErrorId = "get_basket_deserialization_error_id"

  val StartBasketSuccessId = "start_basket_success_id"
  val StartBasketFailureId = "start_basket_unknown_failure_id"
  val StartBasketDeserializationErrorId = "start_basket_deserialization_error_id"

  val ClearBasketSuccessId = "clear_basket_success_id"
  val ClearBasketFailureId = "clear_basket_failure_id"

  val UnholdBasketSuccessId = "unhold_basket_success_id"
  val UnholdBasketFailureId = "unhold_basket_failure_id"

  val CreateRefundSuccessOrderId = "create_refund_success_order_id"
  val CreateRefundFailureOrderId = "create_refund_failure_order_id"

  val StartRefundSuccessId = "start_refund_success_id"
  val StartRefundNotFoundId = "start_refund_not_found_id"
  val StartRefundFailureId = "start_refund_failure_id"

  val GetRefundSuccessId = "get_refund_success_id"
  val GetRefundNotFoundId = "get_refund_not_found_id"
  val GetRefundFailedId = "get_refund_failed_id"

  val GetPaymentMethodsSuccessYandexUid = "21123456"
  val GetPaymentMethodsNoAuthYandexUid = "401"
  val GetPaymentMethodsFailureYandexUid = "500"
  val GetPaymentMethodTvmErrorUid = "403"

  val GetAccountsSuccessYandexUid = "21123456"
  val GetAccountsSuccessEmptyYandexUid = "21123457"
  val GetAccountsFailureYandexUid = "500"

  val CreateAccountSuccessYandexUid = "21123456"
  val CreateAccountSuccessCurrency = "RUB"
  val CreateAccountSuccessAlreadyExistsYandexUid = "21123457"
  val CreateAccountSuccessAlreadyExistsCurrency = "RUR"
  val CreateAccountFailureYandexUid = "500"

  val BasketMarkupSuccessId = "basket_markup_success_id"
  val BasketMarkupFailureId = "basket_markup_failure_id"

  val ReceiptEndpoint = "receipt-endpoint"
}
