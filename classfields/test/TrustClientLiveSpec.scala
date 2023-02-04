package billing.shop.billing_gates.trust.test

import billing.shop.billing_gates.trust.test.TrustClientLiveSpec.jsonOk
import common.zio.sttp.endpoint.Endpoint
import ru.yandex.vertis.{
  BasketResponse,
  BoundPaymentMethod,
  CreateBasketRequest,
  CreateProductSuccess,
  EnabledPaymentMethod,
  PaymentMethodsResponse,
  PaymentOrder,
  PaymentStatus,
  PurchaseToken
}
import ru.yandex.vertis.billing.shop.billing_gates.trust.src.TrustClient
import ru.yandex.vertis.billing.shop.billing_gates.trust.src.model.CreateProductRequest
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._
import ru.yandex.vertis.billing.shop.billing_gates.trust.src.TrustClientLive
import ru.yandex.vertis.billing.shop.model.TrustLogicalError
import sttp.client3.{Response, StringBody}
import sttp.model.{Header, MediaType, Method, Uri}

import scala.io.Source

object TrustClientLiveSpec extends DefaultRunnableSpec {

  private val successCreateProduct = createProductBody("test_product_id_success_case")
  private val unknownFailureCreateProduct = createProductBody("test_unknown_failure")
  private val concreteFailureCreateProduct = createProductBody("test_logical_trust_failure")

  private val successCreateBasket = createBasketBody("success_test_product_id")
  private val failureCreateBasket = createBasketBody("error_test_product_id")

