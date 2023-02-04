package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.billing.dao.CampaignHistoryDao.CampaignHistoryPoint
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.Filter.UpdatedSinceBatchOrdered
import ru.yandex.vertis.billing.model_core.{CampaignId, Epoch}
import ru.yandex.vertis.billing.service.async.AsyncCampaignHistoryService
import ru.yandex.vertis.billing.util.RequestContext

import scala.concurrent.Future

/**
  * @author tolmach
  */
case class AsyncCampaignHistoryServiceMockBuilder() extends MockBuilder[AsyncCampaignHistoryService] {

  private val m: AsyncCampaignHistoryService = mock[AsyncCampaignHistoryService]

  def withGetUpdateSinceBatchOrderedMock(
      epoch: Epoch,
      id: Option[CampaignId],
      batch: Seq[CampaignHistoryPoint],
      batchSize: Int
    )(implicit rc: RequestContext): AsyncCampaignHistoryServiceMockBuilder = {
    val filter = UpdatedSinceBatchOrdered(epoch, id, batchSize)
    when(m.get(filter)(rc)).thenReturn(Future.successful(batch))
    this
  }

  override def build: AsyncCampaignHistoryService = m

}
