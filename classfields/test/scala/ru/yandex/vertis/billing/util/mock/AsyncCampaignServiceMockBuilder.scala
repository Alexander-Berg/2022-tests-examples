package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.billing.model_core.CampaignHeader
import ru.yandex.vertis.billing.service.CampaignService.Filter
import ru.yandex.vertis.billing.service.CampaignService.Filter.ForCampaigns
import ru.yandex.vertis.billing.service.async.AsyncCampaignService
import ru.yandex.vertis.billing.util.RequestContext

import scala.concurrent.Future

/**
  * @author tolmach
  */
case class AsyncCampaignServiceMockBuilder() extends MockBuilder[AsyncCampaignService] {

  private val m: AsyncCampaignService = mock[AsyncCampaignService]

  def withGetForCampaigns(
      campaigns: Seq[CampaignHeader]
    )(implicit rc: RequestContext): AsyncCampaignServiceMockBuilder = {
    val campaignsMap = campaigns.groupBy(_.id).view.mapValues(_.head).toMap
    stub(m.get(_: Filter)(_: RequestContext)) { case (ForCampaigns(ids), `rc`) =>
      val result = ids.map { id =>
        campaignsMap(id)
      }
      Future.successful(result)
    }
    this
  }

  override def build: AsyncCampaignService = m

}
