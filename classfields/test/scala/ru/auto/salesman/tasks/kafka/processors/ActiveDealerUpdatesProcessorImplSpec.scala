package ru.auto.salesman.tasks.kafka.processors

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.cabinet.DealerAutoru.Dealer
import ru.auto.salesman.client.billing.BillingCampaignClient
import ru.auto.salesman.client.billing.model.CampaignPatch
import ru.auto.salesman.client.cabinet.CabinetClient
import ru.auto.salesman.client.cabinet.model.BalanceOrder
import ru.auto.salesman.environment.TimeZone
import ru.auto.salesman.model.DetailedClient
import ru.auto.salesman.service.{BillingService, DetailedClientSource}
import ru.auto.salesman.tasks.kafka.processors.impl.ActiveDealerUpdatesProcessorImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._

class ActiveDealerUpdatesProcessorImplSpec extends BaseSpec {

  private val billingService = mock[BillingService]
  private val billingCampaignClient = mock[BillingCampaignClient]
  private val detailedClientSource = mock[DetailedClientSource]
  private val cabinetClient = mock[CabinetClient]

  private val processor = new ActiveDealerUpdatesProcessorImpl(
    billingService,
    billingCampaignClient,
    detailedClientSource,
    cabinetClient
  )

  private val products = ActiveDealerUpdatesProcessorImpl.CampaignsProductsToBeActivated

  import ActiveDealerUpdatesProcessorImplSpec._

  "ActiveDealerUpdatesProcessor" should {
    "activate inactive campaigns of active client" in {
      forAll(
        clientRecordGen(),
        balanceRecordGen,
        Gen.listOfN(products.size, inactiveCampaignHeaderGen)
      ) { (client, balanceClient, campaignHeaders) =>
        val clientId = client.clientId
        val dealer = Dealer.newBuilder().setId(clientId).build()
        val detailedClient = DetailedClient(client, balanceClient)

        (cabinetClient.createBalanceOrder _)
          .expects(clientId, true)
          .returningZ(balanceOrder)

        (detailedClientSource.unsafeResolve _)
          .expects(clientId, true)
          .returningZ(DetailedClient(client, balanceClient))

        products.zip(campaignHeaders).foreach { case (productId, campaignHeader) =>
          (billingService.getProductCampaign _)
            .expects(detailedClient, productId)
            .returningZ(Some(campaignHeader))

          (billingCampaignClient.updateCampaign _)
            .expects(
              detailedClient.balanceClientId,
              detailedClient.balanceAgencyId,
              campaignHeader.getId,
              CampaignPatch(
                campaignHeader.getOrder.getId,
                enabled = Some(true)
              )
            )
            .returningZ(campaignHeader)
        }

        processor.process(dealer).success
      }
    }

    "skip missing campaigns" in {
      forAll(clientRecordGen(), balanceRecordGen, inactiveCampaignHeaderGen) {
        (client, balanceClient, campaignHeader) =>
          val clientId = client.clientId
          val dealer = Dealer.newBuilder().setId(clientId).build()
          val detailedClient = DetailedClient(client, balanceClient)

          (cabinetClient.createBalanceOrder _)
            .expects(clientId, true)
            .returningZ(balanceOrder)

          (detailedClientSource.unsafeResolve _)
            .expects(clientId, true)
            .returningZ(DetailedClient(client, balanceClient))

          products.size > 1 shouldBe true

          products.tail.foreach { productId =>
            (billingService.getProductCampaign _)
              .expects(detailedClient, productId)
              .returningZ(None)
          }

          (billingService.getProductCampaign _)
            .expects(detailedClient, products.head)
            .returningZ(Some(campaignHeader))

          (billingCampaignClient.updateCampaign _)
            .expects(
              detailedClient.balanceClientId,
              detailedClient.balanceAgencyId,
              campaignHeader.getId,
              CampaignPatch(campaignHeader.getOrder.getId, enabled = Some(true))
            )
            .returningZ(campaignHeader)

          processor.process(dealer).success
      }
    }

    "not update active campaigns" in {
      forAll(
        clientRecordGen(),
        balanceRecordGen,
        Gen.listOfN(products.size, activeCampaignHeaderGen)
      ) { (client, balanceClient, campaignHeaders) =>
        val clientId = client.clientId
        val dealer = Dealer.newBuilder().setId(clientId).build()
        val detailedClient = DetailedClient(client, balanceClient)

        (cabinetClient.createBalanceOrder _)
          .expects(clientId, true)
          .returningZ(balanceOrder)

        (detailedClientSource.unsafeResolve _)
          .expects(clientId, true)
          .returningZ(DetailedClient(client, balanceClient))

        products.zip(campaignHeaders).foreach { case (productId, campaignHeader) =>
          (billingService.getProductCampaign _)
            .expects(detailedClient, productId)
            .returningZ(Some(campaignHeader))
        }

        (billingCampaignClient.updateCampaign _)
          .expects(*, *, *, *)
          .never()

        processor.process(dealer).success
      }
    }
  }

}

object ActiveDealerUpdatesProcessorImplSpec {
  private val now = DateTime.now(TimeZone)

  private val balanceOrder = BalanceOrder(
    15006630L,
    99L,
    1L,
    7320375L,
    175L,
    0L,
    0L,
    1L,
    now,
    now,
    "Технические услуги в отношении Объявлений Заказчика в соответствии с «Условиями оказания услуг на сервисе Auto.ru» msk7471",
    0L,
    now,
    now
  )
}
