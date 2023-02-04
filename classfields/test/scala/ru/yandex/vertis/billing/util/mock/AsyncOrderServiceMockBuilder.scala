package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.billing.model_core.{Epoch, Order, OrderId, OrderTransaction, OrderTransactions, TransactionType}
import ru.yandex.vertis.billing.service.async.AsyncOrderService
import ru.yandex.vertis.billing.util.RequestContext

import scala.concurrent.Future

/**
  * @author tolmach
  */
case class AsyncOrderServiceMockBuilder(
    getByOrderIdsStubs: Seq[PartialFunction[(Iterable[OrderId], Boolean, RequestContext), Future[Iterable[Order]]]] =
      Seq.empty,
    getModifiedSinceTransactionsStubs: Seq[
      PartialFunction[(Epoch, Set[TransactionType], Boolean, RequestContext), Future[Iterable[OrderTransaction]]]
    ] = Seq.empty)
  extends MockBuilder[AsyncOrderService] {

  def withGetByOrderIds(ordersMap: Map[OrderId, Order])(implicit rc: RequestContext): AsyncOrderServiceMockBuilder = {
    val stub: PartialFunction[(Iterable[OrderId], Boolean, RequestContext), Future[Iterable[Order]]] = {
      case (orderIds, _, `rc`) if orderIds.forall(ordersMap.contains) =>
        Future.successful(orderIds.map(ordersMap.apply))
    }
    this.copy(getByOrderIdsStubs = getByOrderIdsStubs :+ stub)
  }

  def withGetModifiedSinceTransactions(
      epoch: Epoch,
      types: Set[TransactionType],
      result: Iterable[OrderTransaction]
    )(implicit rc: RequestContext): AsyncOrderServiceMockBuilder = {
    val stub: PartialFunction[(Epoch, Set[TransactionType], Boolean, RequestContext), Future[Iterable[OrderTransaction]]] = {
      case (`epoch`, t, _, `rc`) if t.forall(types.contains) =>
        Future.successful(result)
    }
    this.copy(getModifiedSinceTransactionsStubs = getModifiedSinceTransactionsStubs :+ stub)
  }

  def withGetModifiedSinceTransactions(
      epoch: Epoch,
      types: Set[TransactionType],
      result: Throwable
    )(implicit rc: RequestContext): AsyncOrderServiceMockBuilder = {
    val stub: PartialFunction[(Epoch, Set[TransactionType], Boolean, RequestContext), Future[Iterable[OrderTransaction]]] = {
      case (`epoch`, t, _, `rc`) if t.forall(types.contains) =>
        Future.failed(result)
    }
    this.copy(getModifiedSinceTransactionsStubs = getModifiedSinceTransactionsStubs :+ stub)
  }

  override def build: AsyncOrderService = {
    val m = mock[AsyncOrderService]

    val getByOrderIdsStub = getByOrderIdsStubs.reduceOption(_.orElse(_))
    getByOrderIdsStub.foreach { getByOrderIdsStub =>
      stub(m.get(_: Iterable[OrderId], _: Boolean)(_: RequestContext))(getByOrderIdsStub)
    }

    val getModifiedSinceTransactionsStub = getModifiedSinceTransactionsStubs.reduceOption(_.orElse(_))
    getModifiedSinceTransactionsStub.foreach { getModifiedSinceTransactionsStub =>
      stub(m.getModifiedSinceTransactions(_: Epoch, _: Set[TransactionType], _: Boolean)(_: RequestContext))(
        getModifiedSinceTransactionsStub
      )
    }

    m
  }

}
