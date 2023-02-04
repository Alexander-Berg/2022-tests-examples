package ru.auto.salesman.tasks.kafka.services

import org.mockito.Mockito
import org.mockito.Mockito.verify
import ru.auto.salesman.client.billing.BillingCampaignClient
import ru.auto.salesman.client.billing.model.{
  CampaignPatch,
  SimpleDealerCampaign,
  SimpleOrder
}
import ru.auto.salesman.dao.BalanceClientDao
import ru.auto.salesman.tasks.kafka.services.BillingTestData.BalanceClientCoreInfo
import ru.auto.salesman.tasks.kafka.services.ondelete.BillingCampaignDisablingService
import ru.auto.salesman.test.{DeprecatedMockitoBaseSpec, TestException}
import ru.yandex.vertis.mockito.util._

class BillingCampaignDisablingServiceSpec extends DeprecatedMockitoBaseSpec {

  private val balanceClientDao = mock[BalanceClientDao]
  private val billingCampaignClient = mock[BillingCampaignClient]

  private val billingCampaignDisablingService =
    new BillingCampaignDisablingService(billingCampaignClient, balanceClientDao)

  private val clientId = 1

  private val orderId = 123

  "BillingCampaignDisablingService.disableBillingCampaigns()" should {
    "work successfully" in {
      when(balanceClientDao.getCore(clientId))
        .thenReturnT(Some(BalanceClientCoreInfo))
      when(billingCampaignClient.getClientCampaigns(?, ?))
        .thenReturnZ(List(campaignHeader))
      when(billingCampaignClient.updateCampaign(?, ?, ?, ?))
        .thenReturnZ(BillingTestData.campaignHeader(false))

      billingCampaignDisablingService
        .disableBillingCampaigns(clientId)
        .success
        .value

      verify(balanceClientDao).getCore(clientId)
      verify(billingCampaignClient).updateCampaign(
        eq(BalanceClientCoreInfo.balanceClientId),
        ?,
        ?,
        eq(CampaignPatch(orderId = orderId, enabled = Some(false)))
      )
    }

    "fail if unable to resolve client" in {
      when(balanceClientDao.getCore(clientId))
        .thenThrowT(new TestException())

      billingCampaignDisablingService
        .disableBillingCampaigns(clientId)
        .failure
        .exception shouldBe a[TestException]
    }

    "fail of unable to receive call campaign" in {
      when(balanceClientDao.getCore(clientId))
        .thenReturnT(Some(BalanceClientCoreInfo))
      when(billingCampaignClient.getClientCampaigns(?, ?))
        .thenThrowZ(new TestException())

      billingCampaignDisablingService
        .disableBillingCampaigns(clientId)
        .failure
        .exception shouldBe a[TestException]
    }

    "fail if unable to disable call campaign" in {
      when(balanceClientDao.getCore(clientId))
        .thenReturnT(Some(BalanceClientCoreInfo))
      when(billingCampaignClient.getClientCampaigns(?, ?))
        .thenReturnZ(List(campaignHeader))
      when(billingCampaignClient.updateCampaign(?, ?, ?, ?))
        .thenThrowZ(new TestException())

      billingCampaignDisablingService
        .disableBillingCampaigns(clientId)
        .failure
        .exception shouldBe a[TestException]
    }

    "fail if no balance info found for client" in {
      when(balanceClientDao.getCore(clientId))
        .thenReturnT(None)
      billingCampaignDisablingService
        .disableBillingCampaigns(clientId)
        .failure
        .exception shouldBe a[NoSuchElementException]
    }

    "fail if unable to receive balance info for client" in {
      when(balanceClientDao.getCore(clientId))
        .thenThrowT(new TestException)
      billingCampaignDisablingService
        .disableBillingCampaigns(clientId)
        .failure
        .exception shouldBe a[TestException]
    }

    "disable selected campaign" in {
      val defaultId = "2"
      val wrongId = "12"

      Mockito.reset(balanceClientDao, billingCampaignClient)
      val campaigns =
        SimpleDealerCampaign(wrongId, SimpleOrder(orderId)) ::
          SimpleDealerCampaign(defaultId, SimpleOrder(orderId)) :: Nil

      when(balanceClientDao.getCore(clientId))
        .thenReturnT(Some(BalanceClientCoreInfo))

      when(billingCampaignClient.getClientCampaigns(?, ?))
        .thenReturnZ(campaigns)

      when(
        billingCampaignClient.updateCampaign(
          balanceClientId = ?,
          agencyId = ?,
          campaignId = eq(defaultId),
          patch = ?
        )
      )
        .thenReturnZ(BillingTestData.campaignHeader(false))

      billingCampaignDisablingService
        .disableClientCampaign(clientId, defaultId)
        .success
        .value

      verify(balanceClientDao).getCore(clientId)
      verify(billingCampaignClient).updateCampaign(
        eq(BalanceClientCoreInfo.balanceClientId),
        eq(BalanceClientCoreInfo.balanceAgencyId),
        campaignId = eq(defaultId),
        eq(CampaignPatch(orderId = orderId, enabled = Some(false)))
      )

    }
  }

  private def campaignHeader: SimpleDealerCampaign =
    SimpleDealerCampaign("123", SimpleOrder(orderId))

}
