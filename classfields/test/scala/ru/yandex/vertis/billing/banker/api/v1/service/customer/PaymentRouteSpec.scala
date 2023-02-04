package ru.yandex.vertis.billing.banker.api.v1.service.customer

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MediaTypes, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.api.v1.view._
import ru.yandex.vertis.billing.banker.model.PaymentRequest._
import ru.yandex.vertis.billing.banker.model.State.{Incoming, StateStatuses}
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentMarkupRequest,
  paymentRequestGen,
  paymentRequestSourceGen,
  refundRequestSourceDataGen,
  PaymentRequestParams,
  PaymentRequestSourceParams,
  Producer,
  StateParams
}
import ru.yandex.vertis.billing.banker.service.{AccessDenyException, AvailableMethodsResponse}
import spray.json.JsObject

import scala.concurrent.Future

/**
  * Spec on [[PaymentRoute]]
  *
  * @author ruslansd
  */
class PaymentRouteSpec extends AnyWordSpecLike with RootHandlerSpecBase with AsyncSpecBase {

  private lazy val customer = "test_customer"

  override def basePath: String = s"/api/1.x/service/autoru/customer/$customer"
  private val account = Account("1", customer)

  import PaymentRequestSourceView.jsonFormat
  import ru.yandex.vertis.billing.banker.api.v1.view.PaymentMethodView.modelIterableUnmarshaller
  import spray.json.enrichAny

  private val method = PaymentMethod(PaymentSystemIds.Robokassa, "test_method")
  val methods = Seq(method)
  val source = paymentRequestSourceGen().next
  val entity = PaymentRequestSourceView.asView(source).toJson.compactPrint
  val setup = backend.psregistry.get(PaymentSystemIds.Robokassa).futureValue
  val accountTransactions = backend.transactions

  val paymentRequestId = "test_payment_id"
  val getPaymentRequest: HttpRequest = Get(url(s"/payment/${method.ps}/$paymentRequestId"))

  def checkPaymentGetRoute(
      stateParams: Option[StateParams] = None,
      hasActiveTr: Option[Boolean] = None
    )(toExpected: PaymentRequest => PaymentRequest): Unit = {
    val sourceParams = PaymentRequestSourceParams(withReceipt = Some(false))

    val paymentRequest = paymentRequestGen(
      PaymentRequestParams(
        id = Some(paymentRequestId),
        source = sourceParams,
        state = stateParams
      )
    ).next
    when(setup.support.getPaymentRequest(?, ?)(?))
      .thenReturn(Future.successful(paymentRequest))

    when(setup.support.syncPendingPayment(?)(?))
      .thenReturn(Future.successful(paymentRequest))

    hasActiveTr.foreach { has =>
      when(accountTransactions.has(?)(?))
        .thenReturn(Future.successful(has))
    }

    getPaymentRequest ~> defaultHeaders ~> route ~> check {
      status shouldBe StatusCodes.OK
      val actual = responseAs[PaymentRequestView]
      val expected = toExpected(paymentRequest)
      actual shouldBe PaymentRequestView.asView(expected): Unit
    }
  }

  def checkPaymentGetRoute(
      stateStatus: StateStatuses.Value,
      hasActiveTr: Boolean
    )(toExpected: PaymentRequest => PaymentRequest): Unit = {
    val stateParams = StateParams(
      `type` = Some(State.Types.Incoming),
      stateStatus = Set(stateStatus)
    )
    checkPaymentGetRoute(Some(stateParams), Some(hasActiveTr))(toExpected)
  }

  def checkPaymentGetRoute(stateStatus: StateStatuses.Value)(toExpected: PaymentRequest => PaymentRequest): Unit = {
    val stateParams = StateParams(
      `type` = Some(State.Types.Incoming),
      stateStatus = Set(stateStatus)
    )
    checkPaymentGetRoute(Some(stateParams), None)(toExpected)
  }

