package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.billing.dao.NotifyClientDao
import ru.yandex.vertis.billing.dao.NotifyClientDao.{
  ClientIdWithTid,
  LastBeforeForClientsByTid,
  UpdatedSinceBatchOrdered
}
import ru.yandex.vertis.billing.model_core.{Epoch, NotifyClient, NotifyClientRecordId}
import ru.yandex.vertis.billing.service.async.AsyncNotifyClientService

import scala.concurrent.Future

/**
  * @author tolmach
  */
case class AsyncNotifyClientServiceMockBuilder(
    getStubs: Seq[PartialFunction[NotifyClientDao.Filter, Future[Iterable[NotifyClient]]]] = Seq.empty)
  extends MockBuilder[AsyncNotifyClientService] {

  def withGetUpdatedSinceBatchOrdered(
      epoch: Epoch,
      id: Option[NotifyClientRecordId],
      batch: Seq[NotifyClient],
      batchSize: Int): AsyncNotifyClientServiceMockBuilder = {
    val filter = UpdatedSinceBatchOrdered(epoch, id, batchSize)
    val newStub: PartialFunction[NotifyClientDao.Filter, Future[Iterable[NotifyClient]]] = { case `filter` =>
      Future.successful(batch)
    }
    this.copy(getStubs = getStubs :+ newStub)
  }

  def withGetLastBeforeForClientsByTid(
      clientIdsWithTid: Seq[ClientIdWithTid],
      batch: Seq[NotifyClient]): AsyncNotifyClientServiceMockBuilder = {
    val newStub: PartialFunction[NotifyClientDao.Filter, Future[Iterable[NotifyClient]]] = {
      case LastBeforeForClientsByTid(actual) if clientIdsWithTid.forall(actual.contains) =>
        Future.successful(batch)
    }
    this.copy(getStubs = getStubs :+ newStub)
  }

  def build: AsyncNotifyClientService = {
    val m: AsyncNotifyClientService = mock[AsyncNotifyClientService]
    val getStub = getStubs.reduce(_.orElse(_))
    stub(m.get(_: NotifyClientDao.Filter))(getStub)
    m
  }

}
