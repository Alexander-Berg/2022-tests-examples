package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.billing.model_core.Epoch
import ru.yandex.vertis.billing.service.ArchiveService.{ArchiveRecord, ArchiveRecordId}
import ru.yandex.vertis.billing.service.ArchiveService.Filter.CampaignUpdatedSinceBatchOrdered
import ru.yandex.vertis.billing.service.async.AsyncArchiveService
import ru.yandex.vertis.billing.util.RequestContext

import scala.concurrent.Future

/**
  * @author tolmach
  */
case class AsyncArchiveServiceMockBuilder() extends MockBuilder[AsyncArchiveService] {

  private val m: AsyncArchiveService = mock[AsyncArchiveService]

  def withGetCampaignUpdatedSinceBatchOrdered(
      epoch: Epoch,
      id: Option[ArchiveRecordId],
      batch: Seq[ArchiveRecord],
      batchSize: Int
    )(implicit rc: RequestContext): AsyncArchiveServiceMockBuilder = {
    val filter = CampaignUpdatedSinceBatchOrdered(epoch, id, batchSize)
    when(m.get(filter, readFromMaster = true)(rc)).thenReturn(Future.successful(batch))
    this
  }

  def build: AsyncArchiveService = m

}
