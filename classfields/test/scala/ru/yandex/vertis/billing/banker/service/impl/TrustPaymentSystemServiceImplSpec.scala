package ru.yandex.vertis.billing.banker.service.impl

import ru.yandex.vertis.billing.banker.dao.TrustExternalPurchaseDao
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.service.refund.processor.TrustRefundProcessor
import ru.yandex.vertis.billing.banker.service.{PaymentSystemServiceSpec, RefundHelperService}
import ru.yandex.vertis.billing.trust.TrustApi
import ru.yandex.vertis.mockito.MockitoSupport

class TrustPaymentSystemServiceImplSpec extends PaymentSystemServiceSpec with MockitoSupport {

  lazy val psId = PaymentSystemIds.Trust

  private lazy val trustApi = mock[TrustApi]
  private lazy val purchaseDao = mock[TrustExternalPurchaseDao]

  private lazy val refundProcessor = new TrustRefundProcessor(trustApi, purchaseDao)

  override lazy val refundHelperService: RefundHelperService = new RefundHelperServiceImpl(
    database,
    accountTransactionsDao,
    refundProcessor,
    psTransactionsDao,
    dao
  )

}
