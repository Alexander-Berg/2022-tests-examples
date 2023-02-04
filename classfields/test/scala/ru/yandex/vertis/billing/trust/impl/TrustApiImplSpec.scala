package ru.yandex.vertis.billing.trust.impl

import ru.yandex.vertis.billing.trust.TrustApiSpec._
import ru.yandex.vertis.billing.trust.{StaticTokenProvider, TrustApi, TrustApiSpec}
import ru.yandex.vertis.tracing.Traced
import sttp.capabilities
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Response, StringBody, SttpBackend}
import sttp.model.{MediaType, Method, StatusCode, Uri}

import scala.concurrent.Future
import scala.io.Source

class TrustApiImplSpec extends TrustApiSpec {

  private val createProductSuccess = createProductBody(CreateProductSuccessId)
  private val createProductUnknownFailure = createProductBody(CreateProductUnknownFailureId)
  private val createProductConcreteFailure = createProductBody(CreateProductConcreteFailureId)
  private val createProductDeserializationError = createProductBody(CreateProductDeserializationErrorId)

  private val updateProductSuccess = updateProductBody(UpdateProductSuccessId)
  private val updateProductUnknownFailure = updateProductBody(UpdateProductUnknownFailureId)
  private val updateProductConcreteFailure = updateProductBody(UpdateProductConcreteFailureId)
  private val updateProductDeserializationError = updateProductBody(UpdateProductDeserializationErrorId)

  private val createOrderSuccess = createOrderBody(CreateOrderSuccessId)
  private val createOrderConcreteFailure = createOrderBody(CreateOrderConcreteFailureId)
  private val createOrderUnknownFailure = createOrderBody(CreateOrderUnknownFailureId)
  private val createOrderDeserializationError = createOrderBody(CreateOrderDeserializationErrorId)

  private val createBasketSuccess = createBasketBody(CreateBasketSuccessId)
  private val createBasketFailure = createBasketBody(CreateBasketFailureId)
  private val createBasketDeserializationError = createBasketBody(CreateBasketDeserializationErrorId)

  private val createRefundSuccess = createRefundBody(CreateRefundSuccessOrderId)
  private val createRefundFailure = createRefundBody(CreateRefundFailureOrderId)

  private val createAccountSuccess = createAccountBody(CreateAccountSuccessCurrency)
  private val createAccountAlreadyExistsSuccess = createAccountBody(CreateAccountSuccessAlreadyExistsCurrency)

  private val basketMarkupBody = createBasketMarkupBody()

  private val settings = TrustApiSettings(Endpoint("localhost", 8028), "service-token", ReceiptEndpoint)
  private val tokenProvider = new StaticTokenProvider("tvm_service_ticket")

