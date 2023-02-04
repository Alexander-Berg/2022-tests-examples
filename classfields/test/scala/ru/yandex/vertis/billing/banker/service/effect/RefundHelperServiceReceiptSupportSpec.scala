package ru.yandex.vertis.billing.banker.service.effect

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.actor.ReceiptActor
import ru.yandex.vertis.billing.banker.model.gens.{paymentForRefundGen, refundGen, PaymentSystemIdGen, Producer}
import ru.yandex.vertis.billing.banker.model.{PaymentMethod, RefundPaymentRequest}
import ru.yandex.vertis.billing.banker.service.effect.RefundHelperServiceReceiptSupportSpec.RefundHelperServiceReceiptSupportMockProvider
import ru.yandex.vertis.billing.banker.service.effect.util.EffectOnRequestRefundHelperServiceBaseSpec
import EffectRefundHelperServiceHelpers.refundRequestRecordGen

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

class RefundHelperServiceReceiptSupportSpec
  extends TestKit(ActorSystem("RefundHelperServiceReceiptSupportSpec"))
  with RefundHelperServiceReceiptSupportMockProvider
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  override type T = RefundHelperServiceMock with RefundHelperServiceReceiptSupportMock

  override protected def createInstance: T = {
    val probe = TestProbe()
    new RefundHelperServiceMock with RefundHelperServiceReceiptSupportMock {
      override val receiptActorTestProbe: TestProbe = probe
    }
  }

  override protected def checkEffectNotCalled(instance: T): Unit = {
    instance.receiptActorTestProbe.expectNoMessage(100.millis)
  }

  private def checkSendReceiptRequest(prepare: T => Future[Unit]): Unit = {
    forAll(PaymentSystemIdGen, refundRequestRecordGen(), Gen.option(refundGen())) { (psId, record, refund) =>
      val instance = createInstance
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

      val action = prepare(instance)
      instance.receiptActorTestProbe.expectMsgPF() {
        case ReceiptActor.Request.ProcessRefundPaymentRequest(request, _, false) =>
          request shouldBe expectedRequest
      }

      action.futureValue
    }
  }

  "RefundHelperServiceReceiptSupport" should {
    "send request to receipt actor" when {
      checkSuccess("all params are correct", checkSendReceiptRequest)
    }
  }

}

object RefundHelperServiceReceiptSupportSpec {

  trait RefundHelperServiceReceiptSupportMockProvider extends EffectOnRequestRefundHelperServiceBaseSpec {

    trait RefundHelperServiceReceiptSupportMock
      extends EffectOnRequestRefundHelperServiceMock
      with RefundHelperServiceReceiptSupport {

      def receiptActorTestProbe: TestProbe

      override protected def receiptActorOpt: Option[ActorRef] = Some(receiptActorTestProbe.ref)

    }

  }

}
