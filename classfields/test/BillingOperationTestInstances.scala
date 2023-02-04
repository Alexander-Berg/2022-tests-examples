package vsmoney.dealer_finstat_writer.autoru_monetization

import billing.finstat.autoru_dealers.AutoruDealersFinstat
import cats.syntax.option._
import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.util.Timestamps
import ru.yandex.vertis.billing.billing_event.BillingOperation.WithdrawPayload
import ru.yandex.vertis.billing.billing_event.BillingOperation.WithdrawPayload.EventType
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.TransactionInfo.TransactionType
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.{BillingDomain, TransactionInfo}
import ru.yandex.vertis.billing.billing_event.{BillingOperation, CommonBillingInfo, RawEvent, SimpleProduct}
import ru.yandex.vertis.billing.model.CallFact

object BillingOperationTestInstances {

  val timestamp: Timestamp = Timestamp.defaultInstance.withSeconds(200)
  val operationTimestamp: Timestamp = Timestamp.defaultInstance.withSeconds(100)
  val defaultTransactionInfo: TransactionInfo = TransactionInfo(id = None, `type` = Some(TransactionType.WITHDRAW))

  // Saturday, 1 January 2022 г., 0:00:00 (2022-01-01T:00.00.000000Z)
  val startOfYear: Timestamp = Timestamp.defaultInstance.withSeconds(1640995200)

  object Indexing {

    val specificServiceData: Map[String, String] = Map(
      "service.payload.offerId" -> "9999999999-999aa99",
      "service.payload.offerSection" -> "CARS",
      "service.payload.offerCategory" -> "NEW",
      "service.payload.serviceClientId" -> "dealer:20101",
      "service.payload.serviceClientAgencyId" -> "23719",
      "service.payload.serviceClientCompanyId" -> "99888"
    )

    val rawEvent = RawEvent(
      id = "0c14f043558f35a22496e1a4ba6fc3cc42e339886d699999929094588a60fc35".some,
      data = specificServiceData
    )
    val product = SimpleProduct(name = Some("turbo-package"))

    val withdrawPayload: WithdrawPayload = WithdrawPayload(
      rawEvent = rawEvent.some,
      eventType = EventType.INDEXING.some,
      actual = 1000L.some,
      product = product.some
    )

    val default: BillingOperation = BillingOperation(
      "test_id".some,
      timestamp = timestamp.some,
      domain = BillingDomain.AUTORU.some,
      operationTimestamp = startOfYear.some,
      orderState = None,
      defaultTransactionInfo.some,
      withdrawPayload.some,
      correctionPayload = None,
      incomingPayload = None,
      rebatePayload = None
    )

    val converted = AutoruDealersFinstat(
      transactionId = "0c14f043558f35a22496e1a4ba6fc3cc42e339886d699999929094588a60fc35",
      version = Timestamps.toMillis(Timestamp.toJavaProto(timestamp)),
      eventTime = startOfYear.some,
      spentKopecks = 1000L,
      product = "turbo-package",
      clientId = "dealer:20101",
      offerId = "9999999999-999aa99",
      category = "NEW",
      section = "CARS",
      agencyId = "23719",
      companyGroup = "99888"
    )
  }

  object PhoneShow {

    val poiId = 31711L
    val clientId = 20101L

    val defaultProduct = SimpleProduct(name = Some("call"))

    val withdrawPayload: WithdrawPayload = WithdrawPayload(
      eventType = EventType.PHONE_SHOW.some,
      actual = 1000L.some,
      product = defaultProduct.some,
      callFact = CommonBillingInfo
        .CallFact(
          callFact = CallFact(
            id = "xaQhSdfJwSU".some,
            objectId = s"dealer-$poiId".some,
            tag = "category=CARS#section=NEW#offer_id=1111111111-111aa11".some
          ).some
        )
        .some
    )

    val default: BillingOperation = BillingOperation(
      "call_test_id".some,
      timestamp = timestamp.some,
      domain = BillingDomain.AUTORU.some,
      operationTimestamp = startOfYear.some,
      orderState = None,
      defaultTransactionInfo.some,
      withdrawPayload.some,
      correctionPayload = None,
      incomingPayload = None,
      rebatePayload = None
    )

    val converted = AutoruDealersFinstat(
      transactionId = "xaQhSdfJwSU",
      version = Timestamps.toMillis(Timestamp.toJavaProto(timestamp)),
      eventTime = startOfYear.some,
      spentKopecks = 1000L,
      product = "call",
      clientId = clientId.toString,
      offerId = "1111111111-111aa11",
      category = "CARS",
      section = "NEW"
    )
  }

}
