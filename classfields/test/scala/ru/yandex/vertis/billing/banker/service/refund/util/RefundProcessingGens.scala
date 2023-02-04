package ru.yandex.vertis.billing.banker.service.refund.util

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.gens.{getOr, idNoMoreLength, paymentGen, StateParams, UserGen}
import ru.yandex.vertis.billing.banker.model.{Funds, RefundPaymentRequestId, State, User}
import ru.yandex.vertis.billing.banker.service.refund.processor.RefundProcessor
import ru.yandex.vertis.billing.banker.service.refund.processor.RefundProcessor.RefundProcessSource

import scala.concurrent.Future

object RefundProcessingGens extends ScalaCheckPropertyChecks with ShrinkLowPriority {

  case class RefundProcessSourceParams(
      state: StateParams = StateParams(),
      expectedRefundedAmount: Option[Funds] = None,
      refundAmount: Option[Funds] = None,
      refundPaymentRequestId: Option[RefundPaymentRequestId] = None,
      comment: Option[String] = None,
      operator: Option[User] = None)

  def refundProcessSourceGen(
      params: RefundProcessSourceParams = RefundProcessSourceParams()): Gen[RefundProcessSource] =
    for {
      paymentAmount <-
        if (params.state.amount.isEmpty) {
          Gen.chooseNum(1000000L, 10000000L)
        } else {
          Gen.const(params.state.amount.get)
        }
      normStateParams = params.state.copy(
        `type` = Some(State.Types.Incoming),
        amount = Some(paymentAmount)
      )
      payment <- paymentGen(normStateParams)
      expectedRefundedAmount <- getOr(params.expectedRefundedAmount, Gen.chooseNum(100000L, payment.amount - 10000L))
      refundAmount = params.refundAmount.getOrElse(payment.amount - expectedRefundedAmount)
      refundPaymentRequestId = params.refundPaymentRequestId.getOrElse(payment.id)
      comment <- getOr(params.comment, idNoMoreLength(256, 128))
      operator <- getOr(params.operator, UserGen)
    } yield RefundProcessSource(
      payment,
      expectedRefundedAmount,
      refundAmount,
      refundPaymentRequestId,
      comment,
      operator
    )

  def onSource(stateParams: StateParams)(action: RefundProcessSource => Unit): Unit = {
    val gen = refundProcessSourceGen(
      RefundProcessSourceParams(stateParams)
    )
    forAll(gen)(action)
  }

  val PaymentInvoiceId = Some("666666")

  def onCorrectSource(action: RefundProcessSource => Unit): Unit = {
    val stateParams = StateParams(
      invoiceId = Some(PaymentInvoiceId)
    )
    onSource(stateParams)(action)
  }

  def onPaymentWithoutInvoiceId(action: RefundProcessSource => Unit): Unit = {
    val stateParams = StateParams(
      invoiceId = Some(None)
    )
    onSource(stateParams)(action)
  }

  def processorActionGen(processor: RefundProcessor): Gen[RefundProcessSource => Future[State.Refund]] =
    Gen.oneOf(processor.process _, processor.sync _)

}
