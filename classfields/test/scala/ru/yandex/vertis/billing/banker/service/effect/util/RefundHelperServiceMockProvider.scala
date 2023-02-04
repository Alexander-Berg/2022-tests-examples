package ru.yandex.vertis.billing.banker.service.effect.util

import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{ReceiptData, ReceiptGood}
import ru.yandex.vertis.billing.banker.model.{
  Funds,
  PaymentRequestId,
  PaymentSystemId,
  RefundPaymentRequest,
  RefundPaymentRequestId,
  User
}
import ru.yandex.vertis.billing.banker.service.RefundHelperService
import ru.yandex.vertis.billing.banker.service.refund.processor.RefundProcessor
import ru.yandex.vertis.util.concurrent.Threads
import spray.json.JsObject

import scala.concurrent.Future

trait RefundHelperServiceMockProvider extends MockFactory {

  protected val RefundPaymentId = "refund_payment_request_id"
  protected val PaymentRequestId = "payment_request_id"

  protected val ValidationException = new IllegalArgumentException("VALIDATION")

  protected val SourceData = {
    val receipt = ReceiptData(
      Seq(ReceiptGood("good", 1, 1L))
    )
    RefundPaymentRequest.SourceData(
      "comment",
      None,
      Some(JsObject()),
      Some(receipt)
    )
  }

  class RefundHelperServiceMock extends RefundHelperService {

    private val serviceMock = mock[RefundHelperService]

    def mockPsId(psId: PaymentSystemId): Unit = {
      (() => serviceMock.psId).expects().returns(psId): Unit
    }

    override def psId: PaymentSystemId = {
      serviceMock.psId
    }

    override def refundProcessor: RefundProcessor = {
      serviceMock.refundProcessor
    }

    def mockProcessRefundAndCall: Future[Unit] = {
      (serviceMock
        .processRefund(
          _: PaymentRequestId,
          _: Funds,
          _: User,
          _: RefundPaymentRequest.SourceData
        ))
        .expects(*, *, *, *)
        .returns(Future.successful(RefundPaymentId))

      processRefund("refundFor", 1L, "user", SourceData).map(_ => ())(Threads.SameThreadEc)
    }

    def mockProcessRefundFailAndCall: Future[Unit] = {
      (serviceMock
        .processRefund(
          _: PaymentRequestId,
          _: Funds,
          _: User,
          _: RefundPaymentRequest.SourceData
        ))
        .expects(*, *, *, *)
        .returns(Future.failed(ValidationException))

      processRefund("refundFor", 1L, "user", SourceData).map(_ => ())(Threads.SameThreadEc)
    }

    override def processRefund(
        refundFor: PaymentRequestId,
        desiredAmount: Funds,
        user: User,
        sourceData: RefundPaymentRequest.SourceData): Future[RefundPaymentRequestId] = {
      serviceMock.processRefund(
        refundFor,
        desiredAmount,
        user,
        sourceData
      )
    }

    def mockProcessRefundRequestAndCall: Future[Unit] = {
      (serviceMock
        .processRefundRequest(
          _: RefundPaymentRequestId,
          _: PaymentRequestId
        ))
        .expects(*, *)
        .returns(Future.unit)

      processRefundRequest(RefundPaymentId, PaymentRequestId)
    }

    def mockProcessRefundRequestFailAndCall: Future[Unit] = {
      (serviceMock
        .processRefundRequest(
          _: RefundPaymentRequestId,
          _: PaymentRequestId
        ))
        .expects(*, *)
        .returns(Future.failed(ValidationException))

      processRefundRequest(RefundPaymentId, PaymentRequestId)
    }

    override def processRefundRequest(refundId: RefundPaymentRequestId, refundFor: PaymentRequestId): Future[Unit] = {
      serviceMock.processRefundRequest(
        refundId,
        refundFor
      )
    }

    override def getOrCreateRefundPaymentRequest(
        refundFor: PaymentRequestId,
        desiredAmount: Funds,
        user: User,
        sourceData: RefundPaymentRequest.SourceData): Future[RefundPaymentRequest] = {
      serviceMock.getOrCreateRefundPaymentRequest(
        refundFor,
        desiredAmount,
        user,
        sourceData
      )
    }

  }

}
