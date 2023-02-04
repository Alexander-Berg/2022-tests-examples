package ru.yandex.vertis.billing.tasks

import ru.yandex.vertis.billing.model_core.{
  OrderTransactionRequest,
  OrderTransactionResponse,
  TransactionId,
  WithdrawRequest2
}
import ru.yandex.vertis.billing.service.OrderService
import ru.yandex.vertis.billing.util.RequestContext

import scala.util.Try

/**
  * Memorizes holds passes trough `execute` and provides access to them.
  * For testing purposes only.
  *
  * @author dimas
  */
trait HoldMemorizedOrderService extends OrderService {

  private var holds = Set.empty[TransactionId]

  def getHolds: Set[TransactionId] = holds

  abstract override def execute2(
      request: OrderTransactionRequest
    )(implicit rc: RequestContext): Try[OrderTransactionResponse] = {
    request match {
      case w: WithdrawRequest2 =>
        holds = holds ++ w.holds
      case _ =>
    }
    super.execute2(request)
  }
}
