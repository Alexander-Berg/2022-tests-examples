package ru.yandex.vertis.billing.banker.service.effect

import akka.actor.{ActorRef, ActorSystem}
import ru.yandex.vertis.billing.banker.model.{
  AccountTransaction,
  AccountTransactions,
  PaymentMethod,
  PaymentSystemAccountTransactionId,
  PaymentSystemId,
  RefundPaymentRequest,
  RefundPaymentRequestId
}
import akka.testkit.{TestKit, TestProbe}
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.actor.NotifyActor
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.{ForId, TransactionsFilter}
import ru.yandex.vertis.billing.banker.dao.GlobalAccountTransactionDao
import ru.yandex.vertis.billing.banker.model.gens.{refundGen, AccountTransactionGen, PaymentSystemIdGen}
import ru.yandex.vertis.billing.banker.service.effect.RefundHelperServiceNotificationSupportSpec.RefundHelperServiceNotificationSupportMockProvider
import ru.yandex.vertis.billing.banker.service.effect.util.{
  EffectRefundHelperServiceBaseSpec,
  TestingEffectExecutionContextAware
}

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

class RefundHelperServiceNotificationSupportSpec
  extends TestKit(ActorSystem("RefundHelperServiceNotificationSupportSpec"))
  with RefundHelperServiceNotificationSupportMockProvider
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  override type T = RefundHelperServiceMock with RefundHelperServiceNotificationSupportMock

  override protected def createInstance: T = {
    val probe = TestProbe()
    new RefundHelperServiceMock with RefundHelperServiceNotificationSupportMock {
      override val notifyActorTestProbe: TestProbe = probe
    }
  }

  override protected def checkEffectNotCalled(instance: T): Unit = {
    instance.notifyActorTestProbe.expectNoMessage(100.millis)
  }

  private def checkNotificationRequest(prepare: T => Future[Unit]): Unit = {
    forAll(PaymentSystemIdGen, AccountTransactionGen) { (psId, tr) =>
      val instance = createInstance
      instance.mockPsId(psId)
      instance.mockTransactions(psId, RefundPaymentId, Iterable(tr))

      val action = prepare(instance)
      instance.notifyActorTestProbe.expectMsgPF() { case NotifyActor.Request.One(transaction, false) =>
        transaction shouldBe tr
      }

      action.futureValue
    }
  }

  "RefundHelperServiceNotificationSupport" should {
    "send request to notify actor" when {
      checkSuccess("all params are correct", checkNotificationRequest)
    }
  }

}

object RefundHelperServiceNotificationSupportSpec {

  trait RefundHelperServiceNotificationSupportMockProvider extends EffectRefundHelperServiceBaseSpec {

    trait RefundHelperServiceNotificationSupportMock
      extends RefundHelperServiceNotificationSupport
      with TestingEffectExecutionContextAware {

      private lazy val transactionsMock = {
        val m = mock[GlobalAccountTransactionDao]
        (() => m.prefix).expects().returning("account").anyNumberOfTimes()
        m
      }

      def mockTransactions(
          psId: PaymentSystemId,
          refundId: RefundPaymentRequestId,
          trs: Iterable[AccountTransaction]): Unit = {
        (transactionsMock.get _).expects(*).onCall { filters: Seq[TransactionsFilter] =>
          filters.head match {
            case ForId(p: PaymentSystemAccountTransactionId)
                if p.psId == psId && p.id == refundId && p.`type` == AccountTransactions.Refund =>
              Future.successful(trs)
            case _ =>
              fail("Unexpected")
          }
        }: Unit
      }

      def mockTransactionsFail(): Unit = {
        (transactionsMock.get _).expects(*).returns(Future.failed(ValidationException)): Unit
      }

      override protected def transactions: GlobalAccountTransactionDao = transactionsMock

      def notifyActorTestProbe: TestProbe

      override protected def notifyActorOpt: Option[ActorRef] = Some(notifyActorTestProbe.ref)

    }

  }

}