  private val testingBackend = SttpBackendStub.asynchronousFuture.whenRequestMatchesPartial {
    case req if path(req.uri).endsWith("""trust-payments/v2/products/""") && req.method == Method.POST =>
      req.body match {
        case `createProductSuccess` =>
          jsonOk("create_product_success.json")
        case `createProductUnknownFailure` =>
          json("create_product_unknown_failure.json", StatusCode.InternalServerError)
        case `createProductConcreteFailure` =>
          json("create_product_concrete_failure.json", StatusCode.BadRequest)
        case `createProductDeserializationError` =>
          jsonOk("deserialization_error.json")
        case other =>
          throw new Exception(s"Fix createProduct test, no matches for request body: $other")
      }
    case req if path(req.uri).endsWith("""trust-payments/v2/products/""") && req.method == Method.PUT =>
      req.body match {
        case `updateProductSuccess` =>
          jsonOk("update_product_success.json")
        case `updateProductUnknownFailure` =>
          json("update_product_unknown_failure.json", StatusCode.InternalServerError)
        case `updateProductConcreteFailure` =>
          json("update_product_concrete_failure.json", StatusCode.BadRequest)
        case `updateProductDeserializationError` =>
          jsonOk("deserialization_error.json")
        case other =>
          throw new Exception(s"Fix updateProduct test, no matches for request body: $other")
      }
    case req if path(req.uri).contains("""trust-payments/v2/products/""") && req.method == Method.GET =>
      val productId = req.uri.path.last
      productId match {
        case GetProductSuccessId =>
          jsonOk("get_product_success.json")
        case GetProductNotFoundFailureId =>
          json("get_product_not_found_failure.json", StatusCode.NotFound)
        case GetProductUnknownFailureId =>
          json("get_product_unknown_failure.json", StatusCode.InternalServerError)
        case GetProductFieldMissingFailureId =>
          jsonOk("get_product_field_missing_failure.json")
        case _ =>
          throw new Exception(s"Fix getProductStatus test, doesn't match test product_id $productId.")
      }

    case req if path(req.uri).endsWith("""trust-payments/v2/orders/""") && req.method == Method.POST =>
      req.body match {
        case `createOrderSuccess` =>
          jsonOk("create_order_success.json")
        case `createOrderConcreteFailure` =>
          json("create_order_concrete_failure.json", StatusCode.NotFound)
        case `createOrderUnknownFailure` =>
          json("create_order_unknown_failure.json", StatusCode.InternalServerError)
        case `createOrderDeserializationError` =>
          jsonOk("deserialization_error.json")
        case other =>
          throw new Exception(s"Fix createOrder test, no matches for request body: $other")
      }

    case req if path(req.uri).contains("""trust-payments/v2/orders/""") && req.method == Method.GET =>
      val productId = req.uri.path.last
      productId match {
        case GetOrderStatusSuccessId =>
          jsonOk("get_order_status_success.json")
        case GetOrderStatusConcreteFailureId =>
          json("get_order_status_concrete_failure.json", StatusCode.NotFound)
        case GetOrderStatusUnknownFailureId =>
          json("get_order_status_unknown_failure.json", StatusCode.InternalServerError)
        case GetOrderStatusDeserializationErrorId =>
          jsonOk("deserialization_error.json")
        case _ =>
          throw new Exception(s"Fix getOrderStatus test, doesn't match test product_id $productId.")
      }

    case req if path(req.uri).endsWith("""trust-payments/v2/payments/""") && (req.method == Method.POST) =>
      req.body match {
        case `createBasketSuccess` =>
          jsonOk("create_basket_success.json")
        case `createBasketFailure` =>
          jsonOk("create_basket_concrete_failure.json")
        case `createBasketDeserializationError` =>
          jsonOk("deserialization_error.json")
        case other =>
          throw new Exception(s"Fix createBasket test, doesn't match any request body: $other")
      }

    case req if path(req.uri).contains("""trust-payments/v2/payments/""") && req.method == Method.GET =>
      val paymentToken = req.uri.path.last
      paymentToken match {
        case GetBasketSuccessNotStartedId =>
          jsonOk("get_basket_not_started.json")
        case GetBasketSuccessStartedId =>
          jsonOk("get_basket_started.json")
        case GetBasketSuccess3dsStartedId =>
          jsonOk("get_basket_3ds_started.json")
        case GetBasketSuccessAuthorizedId =>
          jsonOk("get_basket_authorized.json")
        case GetBasketSuccessNotAuthorizedId =>
          jsonOk("get_basket_not_authorized.json")
        case GetBasketSuccessClearedId =>
          jsonOk("get_basket_cleared.json")
        case GetBasketSuccessRefundedId =>
          jsonOk("get_basket_refunded.json")
        case GetBasketSuccessWithMarkupId =>
          jsonOk("get_basket_with_markup.json")
        case GetBasketConcreteFailureId =>
          json("get_basket_concrete_failure.json", StatusCode.NotFound)
        case GetBasketDeserializationErrorId =>
          jsonOk("deserialization_error.json")
        case _ =>
          throw new Exception(s"Fix getBasket test, doesn't match test paymentToken $paymentToken.")
      }

    case req
        if path(req.uri).contains("""trust-payments/v2/payments/""") && path(req.uri).endsWith(
          "start"
        ) && req.method == Method.POST =>
      val paymentToken = req.uri.path.reverse.drop(1).head
      paymentToken match {
        case StartBasketSuccessId =>
          jsonOk("start_basket_success.json")
        case StartBasketFailureId =>
          json("start_basket_concrete_failure.json", StatusCode.NotFound)
        case StartBasketDeserializationErrorId =>
          jsonOk("deserialization_error.json")
        case _ =>
          throw new Exception(s"Fix startBasket test, doesn't match test paymentToken $paymentToken.")
      }

    case req
        if path(req.uri).contains("""trust-payments/v2/payments/""") && path(req.uri).endsWith(
          "clear"
        ) && req.method == Method.POST =>
      val paymentToken = req.uri.path.reverse.drop(1).head
      paymentToken match {
        case ClearBasketSuccessId =>
          jsonOk("clear_basket_success.json")
        case ClearBasketFailureId =>
          json("clear_basket_failure.json", StatusCode.NotFound)
        case _ =>
          throw new Exception(s"Fix clearBasket test, doesn't match test paymentToken $paymentToken.")
      }

    case req
        if path(req.uri).contains("""trust-payments/v2/payments/""") && path(req.uri).endsWith(
          "unhold"
        ) && req.method == Method.POST =>
      val paymentToken = req.uri.path.reverse.drop(1).head
      paymentToken match {
        case UnholdBasketSuccessId =>
          jsonOk("unhold_basket_success.json")
        case UnholdBasketFailureId =>
          json("unhold_basket_failure.json", StatusCode.NotFound)
        case _ =>
          throw new Exception(s"Fix unholdBasket test, doesn't match test paymentToken $paymentToken.")
      }

    case req if path(req.uri).endsWith("""trust-payments/v2/refunds/""") && (req.method == Method.POST) =>
      req.body match {
        case `createRefundSuccess` =>
          jsonOk("create_refund_success.json")
        case `createRefundFailure` =>
          json("create_refund_failure.json", StatusCode.InternalServerError)
        case other =>
          throw new Exception(s"Fix createRefund test, doesn't match any request body: $other")
      }

    case req
        if path(req.uri).contains("""trust-payments/v2/refunds/""") && path(req.uri).endsWith(
          "start"
        ) && req.method == Method.POST =>
      val refundId = req.uri.path.reverse.drop(1).head
      refundId match {
        case StartRefundSuccessId =>
          jsonOk("start_refund_success.json")
        case StartRefundNotFoundId =>
          json("start_refund_not_found.json", StatusCode.NotFound)
        case StartRefundFailureId =>
          json("start_refund_failure.json", StatusCode.InternalServerError)
        case _ =>
          throw new Exception(s"Fix startRefund test, doesn't match test refundId $refundId.")
      }

    case req if path(req.uri).contains("""trust-payments/v2/refunds/""") && req.method == Method.GET =>
      val refundId = req.uri.path.last
      refundId match {
        case GetRefundSuccessId =>
          jsonOk("get_refund_success.json")
        case GetRefundFailedId =>
          jsonOk("get_refund_failed.json")
        case GetRefundNotFoundId =>
          json("get_refund_not_found.json", StatusCode.NotFound)
        case _ =>
          throw new Exception(s"Fix getRefund test, doesn't match test refundId $refundId.")
      }

    case req if path(req.uri).contains("""trust-payments/v2/payment-methods/""") && req.method == Method.GET =>
      val uid = req.header("X-Uid")
      uid match {
        case Some(GetPaymentMethodsSuccessYandexUid) =>
          jsonOk("get_payment_methods_success.json")
        case Some(GetPaymentMethodsNoAuthYandexUid) =>
          json("get_payment_methods_no_auth.json", StatusCode.Unauthorized)
        case Some(GetPaymentMethodsFailureYandexUid) =>
          json("get_payment_methods_failure.json", StatusCode.InternalServerError)
        case Some(GetPaymentMethodTvmErrorUid) =>
          json("get_payment_methods_tvm_error.json", StatusCode.Forbidden)
        case _ =>
          throw new Exception(s"Fix getPaymentMethods test, doesn't match test uid $uid.")
      }

    case req if path(req.uri).contains("""trust-payments/v2/account/""") && req.method == Method.GET =>
      val uid = req.header("X-Uid")
      uid match {
        case Some(GetAccountsSuccessYandexUid) =>
          jsonOk("get_accounts_success.json")
        case Some(GetAccountsSuccessEmptyYandexUid) =>
          jsonOk("get_accounts_success_empty.json")
        case Some(GetAccountsFailureYandexUid) =>
          json("get_accounts_failure.json", StatusCode.InternalServerError)
        case _ =>
          throw new Exception(s"Fix getAccounts test, doesn't match test uid $uid.")
      }

    case req if path(req.uri).contains("""trust-payments/v2/account/""") && req.method == Method.POST =>
      val uid = req.header("X-Uid")
      val body = req.body
      (uid, body) match {
        case (Some(CreateAccountSuccessYandexUid), `createAccountSuccess`) =>
          jsonOk("create_account_success.json")
        case (Some(CreateAccountSuccessAlreadyExistsYandexUid), `createAccountAlreadyExistsSuccess`) =>
          json("create_account_success_already_exists.json", StatusCode.Created)
        case (Some(CreateAccountFailureYandexUid), _) =>
          json("create_account_failure.json", StatusCode.InternalServerError)
        case _ =>
          throw new Exception(s"Fix createAccount test, doesn't match test uid $uid and body $body.")
      }

    case req
        if path(req.uri).contains("""trust-payments/v2/payments/""") && path(req.uri).endsWith(
          "markup"
        ) && req.method == Method.POST =>
      val paymentToken = req.uri.path.reverse.drop(1).head
      val body = req.body
      (paymentToken, body) match {
        case (BasketMarkupSuccessId, `basketMarkupBody`) =>
          jsonOk("basket_markup_success.json")
        case (BasketMarkupFailureId, `basketMarkupBody`) =>
          json("basket_markup_failure.json", StatusCode.NotFound)
        case _ =>
          throw new Exception(s"Fix markupBasket test, doesn't match any paymentToken $paymentToken and body $body.")
      }

    case req =>
      throw new Exception(s"Unexpected http request. ${path(req.uri)}")

  }