  "/payment" should {
    val request = Post(url("/payment"))
    "post source" in {
      when(backend.psregistry.available(?, ?)(?))
        .thenReturn(Future.successful(AvailableMethodsResponse(succs = List.from(methods), errs = Nil)))

      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Iterable[PaymentMethod]]
          response should contain theSameElementsAs methods
        }
    }
    "fail post source without header" in {
      request.withEntity(ContentTypes.`application/json`, entity) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }
    "fail post source without entity" in {
      request ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail post source by " in {
      when(backend.psregistry.available(?, ?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))

      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }
  }

  "/payment/{gate}/method/{method}" should {
    import PaymentRequestFormView.modelUnmarshaller
    val request = Post(url(s"/payment/${method.ps}/method/${method.id}"))
    "post source" in {
      val form = UrlForm("1212", "test_url", None)
      when(setup.support.request(?, ?, ?)(?))
        .thenReturn(Future.successful(form))

      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        addAcceptJson ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[PaymentRequest.Form]
          response shouldBe form
        }
    }
    "post source with text/html" in {
      val form = PostForm("1212", Seq(("action", "action")), None)
      when(setup.support.request(?, ?, ?)(?))
        .thenReturn(Future.successful(form))
      val preparedRequest =
        request.withEntity(ContentTypes.`application/json`, entity) ~>
          defaultHeaders

      preparedRequest ~>
        addHeader(Accept(ContentTypes.`text/html(UTF-8)`.mediaType)) ~>
        route ~> check {
          status shouldBe StatusCodes.OK
        }

      preparedRequest ~>
        addHeader(Accept(MediaTypes.`text/html`)) ~>
        route ~> check {
          status shouldBe StatusCodes.OK
        }
    }

    "fail post source with text/html" in {
      val form = EmptyForm("1212")
      when(setup.support.request(?, ?, ?)(?))
        .thenReturn(Future.successful(form))

      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        addHeader(Accept(MediaTypes.`text/html`)) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }

    "fail post source without header" in {
      request.withEntity(ContentTypes.`application/json`, entity) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }
    "fail post source without entity" in {
      request ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }

    "fail post source by " in {
      when(setup.support.request(?, ?, ?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))

      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

  }

  "/payment/{gate}/{id}" should {
    "get payment without state" in {
      checkPaymentGetRoute()(p => p)
    }

    "get payment valid and processed" in {
      checkPaymentGetRoute(StateStatuses.Valid, hasActiveTr = true)(p => p)
    }

    "get payment valid and unprocessed" in {
      checkPaymentGetRoute(StateStatuses.Valid, hasActiveTr = false) { p =>
        p.copy(state = None)
      }
    }

    "get payment cancelled and processed" in {
      checkPaymentGetRoute(StateStatuses.Cancelled, hasActiveTr = false)(p => p)
    }

    "get payment cancelled and unprocessed" in {
      checkPaymentGetRoute(StateStatuses.Cancelled, hasActiveTr = true) { p =>
        val state = p.state.get.asInstanceOf[Incoming]
        val changedState = state.copy(stateStatus = StateStatuses.Valid)
        p.copy(state = Some(changedState))
      }
    }

    "get payment full refunded" in {
      checkPaymentGetRoute(StateStatuses.Refunded)(p => p)
    }

    "get payment partly refunded" in {
      checkPaymentGetRoute(StateStatuses.PartlyRefunded)(p => p)
    }

    "fail get payment without headers" in {
      getPaymentRequest ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail get payment by AccessDenyException" in {
      when(setup.support.getPaymentRequest(?, ?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))

      getPaymentRequest ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }
  }

  "/payment/{gate}/{id} delete" should {
    val request = Delete(url(s"/payment/${method.ps}/id"))
    "refund payment" in {
      when(setup.support.fullRefund(?, ?, ?, ?)(?))
        .thenReturn(Future.successful(()))

      request ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "fail refund payment without headers" in {
      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail get payment by AccessDenyException" in {
      when(setup.support.fullRefund(?, ?, ?, ?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))

      request ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }
  }

  "/payment/{gate}/{id}/refund post" should {
    val params = PaymentRequestParams().withState
    val paymentRequest = paymentRequestGen(params).next
    val sourceData = refundRequestSourceDataGen().next
    val request = Post(url(s"/payment/${method.ps}/${paymentRequest.id}/refund?amount=1000"))

    "refund payment wallet request" in {
      val walletSource = paymentRequest.source.copy(context = Context(Targets.Wallet))
      val walletPaymentRequest = paymentRequest.copy(source = walletSource)

      when(setup.support.getPaymentRequest(?, ?)(?))
        .thenReturn(Future.successful(walletPaymentRequest))
      when(setup.support.refund(?, ?, ?, ?)(?))
        .thenReturn(Future.unit)

      val validSourceData = sourceData.copy(jsonPayload = None)
      val validEntity = RefundPaymentRequestSourceDataView.asView(validSourceData).toJson.compactPrint
      val validRequest = request.withEntity(ContentTypes.`application/json`, validEntity)

      validRequest ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "refund payment purchase request" in {
      val purchaseSource = paymentRequest.source.copy(context = Context(Targets.Purchase))
      val purchasePaymentRequest = paymentRequest.copy(source = purchaseSource)

      when(setup.support.getPaymentRequest(?, ?)(?))
        .thenReturn(Future.successful(purchasePaymentRequest))
      when(setup.support.refund(?, ?, ?, ?)(?))
        .thenReturn(Future.unit)

      val validSourceData = sourceData.copy(jsonPayload = Some(JsObject()))
      val validEntity = RefundPaymentRequestSourceDataView.asView(validSourceData).toJson.compactPrint
      val validRequest = request.withEntity(ContentTypes.`application/json`, validEntity)

      validRequest ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "fail refund payment without headers" in {
      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail get payment by AccessDenyException" in {
      when(setup.support.getPaymentRequest(?, ?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))

      val entity = RefundPaymentRequestSourceDataView.asView(sourceData).toJson.compactPrint
      val refundRequest = request.withEntity(ContentTypes.`application/json`, entity)

      refundRequest ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }
  }

  "/payment/{gate}/{id}/markup put" should {
    val prId = 100
    val data = paymentMarkupRequest().next
    val request = Put(url(s"/payment/${method.ps}/$prId/markup"))

    "successfully put payment markup" in {
      when(setup.support.markupPayment(?, ?, ?)(?))
        .thenReturn(Future.unit)

      val validEntity = PaymentMarkupRequestView.asView(data).toJson.compactPrint
      val validRequest = request.withEntity(ContentTypes.`application/json`, validEntity)

      validRequest ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "fail pur markup without header" in {
      request.withEntity(ContentTypes.`application/json`, entity) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }

    "fail put markup without entity" in {
      request ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }
  }
}
