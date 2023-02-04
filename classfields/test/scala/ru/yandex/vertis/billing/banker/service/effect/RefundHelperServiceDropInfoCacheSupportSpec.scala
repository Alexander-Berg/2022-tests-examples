package ru.yandex.vertis.billing.banker.service.effect

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.State.{StateStatuses, Statuses}
import ru.yandex.vertis.billing.banker.model.gens.{
  incomingPaymentGen,
  refundGen,
  PaymentSystemIdGen,
  Producer,
  StateParams
}
import ru.yandex.vertis.billing.banker.model.{Account, AccountId}
import ru.yandex.vertis.billing.banker.service.effect.RefundHelperServiceDropInfoCacheSupportSpec.RefundHelperServiceDropInfoCacheSupportMockProvider
import EffectRefundHelperServiceHelpers.refundRequestRecordGen
import ru.yandex.vertis.billing.banker.service.effect.util.EffectOnRequestRefundHelperServiceBaseSpec
import ru.yandex.vertis.caching.base.AsyncCache

import scala.concurrent.{ExecutionContext, Future}

class RefundHelperServiceDropInfoCacheSupportSpec
  extends RefundHelperServiceDropInfoCacheSupportMockProvider
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  override type T = RefundHelperServiceMock with RefundHelperServiceDropInfoCacheSupportMock

  override protected def createInstance: T = {
    new RefundHelperServiceMock with RefundHelperServiceDropInfoCacheSupportMock
  }

  override protected def checkEffectNotCalled(instance: T): Unit = ()

  private def checkCacheDrop(prepare: T => Future[Unit]): Unit = {
    val stateGen = refundGen(
      StateParams(
        status = Set(Statuses.Processed),
        stateStatus = Set(StateStatuses.Valid)
      )
    )
    forAll(PaymentSystemIdGen, refundRequestRecordGen(), stateGen, incomingPaymentGen()) {
      (psId, record, refund, payment) =>
        val instance = createInstance

        instance.mockPsId(psId)
        instance.mockGetRefundRequests(Seq(record))
        instance.mockGetRefunds(Seq(refund))
        instance.mockCacheDrop(record.account)
        instance.mockPaymentGet(payment)

        val action = prepare(instance)

        action.futureValue
    }
  }

  private def checkWithoutCacheDrop(prepare: T => Future[Unit]): Unit = {
    val createdStateParams = StateParams(
      status = Set(Statuses.Created),
      stateStatus = Set(StateStatuses.Valid, StateStatuses.Cancelled)
    )
    val processedStateParams = StateParams(
      status = Set(Statuses.Processed),
      stateStatus = Set(StateStatuses.Cancelled)
    )
    val stateGen = for {
      stateParams <- Gen.oneOf(createdStateParams, processedStateParams)
      refund <- refundGen(stateParams)
    } yield refund

    forAll(PaymentSystemIdGen, refundRequestRecordGen(), stateGen, incomingPaymentGen()) {
      (psId, record, refund, payment) =>
        val instance = createInstance

        instance.mockPsId(psId)
        instance.mockGetRefundRequests(Seq(record))
        instance.mockGetRefunds(Seq(refund))
        instance.mockPaymentGet(payment)

        val action = prepare(instance)

        action.futureValue
    }
  }

  "RefundHelperServiceDropInfoCacheSupport" should {
    "drop cache" when {
      checkSuccess("all params are correct", checkCacheDrop)
    }
    "do nothing" when {
      checkSuccess("not valid processed request", checkWithoutCacheDrop)
    }
  }

}

object RefundHelperServiceDropInfoCacheSupportSpec {

  trait RefundHelperServiceDropInfoCacheSupportMockProvider extends EffectOnRequestRefundHelperServiceBaseSpec {

    trait RefundHelperServiceDropInfoCacheSupportMock
      extends EffectOnRequestRefundHelperServiceMock
      with RefundHelperServiceDropInfoCacheSupport {

      override val cache: AsyncCache[AccountId, Info] = mock[AsyncCache[AccountId, Account.Info]]

      def mockCacheDrop(accountId: AccountId): Unit = {
        (cache.delete(_: AccountId)(_: ExecutionContext)).expects(accountId, *).returns(Future.unit): Unit
      }

    }

  }

}
