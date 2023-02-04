package ru.yandex.vertis.billing.event

import ru.yandex.vertis.billing.model_core.CampaignSnapshot
import ru.yandex.vertis.billing.util.DateTimeInterval

import scala.util.Try

/**
  * Modify [[CampaignSnapshot]] fields for testing purposes
  *
  * @author alesavin
  */
class ModifiedCampaignSnapshotReader(reader: Reader[CampaignSnapshot], f: CampaignSnapshot => CampaignSnapshot)
  extends Reader[CampaignSnapshot] {

  override def read(interval: DateTimeInterval)(handler: (CampaignSnapshot) => Unit): Try[Unit] =
    reader.read(interval) { snapshot =>
      handler(f(snapshot))
    }
}
