package ru.auto.tests.publicapi.operations.feeds.settings

import ru.auto.tests.publicapi.operations.feeds.settings.cars.{DeleteCarsFeedsSettingsOps, SaveCarsFeedsSettingsOps}
import ru.auto.tests.publicapi.operations.feeds.settings.moto.{DeleteMotoFeedsSettingsOps, SaveMotoFeedsSettingsOps}
import ru.auto.tests.publicapi.operations.feeds.settings.trucks.{DeleteTrucksFeedsSettingsOps, SaveTrucksFeedsSettingsOps}

object FeedsSettingsOps {

  trait AllDeleteFeedsSettingsOps
    extends DeleteCarsFeedsSettingsOps with DeleteMotoFeedsSettingsOps with DeleteTrucksFeedsSettingsOps

  trait AllSaveFeedsSettingsOps
    extends SaveCarsFeedsSettingsOps with SaveMotoFeedsSettingsOps with SaveTrucksFeedsSettingsOps

  trait AllFeedsSettingsOps
    extends GetFeedsSettingsOps with AllSaveFeedsSettingsOps with AllDeleteFeedsSettingsOps

}
