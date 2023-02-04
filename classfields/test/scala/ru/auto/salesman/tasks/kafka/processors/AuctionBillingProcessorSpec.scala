package ru.auto.salesman.tasks.kafka.processors

import ru.auto.salesman.dao.BalanceClientDao
import ru.auto.salesman.service.auction.AuctionService
import ru.auto.salesman.tasks.kafka.processors.impl.AuctionBillingProcessorImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{balanceRecordGen, campaignHeaderGen}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.{Cost, Good, Product}
import ru.yandex.vertis.billing.billing_event.CampaignActiveStatusChangeEvent
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.BillingDomain
import ru.yandex.vertis.billing.model.CampaignHeader

class AuctionBillingProcessorSpec extends BaseSpec {

  private val balanceClientDao = mock[BalanceClientDao]
  private val auctionService = mock[AuctionService]

  private val processor =
    new AuctionBillingProcessorImpl(auctionService, balanceClientDao)

  "AuctionBillingProcessor" should {
    "process only campaigns with AUTORU domain" in {
      (balanceClientDao.getBillingAutoruClientsMapping _)
        .expects(*)
        .never()

      (auctionService.blockUserAuctionForCarsUsed _)
        .expects(*)
        .never()

      (auctionService.unblockUserAuctionForCarsUsed _)
        .expects(*)
        .never()

      processor
        .process(
          CampaignActiveStatusChangeEvent(
            domain = Some(BillingDomain.REALTY)
          )
        )
        .success
        .value
    }

    "ignore campaigns with not call:cars:used custom good id" in {
      forAll(campaignHeaderGen()) { campaignGen =>
        val callCampaign = setCustomGood(campaignGen, "call")

        (balanceClientDao.getBillingAutoruClientsMapping _)
          .expects(*)
          .never()

        (auctionService.blockUserAuctionForCarsUsed _)
          .expects(*)
          .never()

        (auctionService.unblockUserAuctionForCarsUsed _)
          .expects(*)
          .never()

        processor
          .process(
            CampaignActiveStatusChangeEvent(
              domain = Some(BillingDomain.AUTORU),
              campaignHeader = Some(callCampaign)
            )
          )
          .success
          .value
      }
    }

    "block user auction when event.is_active status = false" in {
      forAll(balanceRecordGen, campaignHeaderGen()) { (balanceClient, campaignGen) =>
        val callCarsUsedCampaign =
          setCustomGood(campaignGen, "call:cars:used")

        (balanceClientDao.getBillingAutoruClientsMapping _)
          .expects(Set(balanceClient.balanceClientId))
          .returningZ(
            Map(balanceClient.balanceClientId -> balanceClient.clientId)
          )

        (auctionService.blockUserAuctionForCarsUsed _)
          .expects(balanceClient.clientId)
          .returningZ(())

        processor
          .process(
            CampaignActiveStatusChangeEvent(
              domain = Some(BillingDomain.AUTORU),
              campaignHeader = Some(callCarsUsedCampaign),
              isActive = Some(false),
              clientId = Some(balanceClient.balanceClientId),
              campaignId = Some("test-id")
            )
          )
          .success
          .value
      }
    }

    "unblock user auction when event.is_active status = true" in {
      forAll(balanceRecordGen, campaignHeaderGen()) { (balanceClient, campaignGen) =>
        val callCarsUsedCampaign =
          setCustomGood(campaignGen, "call:cars:used")

        (balanceClientDao.getBillingAutoruClientsMapping _)
          .expects(Set(balanceClient.balanceClientId))
          .returningZ(
            Map(balanceClient.balanceClientId -> balanceClient.clientId)
          )

        (auctionService.unblockUserAuctionForCarsUsed _)
          .expects(balanceClient.clientId)
          .returningZ(())

        processor
          .process(
            CampaignActiveStatusChangeEvent(
              domain = Some(BillingDomain.AUTORU),
              campaignHeader = Some(callCarsUsedCampaign),
              isActive = Some(true),
              clientId = Some(balanceClient.balanceClientId),
              campaignId = Some("test-id")
            )
          )
          .success
          .value
      }
    }

  }

  private def setCustomGood(
      campaign: Model.CampaignHeader,
      customGoodId: String
  ): CampaignHeader =
    CampaignHeader.fromJavaProto(
      campaign.toBuilder
        .setProduct(
          Product
            .newBuilder()
            .setVersion(1)
            .addGoods(
              Good
                .newBuilder()
                .setVersion(1)
                .setCustom(
                  Good.Custom
                    .newBuilder()
                    .setId(customGoodId)
                    .setCost(Cost.newBuilder().setVersion(1))
                )
            )
        )
        .build()
    )

}
