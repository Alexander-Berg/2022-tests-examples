package ru.yandex.vertis.billing.banker.service.effect

import org.scalacheck.Gen
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RefundPaymentRequestRecord
import ru.yandex.vertis.billing.banker.model.gens.{
  getOr,
  refundRequestSourceGen,
  AccountIdGen,
  PaymentSystemMethodIdGen,
  RefundRequestSourceParams,
  RequestIdGen
}
import ru.yandex.vertis.billing.banker.model.{AccountId, PaymentRequestId, PaymentSystemMethodId}

object EffectRefundHelperServiceHelpers {

  case class RefundRequestRecordParams(
      id: Option[PaymentRequestId] = None,
      methodId: Option[PaymentSystemMethodId] = None,
      source: RefundRequestSourceParams = RefundRequestSourceParams(),
      account: Option[AccountId] = None)

  def refundRequestRecordGen(
      params: RefundRequestRecordParams = RefundRequestRecordParams()): Gen[RefundPaymentRequestRecord] =
    for {
      id <- getOr(params.id, RequestIdGen)
      methodId <- getOr(params.methodId, PaymentSystemMethodIdGen)
      account <- getOr(params.account, AccountIdGen)
      source <- refundRequestSourceGen(params.source)
    } yield RefundPaymentRequestRecord(id, methodId, account, source)

}
