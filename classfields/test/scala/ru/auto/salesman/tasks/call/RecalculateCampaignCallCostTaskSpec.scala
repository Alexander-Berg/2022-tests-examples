package ru.auto.salesman.tasks.call

import org.scalatest.concurrent.IntegrationPatience
import ru.auto.salesman.dao.ClientDao
import ru.auto.salesman.service.billingcampaign.{
  BillingCampaignService,
  CallCampaignNotFoundException,
  UpsertParams
}
import ru.auto.salesman.test.TestAkkaComponents._
import ru.auto.salesman.test.model.gens.{campaignHeaderGen, clientRecordGen}
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.yandex.vertis.generators.NetGenerators.asProducer

class RecalculateCampaignCallCostTaskSpec extends BaseSpec with IntegrationPatience {

  private val clientDao = stub[ClientDao]
  private val billingCampaignService = stub[BillingCampaignService]

  private val task = new RecalculateCampaignCallCostTask(
    clientDao,
    billingCampaignService,
    clientIds = None
  )

  "RecalculateCampaignCallCostTask" should {

    "invoke BillingCampaignService.updateCallCampaign with proper UpdateParams for client with calls auction available" in {
      val client = clientRecordGen(paidCallsAvailableGen = true).next
      val campaign = campaignHeaderGen().next
      (clientDao.get _).when(*).returningZ(List(client))
      (billingCampaignService.updateCallCarsNewCampaign _)
        .when(*)
        .returningZ(campaign)

      task.execute().futureValue

      (billingCampaignService.updateCallCarsNewCampaign _).verify(
        UpsertParams(
          clientId = client.clientId,
          dayLimit = None,
          weekLimit = None,
          costPerCall = None,
          enabled = None,
          recalculateCostPerCall = Some(true),
          createNew = false
        )
      )
    }

    "succeed if there is no campaign to update" in {
      val client = clientRecordGen(paidCallsAvailableGen = true).next
      (clientDao.get _).when(*).returningZ(List(client))
      (billingCampaignService.updateCallCarsNewCampaign _)
        .when(*)
        .throwingZ(CallCampaignNotFoundException(client.clientId))

      // futureValue asserts that task succeeds
      task.execute().futureValue
    }

    "fail if unexpected failure happened while updating call campaign" in {
      val client = clientRecordGen(paidCallsAvailableGen = true).next
      (clientDao.get _).when(*).returningZ(List(client))
      (billingCampaignService.updateCallCarsNewCampaign _)
        .when(*)
        .throwingZ(new TestException)

      // failed.futureValue asserts that task fails
      task.execute().failed.futureValue
    }
  }
}
