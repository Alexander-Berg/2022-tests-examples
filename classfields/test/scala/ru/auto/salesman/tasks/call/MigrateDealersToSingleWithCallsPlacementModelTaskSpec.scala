package ru.auto.salesman.tasks.call

import ru.auto.salesman.dao.ClientDao.ForStatusNotDeletedInRegions
import ru.auto.salesman.dao.{AdsRequestDao, ClientDao}
import ru.auto.salesman.model.{AdsRequestTypes, RegionId}
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.service.billingcampaign.{BillingCampaignService, UpsertParams}
import ru.auto.salesman.service.user.PriceService.OneRublePrice
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{campaignHeaderGen, clientRecordGen}

class MigrateDealersToSingleWithCallsPlacementModelTaskSpec extends BaseSpec {

  private val clientDao = mock[ClientDao]
  private val billingCampaignService = mock[BillingCampaignService]
  private val adsRequestDao = mock[AdsRequestDao]
  private val dealerFeatureService = mock[DealerFeatureService]

  private val task = new MigrateDealersToSingleWithCallsPlacementModelTask(
    clientDao,
    adsRequestDao,
    billingCampaignService,
    dealerFeatureService
  )

  "MigrateDealersToSingleWithCallsPlacementModelTask" should {
    val acceptRegionsWithLbu: Set[RegionId] = Set(RegionId(1L), RegionId(1074L))
    "create RC for user with active cars:used single campaign " in {
      forAll(clientRecordGen(), campaignHeaderGen()) { (client, campaign) =>
        (dealerFeatureService.migrationTaskCarsUsedCallsRegions _)
          .expects()
          .returning(acceptRegionsWithLbu)
        (clientDao.get _)
          .expects(
            ForStatusNotDeletedInRegions(acceptRegionsWithLbu)
          )
          .returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(client.clientId))
          .returningT(
            List(
              AdsRequestDao.Record(client.clientId, AdsRequestTypes.CarsUsed)
            )
          )
        (billingCampaignService.updateCallCarsUsedCampaignWithoutCheckRegions _)
          .expects(
            UpsertParams(
              client.clientId,
              dayLimit = None,
              weekLimit = None,
              costPerCall = Some(OneRublePrice),
              enabled = Some(true),
              recalculateCostPerCall = None,
              createNew = true
            )
          )
          .returningZ(campaign)
        task.execute().success.value

      }
    }

    "not create RC for user if active single campaign is not for CARS:USED" in {
      forAll(clientRecordGen(), campaignHeaderGen()) { (client, campaign) =>
        (dealerFeatureService.migrationTaskCarsUsedCallsRegions _)
          .expects()
          .returning(acceptRegionsWithLbu)
        (clientDao.get _)
          .expects(
            ForStatusNotDeletedInRegions(acceptRegionsWithLbu)
          )
          .returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(client.clientId))
          .returningT(
            List(
              AdsRequestDao.Record(client.clientId, AdsRequestTypes.Commercial)
            )
          )

        (billingCampaignService.updateCallCarsUsedCampaign _)
          .expects(*)
          .never()

        task.execute().success.value

      }
    }

    "not create RC for user if no active campaigns found" in {
      forAll(clientRecordGen()) { client =>
        (dealerFeatureService.migrationTaskCarsUsedCallsRegions _)
          .expects()
          .returning(acceptRegionsWithLbu)
        (clientDao.get _)
          .expects(
            ForStatusNotDeletedInRegions(acceptRegionsWithLbu)
          )
          .returningZ(List(client))

        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(client.clientId))
          .returningT(List.empty[AdsRequestDao.Record])

        (billingCampaignService.updateCallCarsUsedCampaign _)
          .expects(*)
          .never()

        task.execute().success.value

      }
    }
  }

}
