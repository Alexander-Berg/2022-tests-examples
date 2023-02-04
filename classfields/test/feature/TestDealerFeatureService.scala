package ru.auto.salesman.test.feature

import org.joda.time.DateTime
import ru.auto.salesman.model.{ClientId, RegionId}
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.service.DealerFeatureService.{
  migrationTaskCarsUsedCallsRegionsInitialValue,
  _
}
import zio.UIO

final case class TestDealerFeatureService(
    callPricePriorityPlacementRatioEnabled: Boolean =
      CallPricePriorityPlacementRatioEnabledInitialValue,
    priorityPlacementByPeriodsEnabled: Boolean = priorityPlacementByPeriodsInitialValue,
    loyaltyParamsActualizationEnabled: Boolean = loyaltyParamsActualizationInitialValue,
    loyaltyApplicationEnabled: Boolean = loyaltyApplicationInitialValue,
    loyaltyNewPlacementDiscountRegions: Set[RegionId] =
      loyaltyNewPlacementDiscountRergionsValue,
    callCampaignDepositDisabled: UIO[Boolean] =
      UIO.succeed(callCampaignDepositDisabledInitialValue),
    skipErrorChangeStatusDealerIds: Set[Long] = skipErrorChangeStatusDealerIdsValue,
    enableTaskOffersWithArchivePaidProducts: Boolean =
      enableTaskOffersWithArchivePaidProductsValue,
    enableTaskCategorizedOffersWithArchivePaidProducts: Boolean =
      enableTaskCategorizedOffersWithArchivePaidProductsValue,
    sendSeparateLoyaltyMessageToAmo: Boolean =
      sendSeparateLoyaltyMessageToAmoInitialValue,
    startTimeForCalculateDiscountWithKoopeks: DateTime =
      startTimeForCalculateDiscountWithKoopeksValue,
    carsUsedCallsRegions: Set[RegionId] = carsUsedCallsRegionsInitialValue,
    regionsDealerDisabledTurbo: Set[RegionId] = regionsDealerDisabledTurboValue,
    regionsDealerDisabledBoostAndPremium: Set[RegionId] =
      regionsDealerDisabledBoostAndPremiumValue,
    migrationTaskCarsUsedCallsRegions: Set[RegionId] =
      migrationTaskCarsUsedCallsRegionsInitialValue,
    carsUsedCallsCashbackEnabled: Boolean = false,
    carsUsedCallsCashbackEnabledDealersWhiteList: Set[ClientId] = Set.empty,
    carsUsedCallsCashbackEnabledRegionsWhiteList: Set[RegionId] = Set.empty,
    checkDealerOffersOwnership: Boolean = false
) extends DealerFeatureService