  private val stub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case req
        if path(req.uri).endsWith("""trust-payments/v2/products/""") &&
          req.method == Method.POST =>
      req.body match {
        case `successCreateProduct` =>
          jsonOk("create_product_success.json")
        case `unknownFailureCreateProduct` =>
          jsonOk("create_product_unknown_failure.json")
        case `concreteFailureCreateProduct` =>
          jsonOk("create_product_concrete_failure.json")
        case _ =>
          throw new Exception("Fix createProduct test, doesn't match any request body")
      }
    case req if path(req.uri).contains("""trust-payments/v2/payment-methods""") && req.method == Method.GET =>
      if (req.headers.contains(Header("X-Uid", "passport_user_id_empty"))) {
        jsonOk("get_payment_methods_empty_success.json")
      } else if (req.headers.contains(Header("X-Uid", "passport_user_id"))) {
        jsonOk("get_payment_methods_success.json")
      } else {
        throw new Exception("Fix getPaymentMethods tests. There is no matching X-Uid")
      }
    case req
        if path(req.uri).contains("""trust-payments/v2/payments""") && path(req.uri).contains(
          "start"
        ) && (req.method == Method.POST) =>
      val basketId = req.uri.path.takeRight(2).head
      basketId match {
        case "success_basket_id" =>
          jsonOk("start_basket_payment_success.json")
        case "failure_basket_id" =>
          jsonOk("start_basket_payment_failure.json")
        case _ =>
          throw new Exception(s"Fix basketStartPayment test, doesn't match test basket_id $basketId")
      }
    case req if path(req.uri).endsWith("""trust-payments/v2/payments/""") && (req.method == Method.POST) =>
      req.body match {
        case `successCreateBasket` =>
          jsonOk("create_basket_success.json")
        case `failureCreateBasket` =>
          jsonOk("create_basket_failure.json")
        case _ =>
          throw new Exception("Fix createBasket test, doesn't match any request body")
      }
    case req if path(req.uri).contains("""trust-payments/v2/payments""") && (req.method == Method.GET) =>
      val basketId = req.uri.path.last
      basketId match {
        case "success_basket_id" =>
          jsonOk("get_basket_success.json")
        case "failure_basket_id" =>
          jsonOk("get_basket_failure.json")
        case _ =>
          throw new Exception(s"Fix getBasket test, doesn't match test basket_id $basketId. ")
      }
  }

  private def path(uri: Uri): String = {
    uri.path.mkString("/")
  }

  private def jsonOk(fileName: String) = {
    Response.ok(Source.fromResource(fileName)(scala.io.Codec.UTF8).getLines().mkString)
  }

  private def createProductBody(productId: String) = {
    StringBody(s"""{"product_id":"$productId","name":"test_name"}""", "utf-8", Some(MediaType.ApplicationJson))
  }

  private def createBasketBody(productId: String) = {
    StringBody(
      s"""{"amount":100.0,"currency":"RUR","product_id":"$productId","return_path":"http://return_path","paymethod_id":"test_paymethod_id"}""",
      "utf-8",
      Some(MediaType.ApplicationJson)
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("trust client")(
    testM("create new product succeed") {
      for {
        res <- TrustClient.createProduct(CreateProductRequest("test_product_id_success_case", "test_name"))
      } yield assert(res)(equalTo(CreateProductSuccess()))
    },
    testM("unknown trust logical error") {
      for {
        res <- TrustClient.createProduct(CreateProductRequest("test_unknown_failure", "test_name")).run
      } yield assert(res)(
        fails(equalTo(TrustLogicalError("createProduct, status: Error, code: unknown_error, desc: no desc")))
      )
    },
    testM("trust logical error") {
      for {
        res <- TrustClient.createProduct(CreateProductRequest("test_logical_trust_failure", "test_name")).run
      } yield assert(res)(
        fails(
          equalTo(TrustLogicalError("createProduct, status: Error, code: partner_not_found, desc: partner not found"))
        )
      )
    },
    testM("get empty payment methods success") {
      for {
        res <- TrustClient.getPaymentMethods(Some("passport_user_id_empty"))
      } yield assert(res)(equalTo(PaymentMethodsResponse(List(), List())))
    },
    testM("get payment methods success") {
      for {
        res <- TrustClient.getPaymentMethods(Some("passport_user_id"))
      } yield assert(res)(
        equalTo(
          PaymentMethodsResponse(
            List(EnabledPaymentMethod(1, None, "card", "RUB")),
            List(
              BoundPaymentMethod(
                "card-x5a411923141e41fbefc9a816",
                "card",
                "555555****4444",
                "MasterCard",
                225,
                1480012966674L,
                "fake",
                "2017",
                "04",
                "TEST",
                expired = false
              )
            )
          )
        )
      )
    },
    testM("create basket with single order success") {
      for {
        res <- TrustClient.createBasket(
          CreateBasketRequest(
            amount = 100,
            "RUR",
            "success_test_product_id",
            "http://return_path",
            paymethodId = "test_paymethod_id"
          ),
          Some("passport_user_id")
        )
      } yield assert(res)(equalTo(PurchaseToken("290dcf726200095c35628d152a9e0f5b")))
    },
    testM("create basket with single order logical trust error") {
      for {
        res <- TrustClient
          .createBasket(
            CreateBasketRequest(
              amount = 100,
              "RUR",
              "error_test_product_id",
              "http://return_path",
              paymethodId = "test_paymethod_id"
            ),
            Some("passport_user_id")
          )
          .run
      } yield assert(res)(fails(equalTo(TrustLogicalError("createBasket, status: Error"))))
    },
    testM("get basket success") {
      for {
        res <- TrustClient.getBasket("success_basket_id")
      } yield assert(res)(
        equalTo(
          BasketResponse(
            "290dcf726200095c35628d152a9e0f5b",
            100.0,
            "RUB",
            List(
              PaymentOrder(
                784371418,
                1480613553,
                "1408716623713",
                "app",
                "default service product",
                100.0,
                0.0,
                0.0
              )
            ),
            1200.0,
            PaymentStatus.NotStarted,
            startTs = None,
            "https://trust.yandex.ru/web/payment?purchase_token=290dcf726200095c35628d152a9e0f5b"
          )
        )
      )
    },
    testM("get basket logical trust error") {
      for {
        res <- TrustClient.getBasket("failure_basket_id").run
      } yield assert(res)(fails(equalTo(TrustLogicalError("getBasket, status: Error"))))
    },
    testM("start basket payment success") {
      for {
        res <- TrustClient.basketStartPayment("success_basket_id")
      } yield assert(res)(
        equalTo(
          BasketResponse(
            "290dcf726200095c35628d152a9e0f5b",
            100.0,
            "RUB",
            List(
              PaymentOrder(
                55555555,
                1480613553,
                "1408716623713",
                "app",
                "default service product",
                100.0,
                0.0,
                0.0
              )
            ),
            1200.0,
            PaymentStatus.Started,
            startTs = Some(1480613594.258),
            "https://trust.yandex.ru/web/payment?purchase_token=290dcf726200095c35628d152a9e0f5b"
          )
        )
      )
    },
    testM("start basket payment trust error") {
      for {
        res <- TrustClient.basketStartPayment("failure_basket_id").run
      } yield assert(res)(fails(equalTo(TrustLogicalError("basketStartPayment, status: Error"))))
    }
  ).provideCustomLayerShared(
    (Endpoint.testEndpointLayer ++ Sttp.fromStub(stub)) >>>
      TrustClientLive.live
  )
}
