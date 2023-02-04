package ru.yandex.vertis.billing.banker.tasks.utils

import org.scalacheck.Gen
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentRequestGen,
  paymentRequestSourceGen,
  refundRequestGen,
  PaymentRequestParams,
  PaymentRequestSourceParams,
  Producer,
  RefundRequestParams,
  RefundRequestSourceParams,
  StateParams
}
import ru.yandex.vertis.billing.banker.model.util.PaymentTestingHelpers
import ru.yandex.vertis.billing.banker.model.{Funds, PaymentRequest, PaymentRequestId, RefundPaymentRequest, State}

object PaymentRequestsGens {

  private def paymentRequests(status: State.Status, stateStatuses: Set[State.StateStatus]): Gen[PaymentRequest] = {
    val stateParams = StateParams(
      status = Set(status),
      stateStatus = stateStatuses
    )
    for {
      amount <- Gen.chooseNum(1000000L, 100000000L)
      source = PaymentRequestSourceParams(
        amount = Some(amount),
        withPayGateContext = Some(false)
      )
      paymentParams = PaymentRequestParams(
        source = source
      ).withState(stateParams)
      paymentRequest <- paymentRequestGen(paymentParams)
    } yield paymentRequest
  }

  val NonProcessedPaymentRequestGen: Gen[PaymentRequestWithRefundRequests] =
    paymentRequests(
      State.Statuses.Created,
      State.StateStatuses.values
    ).map { paymentRequest =>
      PaymentRequestWithRefundRequests(paymentRequest)
    }

  val ProcessedPaymentRequestGen: Gen[PaymentRequestWithRefundRequests] =
    paymentRequests(
      State.Statuses.Processed,
      Set(State.StateStatuses.Valid, State.StateStatuses.Cancelled)
    ).map { paymentRequest =>
      PaymentRequestWithRefundRequests(paymentRequest)
    }

  case class PaymentRequestWithRefundRequests(
      paymentRequest: PaymentRequest,
      refundRequests: Seq[RefundPaymentRequest] = Seq.empty)

  private def refundFor(paymentRequestId: PaymentRequestId, status: State.Status, amount: Funds) = {
    val refundSourceParams = RefundRequestSourceParams(
      amount = Some(amount),
      refundFor = Some(paymentRequestId)
    )
    val stateParams = StateParams(
      status = Set(status),
      amount = Some(amount)
    )
    refundRequestGen(
      RefundRequestParams(
        state = Some(stateParams),
        source = refundSourceParams
      )
    ).next
  }

  val ProcessedPaymentRequestsWithRefundRequestGen: Gen[PaymentRequestWithRefundRequests] = for {
    paymentRequest <- paymentRequests(
      State.Statuses.Processed,
      Set(State.StateStatuses.Refunded, State.StateStatuses.PartlyRefunded)
    )
    (refundAmounts, unprocessed) = paymentRequest.state.get.stateStatus match {
      case State.StateStatuses.PartlyRefunded =>
        val parts = PaymentTestingHelpers.split(paymentRequest.source.amount).init
        if (parts.isEmpty) {
          (Seq(10000L), Gen.option(Gen.const(66666L)).next)
        } else {
          (parts, parts.lastOption)
        }
      case State.StateStatuses.Refunded =>
        (Seq(paymentRequest.source.amount), Gen.option(Gen.const(66666L)).next)
    }
    refundRequests = refundAmounts.map { amount =>
      refundFor(paymentRequest.id, State.Statuses.Processed, amount)
    }
    result = unprocessed match {
      case Some(amount) =>
        val unprocessedRefund = refundFor(paymentRequest.id, State.Statuses.Created, amount)
        refundRequests :+ unprocessedRefund
      case None =>
        refundRequests
    }
  } yield PaymentRequestWithRefundRequests(paymentRequest, result)

}
