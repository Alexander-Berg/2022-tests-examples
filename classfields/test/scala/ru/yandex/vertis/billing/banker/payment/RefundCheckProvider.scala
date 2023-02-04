package ru.yandex.vertis.billing.banker.payment

import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.model.State.StateStatuses
import ru.yandex.vertis.billing.banker.model.{PaymentRequestId, User}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Targets}
import ru.yandex.vertis.billing.banker.model.gens.{paymentRequestSourceGen, PaymentRequestSourceParams, Producer}
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport
import ru.yandex.vertis.billing.banker.util.UserContext

import scala.reflect.ClassTag

trait RefundCheckProvider extends Matchers with AsyncSpecBase {

  protected val paymentSupport: PaymentSystemSupport

  protected val PurchasePaymentRequestSourceParams = PaymentRequestSourceParams(
    context = Some(Context(Targets.Purchase)),
    withReceipt = Some(true)
  )

  def checkRefundFail[T <: AnyRef: ClassTag](user: User, id: PaymentRequestId)(implicit rc: UserContext): Unit = {
    intercept[T] {
      paymentSupport.fullRefund(user, id, None, None).await
    }
    ()
  }

  def checkRefundSuccess(user: User, id: PaymentRequestId)(implicit rc: UserContext): Unit = {
    paymentSupport.fullRefund(user, id, None, None).futureValue
    paymentSupport.getPaymentRequest(user, id).futureValue.state match {
      case Some(payment) =>
        payment.stateStatus shouldBe StateStatuses.Refunded
        ()
      case other => fail(s"Unexpected $other")
    }
  }

}
