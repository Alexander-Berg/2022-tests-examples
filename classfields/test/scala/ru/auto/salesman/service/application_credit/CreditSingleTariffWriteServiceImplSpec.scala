package ru.auto.salesman.service.application_credit

import ru.auto.salesman.client.billing.BillingCampaignClient
import ru.auto.salesman.client.billing.model.BillingProductType.{
  ApplicationCreditSingleTariffCarsNew,
  ApplicationCreditSingleTariffCarsUsed
}
import ru.auto.salesman.client.billing.model.CostType.CostPerIndexing
import ru.auto.salesman.client.billing.model.{CampaignPatch, CampaignRequest}
import ru.auto.salesman.model.AutoruDealer
import ru.auto.salesman.service.application_credit.TariffScope.{CarsNew, CarsUsed}
import ru.auto.salesman.service.{BillingService, DetailedClientSource}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.ProductGenerators

class CreditSingleTariffWriteServiceImplSpec extends BaseSpec with ProductGenerators {

  private val detailedClientSource = mock[DetailedClientSource]
  private val billingService = mock[BillingService]
  private val billingCampaignClient = mock[BillingCampaignClient]

  private val service =
    new CreditSingleTariffWriteServiceImpl(
      detailedClientSource,
      billingService,
      billingCampaignClient
    )

  private val dealer1 = AutoruDealer("dealer:123")

  private val ownerId1 = 1023L
  private val ownerId2 = 10234L

  private val goodCarsNew = buildGood(
    "application-credit:single:tariff:cars:new"
  )

  private val goodCarsUsed = buildGood(
    "application-credit:single:tariff:cars:used"
  )

  private val productNew = buildProduct(List(goodCarsNew))
  private val productUsed = buildProduct(List(goodCarsUsed))

  private val owner1 = buildOwner(ownerId1)
  private val owner2 = buildOwner(ownerId2)

  private val campaign1 = buildActiveCampaign(productNew, owner1)
  private val campaign2 = buildActiveCampaign(productUsed, owner2)

  private val balanceClientId = 111L
  private val balanceAgencyId = Some(11111L)
  private val orderId = 1111L

  "CreditSingleTariffOperationsServiceImpl.activate" should {

    "create application-credit:single:tariff:cars:new campaign if it doesn't exist yet for customer" in {
      val client =
        buildDetailedClient(
          dealer1.id,
          balanceClientId,
          balanceAgencyId,
          orderId
        )

      (detailedClientSource.unsafeResolve _)
        .expects(dealer1.id, /* withDeleted = */ false)
        .returningZ(client)

      (billingService.getCampaign _)
        .expects(client, ApplicationCreditSingleTariffCarsNew)
        .returningZ(None)

      (billingCampaignClient.createCampaign _)
        .expects(balanceClientId, balanceAgencyId, *)
        .returningZ(campaign1)

      service
        .turnOnTariff(dealer1, CarsNew)
        .success
    }

    "create billing campaign with product name = application-credit:single:tariff:cars:used on cars:used request" in {
      (detailedClientSource.unsafeResolve _)
        .expects(*, *)
        .returningZ(buildDetailedClient())

      (billingService.getCampaign _).expects(*, *).returningZ(None)

      (billingCampaignClient.createCampaign _)
        .expects(
          *,
          *,
          argAssert {
            (_: CampaignRequest).product.goods.loneElement.id shouldBe ApplicationCreditSingleTariffCarsUsed
          }
        )
        .returningZ(campaign2)

      service
        .turnOnTariff(dealer1, CarsUsed)
        .success
    }

    "create billing campaign with client's orderId" in {
      (detailedClientSource.unsafeResolve _)
        .expects(*, *)
        .returningZ(buildDetailedClient(orderId = orderId))

      (billingService.getCampaign _).expects(*, *).returningZ(None)

      (billingCampaignClient.createCampaign _)
        .expects(
          *,
          *,
          argAssert {
            (_: CampaignRequest).orderId shouldBe orderId
          }
        )
        .returningZ(campaign2)

      service
        .turnOnTariff(dealer1, CarsUsed)
        .success
    }

    "create billing campaign with cost type = CostPerIndexing" in {
      (detailedClientSource.unsafeResolve _)
        .expects(*, *)
        .returningZ(buildDetailedClient())

      (billingService.getCampaign _).expects(*, *).returningZ(None)

      (billingCampaignClient.createCampaign _)
        .expects(
          *,
          *,
          argAssert {
            (_: CampaignRequest).product.goods.loneElement.cost.constraints.value.costType shouldBe CostPerIndexing
          }
        )
        .returningZ(campaign2)

      service
        .turnOnTariff(dealer1, CarsUsed)
        .success
    }

    "create billing campaign with non-empty deposit" in {
      (detailedClientSource.unsafeResolve _)
        .expects(*, *)
        .returningZ(buildDetailedClient())

      (billingService.getCampaign _).expects(*, *).returningZ(None)

      (billingCampaignClient.createCampaign _)
        .expects(
          *,
          *,
          argAssert {
            (_: CampaignRequest).settings.deposit should not be None
          }
        )
        .returningZ(campaign2)

      service
        .turnOnTariff(dealer1, CarsUsed)
        .success
    }

    "enable billing campaign if it already exists" in {
      (detailedClientSource.unsafeResolve _)
        .expects(*, *)
        .returningZ(
          buildDetailedClient(
            dealer1.id,
            balanceClientId,
            balanceAgencyId,
            orderId
          )
        )

      (billingService.getCampaign _).expects(*, *).returningZ(Some(campaign2))

      (billingCampaignClient.updateCampaign _)
        .expects(
          balanceClientId,
          balanceAgencyId,
          campaign2.getId,
          CampaignPatch(orderId, enabled = Some(true))
        )
        .returningZ(campaign2)

      service
        .turnOnTariff(dealer1, CarsUsed)
        .success
    }
  }

  "CreditSingleTariffOperationsServiceImpl.deactivate" should {

    "disable existing campaign" in {
      (detailedClientSource.unsafeResolve _)
        .expects(*, *)
        .returningZ(
          buildDetailedClient(
            dealer1.id,
            balanceClientId,
            balanceAgencyId,
            orderId
          )
        )

      (billingService.getCampaign _).expects(*, *).returningZ(Some(campaign2))

      (billingCampaignClient.updateCampaign _)
        .expects(
          balanceClientId,
          balanceAgencyId,
          campaign2.getId,
          CampaignPatch(orderId, enabled = Some(false))
        )
        .returningZ(campaign2)

      service
        .turnOffTariff(dealer1, CarsUsed)
        .success
    }
  }

}
