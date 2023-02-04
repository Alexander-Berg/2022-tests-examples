package ru.yandex.vertis.billing.banker.service.effect

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.gens.{paymentForRefundGen, refundGen, PaymentSystemIdGen, Producer}
import ru.yandex.vertis.billing.banker.model.{PaymentMethod, RefundPaymentRequest, State}
import ru.yandex.vertis.billing.banker.service.effect.EffectOnRequestRefundHelperServiceSpec.DefaultEffectOnRequestRefundHelperServiceMockProvider
import ru.yandex.vertis.billing.banker.service.effect.EffectRefundHelperServiceHelpers.refundRequestRecordGen
import ru.yandex.vertis.billing.banker.service.effect.util.EffectOnRequestRefundHelperServiceBaseSpec

import scala.concurrent.Future

class EffectOnRequestRefundHelperServiceSpec
  extends DefaultEffectOnRequestRefundHelperServiceMockProvider
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  override type T = RefundHelperServiceMock with DefaultEffectOnRequestRefundHelperServiceMock

  override protected def createInstance: T = {
    new RefundHelperServiceMock with DefaultEffectOnRequestRefundHelperServiceMock
  }

  override protected def checkEffectNotCalled(instance: T): Unit = ()

  private def checkEffectCall(prepare: T => Future[Unit]): Unit = {
    forAll(PaymentSystemIdGen, refundRequestRecordGen(), Gen.option(refundGen())) { (psId, record, refund) =>
      val instance = createInstance
      instance.mockPsId(psId)
      instance.mockGetRefundRequests(Seq(record))
      instance.mockGetRefunds(refund.toSeq)
      val expectedRequest = RefundPaymentRequest(
        record.id,
        PaymentMethod(psId, record.method),
        record.account,
        record.source,
        refund
      )
      val payment = paymentForRefundGen(expectedRequest).next
      instance.mockPaymentGet(payment)
      instance.mockEffectCall(expectedRequest)

      val action = prepare(instance)

      action.futureValue
    }
  }

  private def checkGetRefundRequestsFail(prepare: T => Future[Unit]): Unit = {
    val instance = createInstance
    instance.mockGetRefundRequestsFail()

    val action = prepare(instance)

    action.futureValue
  }

  private def checkGetRefundRequestsNotExactlyOne(prepare: T => Future[Unit]): Unit = {
    val genList = for {
      count <- Gen.oneOf(Gen.const(0), Gen.chooseNum(2, 10))
      list <- Gen.listOfN(count, refundRequestRecordGen())
    } yield list

    forAll(genList) { records =>
      val instance = createInstance
      instance.mockGetRefundRequests(records)

      val action = prepare(instance)

      action.futureValue
    }
  }

  private def checkGetRefundsFail(prepare: T => Future[Unit]): Unit = {
    forAll(refundRequestRecordGen()) { record =>
      val instance = createInstance
      instance.mockGetRefundRequests(Seq(record))
      instance.mockGetRefundsFail()

      val action = prepare(instance)

      action.futureValue
    }
  }

  private def checkGetRefundsNotAtMostOne(prepare: T => Future[Unit]): Unit = {
    val genList = for {
      count <- Gen.chooseNum(2, 10)
      list <- Gen.listOfN(count, refundGen())
    } yield list

    forAll(refundRequestRecordGen(), genList) { (record, refunds) =>
      val instance = createInstance
      instance.mockGetRefundRequests(Seq(record))
      instance.mockGetRefunds(refunds)

      val action = prepare(instance)

      action.futureValue
    }
  }

  "EffectOnRequestRefundHelperService" should {
    "call effect" when {
      checkSuccess("all params are correct", checkEffectCall)
    }
    "not call event" when {
      checkSuccess("get refund requests throw exception", checkGetRefundRequestsFail)
      checkSuccess("get refund requests return not exactly one", checkGetRefundRequestsNotExactlyOne)
      checkSuccess("get refunds throw exception", checkGetRefundsFail)
      checkSuccess("get refunds return not at most one one", checkGetRefundsNotAtMostOne)
    }
  }

}

object EffectOnRequestRefundHelperServiceSpec {

  trait DefaultEffectOnRequestRefundHelperServiceMockProvider extends EffectOnRequestRefundHelperServiceBaseSpec {

    trait DefaultEffectOnRequestRefundHelperServiceMock extends EffectOnRequestRefundHelperServiceMock {

      private val effect = mockFunction[RefundPaymentRequest, Unit]

      def mockEffectCall(request: RefundPaymentRequest): Unit = {
        effect.expects(request): Unit
      }

      abstract override def effectOnRequest(request: RefundPaymentRequest, payment: State.Payment): Future[Unit] = {
        super.effectOnRequest(request, payment)
        effect.apply(request)
        Future.unit
      }

    }

  }

}
