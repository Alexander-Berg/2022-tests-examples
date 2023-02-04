package ru.yandex.vertis.billing.event

import ru.yandex.vertis.billing.model_core.{CampaignId, CampaignSnapshot, OrderId}
import ru.yandex.vertis.billing.util.DateTimeInterval

import scala.util.Try

/**
  * Modify [[CampaignSnapshot]] fields for testing purposes
  *
  * @author alesavin
  */
class ModifiedCampaignSnapshotWithPayloadReader(
    reader: Reader[(CampaignSnapshot, Long)],
    campaignId: () => CampaignId,
    orderId: () => OrderId)
  extends Reader[(CampaignSnapshot, Long)] {

  override def read(interval: DateTimeInterval)(handler: ((CampaignSnapshot, Long)) => Unit): Try[Unit] =
    reader.read(interval) { case (snapshot, payload) =>
      val changed = snapshot.copy(campaignId = campaignId(), orderId = orderId())
      handler((changed, payload))
    }
}
