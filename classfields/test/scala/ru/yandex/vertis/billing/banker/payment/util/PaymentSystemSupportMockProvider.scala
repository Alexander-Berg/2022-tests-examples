package ru.yandex.vertis.billing.banker.payment.util

import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.billing.banker.model.RefundPaymentRequest.SourceData
import ru.yandex.vertis.billing.banker.model.{
  Funds,
  PaymentRequest,
  PaymentRequestId,
  PaymentSystemId,
  PaymentSystemMethodId,
  State,
  User,
  YandexUid
}
import ru.yandex.vertis.billing.banker.payment.impl.BasePaymentSystemSupport
import ru.yandex.vertis.billing.banker.service.async.unsupported
import ru.yandex.vertis.billing.banker.service.{PaymentSystemService, PaymentSystemSupport}
import ru.yandex.vertis.billing.banker.util.{RequestContext, UserContext}

import scala.concurrent.{ExecutionContext, Future}

object PaymentSystemSupportMockProvider {

  class BasePaymentSystemSupportMock()(implicit val ec: ExecutionContext)
    extends BasePaymentSystemSupport
    with MockFactory {

    override val service: PaymentSystemService = mock[PaymentSystemService]

    override lazy val psId: PaymentSystemId = service.psId

    private val requestMethodMock = {
      mockFunction[User, PaymentSystemMethodId, PaymentRequest.Source, RequestContext, Future[PaymentRequest.Form]]
    }

    override def request(
        customer: User,
        method: PaymentSystemMethodId,
        source: PaymentRequest.Source
      )(implicit rc: RequestContext): Future[PaymentRequest.Form] = {
      requestMethodMock.apply(customer, method, source, rc)
    }

    override def parse(ns: State.NotificationSource)(implicit rc: RequestContext): Future[State.NotificationResponse] =
      unsupported

    def mockPsId(psId: PaymentSystemId): Unit = {
      (() => service.psId).expects().returns(psId): Unit
    }

    def mockRefund(sourceData: SourceData): Unit = {
      (service
        .processRefund(_: PaymentRequestId, _: Funds, _: SourceData)(_: UserContext))
        .expects(*, *, sourceData, *)
        .returns(Future.unit): Unit
    }

    def mockGetPaymentRequest(request: PaymentRequest): Unit = {
      (service
        .getPaymentRequest(_: PaymentRequestId)(_: RequestContext))
        .expects(*, *)
        .returns(Future.successful(request)): Unit
    }

    def mockRequest(source: PaymentRequest.Source, form: PaymentRequest.Form): Unit = {
      requestMethodMock
        .expects(*, *, source, *)
        .returns(Future.successful(form)): Unit
    }

  }

}
