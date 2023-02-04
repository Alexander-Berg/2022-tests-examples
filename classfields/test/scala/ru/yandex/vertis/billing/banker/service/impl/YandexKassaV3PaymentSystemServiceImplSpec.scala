package ru.yandex.vertis.billing.banker.service.impl

import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.JdbcYandexKassaV3PaymentRequestExternalDao
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.service.refund.processor.YandexKassaV3RefundProcessor
import ru.yandex.vertis.billing.banker.service.{
  PaymentSystemServiceSpec,
  RefundHelperService,
  YandexKassaV3RefundHelperService
}
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3
import ru.yandex.vertis.mockito.MockitoSupport

class YandexKassaV3PaymentSystemServiceImplSpec extends PaymentSystemServiceSpec with MockitoSupport {

  lazy val psId = PaymentSystemIds.YandexKassaV3

  override def paymentSystemRefundRequestSpecialCases(): Unit = {
    "fail creation of refund payment request with amount less or equal to 1 rub" in {
      val paymentRequest = createProcessedRequest(Some(10000L)).futureValue

      intercept[IllegalArgumentException] {
        createRefundRequestWithFullAmount(paymentRequest.id, 50L).await
      }

      intercept[IllegalArgumentException] {
        createRefundRequestWithFullAmount(paymentRequest.id, 100L).await
      }
    }
    "fail creation of refund payment request when rest amount less or equal to 1 rub" in {
      val paymentRequest = createProcessedRequest(Some(1000L)).futureValue

      intercept[IllegalArgumentException] {
        createRefundRequestWithFullAmount(paymentRequest.id, 900L).await
      }
      intercept[IllegalArgumentException] {
        createRefundRequestWithFullAmount(paymentRequest.id, 909L).await
      }
    }
  }

  private val externalReqDao = new JdbcYandexKassaV3PaymentRequestExternalDao(database)

  private lazy val kassaMock = mock[YandexKassaApiV3]

  private lazy val refundProcessor = new YandexKassaV3RefundProcessor(kassaMock)

  override lazy val refundHelperService: RefundHelperService = new RefundHelperServiceImpl(
    database,
    accountTransactionsDao,
    refundProcessor,
    psTransactionsDao,
    dao
  ) with YandexKassaV3RefundHelperService {
    override def externalRequestDao: YandexKassaV3PaymentRequestExternalDao = externalReqDao
  }

}
