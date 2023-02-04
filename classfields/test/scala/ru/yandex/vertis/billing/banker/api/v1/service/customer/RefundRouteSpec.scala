package ru.yandex.vertis.billing.banker.api.v1.service.customer

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.model.{PaymentMethod, PaymentSystemIds, RefundPaymentRequest}
import ru.yandex.vertis.billing.banker.model.gens.refundRequestGen
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import ru.yandex.vertis.billing.banker.proto.PaymentProtoFormats.RefundPaymentRequestFormat

import scala.concurrent.Future

/**
  * Spec on [[RefundRoute]]
  *
  * @author tolmach
  */
class RefundRouteSpec
  extends AnyWordSpecLike
  with RootHandlerSpecBase
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  private lazy val customer = "test_customer"

  override def basePath: String = s"/api/1.x/service/autoru/customer/$customer"

  private val method = PaymentMethod(PaymentSystemIds.YandexKassaV3, "test_method")
  val methods = Seq(method)

  val setup = backend.psregistry.get(PaymentSystemIds.YandexKassaV3).futureValue
  val accountTransactions = backend.transactions

  val refundRequestId = "test_refund_id"
  val getRefundByIdRequest: HttpRequest = Get(url(s"/refund/${method.ps}/$refundRequestId"))
  val paymentRequestId = "test_payment_id"
  val getRefundForPaymentRequest: HttpRequest = Get(url(s"/refund/${method.ps}/for/$paymentRequestId"))

  implicit val refundRequestUnmarshaller: FromEntityUnmarshaller[RefundPaymentRequest] =
    ProtobufSupport.protoReadingUnmarshaller(RefundPaymentRequestFormat)

  implicit val refundRequestIterableUnmarshaller: FromEntityUnmarshaller[Seq[RefundPaymentRequest]] =
    ProtobufSupport.protoReadingIterableUnmarshaller(RefundPaymentRequestFormat)

  "/refund/{gate}/{id}" should {
    "fail" when {
      "refund does not exist" in {
        when(setup.support.getRefundRequest(?, ?)(?))
          .thenReturn(
            Future.failed(new NoSuchElementException("Not found"))
          )

        getRefundByIdRequest ~> defaultHeaders ~> route ~> check {
          status shouldBe StatusCodes.NotFound
        }
      }
    }
    "get refund" when {
      "refund exist" in {
        forAll(refundRequestGen()) { refund =>
          when(setup.support.getRefundRequest(?, ?)(?))
            .thenReturn(
              Future.successful(refund)
            )

          getRefundByIdRequest ~> defaultHeaders ~> route ~> check {
            status shouldBe StatusCodes.OK
            val actual = responseAs[RefundPaymentRequest]
            actual shouldBe refund
          }
        }
      }
    }
  }

  "/refund/{gate}/for/{id}" should {
    "get refunds" when {
      "refund exist" in {
        forAll(Gen.listOf(refundRequestGen())) { refunds =>
          when(setup.support.getRefundRequestsFor(?, ?)(?))
            .thenReturn(
              Future.successful(refunds)
            )

          getRefundForPaymentRequest ~> defaultHeaders ~> route ~> check {
            status shouldBe StatusCodes.OK
            val actual = responseAs[Seq[RefundPaymentRequest]]
            actual should contain theSameElementsAs refunds
          }
        }
      }
    }
  }

}
