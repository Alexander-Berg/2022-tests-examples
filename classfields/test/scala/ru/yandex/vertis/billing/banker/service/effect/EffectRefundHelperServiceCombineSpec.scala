package ru.yandex.vertis.billing.banker.service.effect

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.actor.{NotifyActor, ReceiptActor}
import ru.yandex.vertis.billing.banker.model.State.{StateStatuses, Statuses}
import ru.yandex.vertis.billing.banker.model.{PaymentMethod, RefundPaymentRequest}
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentForRefundGen,
  refundGen,
  AccountTransactionGen,
  PaymentSystemIdGen,
  Producer
}
import ru.yandex.vertis.billing.banker.service.effect.EffectRefundHelperServiceHelpers.refundRequestRecordGen
import ru.yandex.vertis.billing.banker.service.effect.RefundHelperServiceDropInfoCacheSupportSpec.RefundHelperServiceDropInfoCacheSupportMockProvider
import ru.yandex.vertis.billing.banker.service.effect.RefundHelperServiceNotificationSupportSpec.RefundHelperServiceNotificationSupportMockProvider
import ru.yandex.vertis.billing.banker.service.effect.RefundHelperServiceReceiptSupportSpec.RefundHelperServiceReceiptSupportMockProvider

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class EffectRefundHelperServiceCombineSpec
  extends TestKit(ActorSystem("EffectRefundHelperServiceCombineSpec"))
  with RefundHelperServiceNotificationSupportMockProvider
  with RefundHelperServiceDropInfoCacheSupportMockProvider
  with RefundHelperServiceReceiptSupportMockProvider
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  override type T =
    RefundHelperServiceMock
      with RefundHelperServiceDropInfoCacheSupportMock
      with RefundHelperServiceReceiptSupportMock
      with RefundHelperServiceNotificationSupportMock

  override protected def createInstance: T = {
    new RefundHelperServiceMock
      with RefundHelperServiceDropInfoCacheSupportMock
      with RefundHelperServiceReceiptSupportMock
      with RefundHelperServiceNotificationSupportMock {

      override val notifyActorTestProbe: TestProbe = TestProbe()

      override val receiptActorTestProbe: TestProbe = TestProbe()

    }
  }

  override protected def checkEffectNotCalled(instance: T): Unit = {
    instance.notifyActorTestProbe.expectNoMessage(100.millis)
    instance.receiptActorTestProbe.expectNoMessage(100.millis)
  }

  private def checkAllEffects(prepare: T => Future[Unit]): Unit = {
    forAll(
      PaymentSystemIdGen,
      refundRequestRecordGen(),
      Gen.option(refundGen()),
      AccountTransactionGen
    ) { (psId, record, refund, tr) =>
      val instance = createInstance

      instance.mockTransactions(psId, RefundPaymentId, Iterable(tr))

      val expectedRequest = RefundPaymentRequest(
        record.id,
        PaymentMethod(psId, record.method),
        record.account,
        record.source,
        refund
      )
      val payment = paymentForRefundGen(expectedRequest).next
      instance.mockPsId(psId)
      instance.mockGetRefundRequests(Seq(record))
      instance.mockGetRefunds(refund.toSeq)
      instance.mockPaymentGet(payment)
      val needMockCacheDrop = refund.exists { r =>
        r.status == Statuses.Processed && r.stateStatus == StateStatuses.Valid
      }
      instance.mockPsId(psId)
      if (needMockCacheDrop) {
        instance.mockCacheDrop(record.account)
      }

      val action = prepare(instance)

      instance.receiptActorTestProbe.expectMsgPF() {
        case ReceiptActor.Request.ProcessRefundPaymentRequest(request, _, false) =>
          request shouldBe expectedRequest
      }

      instance.notifyActorTestProbe.expectMsgPF() { case NotifyActor.Request.One(transaction, false) =>
        transaction shouldBe tr
      }

      action.futureValue
    }
  }

  "EffectRefundHelperServiceCombine" should {
    "call all effects" when {
      checkSuccess("all params are correct", checkAllEffects)
    }
  }

}
