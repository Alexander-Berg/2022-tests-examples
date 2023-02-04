package ru.auto.salesman.tasks.processors.impl

import billing.finstat.autoru_dealers.AutoruDealersFinstat
import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.util.Timestamps
import ru.auto.salesman.service.call.cashback.CallCashbackService
import ru.auto.salesman.service.call.cashback.domain.CallId
import ru.auto.salesman.service.call.client.CallTargetService
import ru.auto.salesman.tasks.kafka.processors.impl.BillingLogCashbackProcessorImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.tasks.call.TestData
import ru.auto.salesman.util.telepony.ObjectId
import ru.yandex.vertis.billing.billing_event.BillingOperation.WithdrawPayload
import ru.yandex.vertis.billing.billing_event.BillingOperation.WithdrawPayload.EventType
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.{
  BillingDomain,
  TransactionInfo
}
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.TransactionInfo.TransactionType
import ru.yandex.vertis.billing.billing_event.{
  BillingOperation,
  CommonBillingInfo,
  RawEvent,
  SimpleProduct
}
import ru.yandex.vertis.billing.model.CallFact

class BillingLogCashbackProcessorImplSpec extends BaseSpec {

  import BillingLogCashbackProcessorImplSpec._

  val callTargetService = mock[CallTargetService]
  val callCashbackService = mock[CallCashbackService]

  val processor =
    new BillingLogCashbackProcessorImpl(callTargetService, callCashbackService)

  "BillingLogCashbackProcessorImpl" should {

    "process events for autoru used cars calls" in {
      (callTargetService.getTarget _)
        .expects(*)
        .returningZ(TestData.clientRecordActive)
      (callCashbackService.withdrawCashback _).expects(*, *, *).returningZ(())

      processor.process(PhoneShow.default).success.value
    }

    "skip events not for autoru used cars calls" in {
      (callTargetService.getTarget _).expects(*).never()
      (callCashbackService.withdrawCashback _).expects(*, *, *).never()
      (callCashbackService.cancelCashbackWithdrawal _).expects(*, *).never()

      processor.process(Indexing.default).success.value
    }

    "withdrawn cashback if event is correct and price > 0" in {
      (callTargetService.getTarget _)
        .expects(ObjectId(PhoneShow.objectId))
        .returningZ(TestData.clientRecordActive)

      (callCashbackService.withdrawCashback _)
        .expects(TestData.clientRecordActive, CallId(PhoneShow.callId), PhoneShow.price)
        .returningZ(())

      (callCashbackService.cancelCashbackWithdrawal _)
        .expects(TestData.clientRecordActive, CallId(PhoneShow.callId))
        .never()

      processor.process(PhoneShow.default).success.value
    }

    "cancel cashback withdrawal if event is correct and price == 0" in {
      (callTargetService.getTarget _)
        .expects(ObjectId(PhoneShow.objectId))
        .returningZ(TestData.clientRecordActive)

      (callCashbackService.withdrawCashback _)
        .expects(TestData.clientRecordActive, CallId(PhoneShow.callId), *)
        .never()

      (callCashbackService.cancelCashbackWithdrawal _)
        .expects(TestData.clientRecordActive, CallId(PhoneShow.callId))
        .returningZ(())

      processor
        .process(
          PhoneShow.default.copy(withdrawPayload =
            Some(PhoneShow.withdrawPayload.copy(expected = Some(0)))
          )
        )
        .success
        .value
    }

  }

}

object BillingLogCashbackProcessorImplSpec {

  val timestamp: Timestamp = Timestamp.defaultInstance.withSeconds(200)
  val operationTimestamp: Timestamp = Timestamp.defaultInstance.withSeconds(100)

  val defaultTransactionInfo: TransactionInfo =
    TransactionInfo(id = None, `type` = Some(TransactionType.WITHDRAW))

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
      id = Some("0c14f043558f35a22496e1a4ba6fc3cc42e339886d699999929094588a60fc35"),
      data = specificServiceData
    )
    val product = SimpleProduct(name = Some("turbo-package"))

    val withdrawPayload: WithdrawPayload = WithdrawPayload(
      rawEvent = Some(rawEvent),
      eventType = Some(EventType.INDEXING),
      actual = Some(1000L),
      product = Some(product)
    )

    val default: BillingOperation = BillingOperation(
      Some("test_id"),
      timestamp = Some(timestamp),
      domain = Some(BillingDomain.AUTORU),
      operationTimestamp = Some(startOfYear),
      orderState = None,
      Some(defaultTransactionInfo),
      Some(withdrawPayload),
      correctionPayload = None,
      incomingPayload = None,
      rebatePayload = None
    )

    val converted = AutoruDealersFinstat(
      transactionId = "0c14f043558f35a22496e1a4ba6fc3cc42e339886d699999929094588a60fc35",
      version = Timestamps.toMillis(Timestamp.toJavaProto(timestamp)),
      eventTime = Some(startOfYear),
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
    val objectId = s"dealer-$poiId"
    val callId = "xaQhSdfJwSU"
    val price = 1000L

    val defaultProduct = SimpleProduct(name = Some("call:cars:used"))

    val withdrawPayload: WithdrawPayload = WithdrawPayload(
      eventType = Some(EventType.PHONE_SHOW),
      expected = Some(price),
      actual = Some(price),
      product = Some(defaultProduct),
      callFact = Some(
        CommonBillingInfo.CallFact(
          callFact = Some(
            CallFact(
              id = Some(callId),
              objectId = Some(objectId),
              tag = Some("category=CARS#section=USED#offer_id=1111111111-111aa11")
            )
          )
        )
      )
    )

    val default: BillingOperation = BillingOperation(
      Some("call_test_id"),
      timestamp = Some(timestamp),
      domain = Some(BillingDomain.AUTORU),
      operationTimestamp = Some(startOfYear),
      orderState = None,
      Some(defaultTransactionInfo),
      Some(withdrawPayload),
      correctionPayload = None,
      incomingPayload = None,
      rebatePayload = None
    )

  }

}