  private def path(uri: Uri): String = {
    uri.path.mkString("/")
  }

  private def json(fileName: String, statusCode: StatusCode) = {
    Response(Source.fromResource(fileName)(scala.io.Codec.UTF8).getLines().mkString, statusCode)
  }

  private def jsonOk(fileName: String) = {
    Response.ok(Source.fromResource(fileName)(scala.io.Codec.UTF8).getLines().mkString)
  }

  private def createProductBody(productId: String) = {
    StringBody(
      s"""{"product_id":"$productId","name":"$productName","fiscal_title":"$productFiscalTitle","fiscal_nds":"${productFiscalNds.value}"}""",
      "utf-8",
      MediaType.ApplicationJson
    )
  }

  private def updateProductBody(productId: String) = {
    StringBody(
      s"""{"id":"$productId","name":"$productName","fiscal_title":"$productFiscalTitle","fiscal_nds":"${productFiscalNds.value}"}""",
      "utf-8",
      MediaType.ApplicationJson
    )
  }

  private def createOrderBody(productId: String) = {
    StringBody(
      s"""{"product_id":"$productId","qty":$orderQty,"price":$orderPrice}""",
      "utf-8",
      MediaType.ApplicationJson
    )
  }

  private def createBasketBody(orderId: String) = {
    StringBody(
      s"""{
           |"orders":[{"order_id":"$orderId","qty":1,"price":10.0}],
           |"currency":"RUB",
           |"return_path":"http://yandex.ru",
           |"back_url":"banker_notification_url",
           |"payment_mode":"web_payment",
           |"payment_timeout":1200,
           |"paymethod_id":"trust_web_page",
           |"template_tag":"desktop/form",
           |"fiscal_taxation_type":"OSN",
           |"fiscal_title":null,
           |"fiscal_type":"full_payment_w_delivery",
           |"user_email":"test@yandex.ru",
           |"user_phone":"+79112223344",
           |"developer_payload":null,
           |"paymethod_markup": {"$orderId": {"yandex_account": 5.5,"card": 4.5}},
           |"show_trust_payment_id":true,
           |"afs_params":{"x":"y"}}""".stripMargin.replaceAll("\\s", ""),
      "utf-8",
      MediaType.ApplicationJson
    )
  }

  private def createRefundBody(orderId: String) = {
    StringBody(
      s"""{"purchase_token":"$refundPurchaseToken","reason_desc":"$refundComment","orders":[{"order_id":"$orderId","delta_amount":$refundDelta}]}""",
      "utf-8",
      MediaType.ApplicationJson
    )
  }

  private def createAccountBody(currency: String) = {
    StringBody(
      s"""{"currency":"$currency"}""",
      "utf-8",
      MediaType.ApplicationJson
    )
  }

  private def createBasketMarkupBody() = {
    StringBody(
      s"""{
             |    "paymethod_markup": {
             |        "1": {
             |            "yandex_account": 10,
             |            "card": 90
             |        },
             |        "2": {
             |            "yandex_account": 90,
             |            "card": 10
             |        }
             |    }
             |}""".stripMargin.replaceAll("\\s", ""),
      "utf-8",
      MediaType.ApplicationJson
    )
  }

  override val trustApi: TrustApi =
    new TrustApiImpl(settings = settings, tokenProvider = tokenProvider, backend = testingBackend)

  override def makeTrustApi(backend: SttpBackendStub[Future, Any]): TrustApi =
    new TrustApiImpl(settings = settings, tokenProvider = tokenProvider, backend)
}
