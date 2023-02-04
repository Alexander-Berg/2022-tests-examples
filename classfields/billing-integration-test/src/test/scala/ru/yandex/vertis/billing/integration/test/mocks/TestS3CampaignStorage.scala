package ru.yandex.vertis.billing.integration.test.mocks

import ru.yandex.vertis.billing.dao.impl.mds.S3CampaignStorage

import scala.concurrent.{ExecutionContext, Future}

class TestS3CampaignStorage extends S3CampaignStorage {
  import ru.yandex.vertis.billing.Model

  @volatile
  private var campaigns = Iterable.empty[Model.CampaignHeader]

  override def upsert(campaigns: Iterable[Model.CampaignHeader])(implicit ec: ExecutionContext): Future[Unit] = {
    this.campaigns = campaigns
    Future.successful(())
  }

  override def getCampaigns(implicit ec: ExecutionContext): Future[Iterable[Model.CampaignHeader]] =
    Future.successful(campaigns)

  def clear() = {
    campaigns = Iterable.empty[Model.CampaignHeader]
  }
}
