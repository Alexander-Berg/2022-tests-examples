package ru.yandex.vertis.billing.banker.service.effect.util

import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RefundPaymentRequestRecord
import ru.yandex.vertis.billing.banker.model.{PaymentSystemId, PaymentSystemIds}
import ru.yandex.vertis.billing.banker.model.State.{Payment, Refund}
import ru.yandex.vertis.billing.banker.service.effect.EffectOnRequestRefundHelperService

import scala.concurrent.Future

trait EffectOnRequestRefundHelperServiceBaseSpec extends EffectRefundHelperServiceBaseSpec {

  trait EffectOnRequestRefundHelperServiceMock
    extends EffectOnRequestRefundHelperService
    with TestingEffectExecutionContextAware {

    private lazy val paymentSystemDaoMock = {
      val m = mock[PaymentSystemDao]
      (() => m.psId)
        .expects()
        .returning(PaymentSystemIds.YandexKassaV3)
        .anyNumberOfTimes()
      m
    }

    override protected def paymentSystemDao: PaymentSystemDao = {
      paymentSystemDaoMock
    }

    def mockPaymentGet(p: Payment): Unit = {
      (paymentSystemDaoMock.getPayments _)
        .expects(*)
        .returns(Future.successful(Seq(p))): Unit
    }

    def mockGetRefundRequests(records: Seq[RefundPaymentRequestRecord]): Unit = {
      (paymentSystemDaoMock.getRefundRequests _)
        .expects(*)
        .returns(Future.successful(records)): Unit
    }

    def mockGetRefundRequestsFail(): Unit = {
      (paymentSystemDaoMock.getRefundRequests _)
        .expects(*)
        .returns(Future.failed(ValidationException)): Unit
    }

    def mockGetRefunds(refunds: Seq[Refund]): Unit = {
      (paymentSystemDaoMock.getRefunds _)
        .expects(*)
        .returns(Future.successful(refunds)): Unit
    }

    def mockGetRefundsFail(): Unit = {
      (paymentSystemDaoMock.getRefunds _)
        .expects(*)
        .returns(Future.failed(ValidationException)): Unit
    }

  }

}
