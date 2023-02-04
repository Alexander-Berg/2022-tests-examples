package ru.auto.tests.publicapi.operations.feeds

import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedSettings
import ru.auto.tests.publicapi.operations.feeds.settings.cars.{DeleteCarsFeedsSettingsOps, SaveCarsFeedsSettingsOps}
import ru.auto.tests.publicapi.operations.feeds.settings.moto.{DeleteMotoFeedsSettingsOps, SaveMotoFeedsSettingsOps}
import ru.auto.tests.publicapi.operations.feeds.settings.trucks.{DeleteTrucksFeedsSettingsOps, SaveTrucksFeedsSettingsOps}

package object settings {

  trait AllDeleteFeedsSettingsOps
    extends DeleteCarsFeedsSettingsOps with DeleteMotoFeedsSettingsOps with DeleteTrucksFeedsSettingsOps

  trait AllSaveFeedsSettingsOps
    extends SaveCarsFeedsSettingsOps with SaveMotoFeedsSettingsOps with SaveTrucksFeedsSettingsOps

  trait AllFeedsSettingsOps
    extends GetFeedsSettingsOps with AllSaveFeedsSettingsOps with AllDeleteFeedsSettingsOps

  def feedSettings(url: String): AutoApiFeedprocessorFeedFeedSettings =
    new AutoApiFeedprocessorFeedFeedSettings()
      .source(url)
      .isActive(false)
      .leaveServices(false)
      .leaveAddedImages(false)
      .deleteSale(false)
      .maxDiscountEnabled(false)

  val defaultFeedSettings: AutoApiFeedprocessorFeedFeedSettings =
    feedSettings("feed.xml")

}
