package ru.auto.salesman.tasks.kafka

import ru.auto.amoyak.InternalServiceModel.AmoSyncRequest
import ru.auto.cabinet.ApiModel.ClientIdsResponse
import ru.auto.cabinet.ApiModel.ClientIdsResponse.ClientInfo
import ru.auto.salesman.service.AmoyakSyncRequestsService
import ru.auto.salesman.client.cabinet.CabinetClient
import ru.auto.salesman.tasks.kafka.processors.impl.AmoyakBillingProcessorImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.data.CampaignStateChangeEventGen
import ru.yandex.vertis.billing.BillingEvent.CampaignStateChangeEvent
import ru.yandex.vertis.billing.Model.{Cost, Good}
import ru.yandex.vertis.billing.Model.Good.{Custom, PremiumPlacement}
import ru.yandex.vertis.billing.BillingEvent.CommonBillingInfo.BillingDomain
import ru.yandex.vertis.generators.ProducerProvider.asProducer

class AmoyakBillingProcessorSpec extends BaseSpec {

  val amoyakSyncRequestsService: AmoyakSyncRequestsService =
    mock[AmoyakSyncRequestsService]

  val cabinetClient: CabinetClient =
    mock[CabinetClient]

  val amoyakBillingProcessor = new AmoyakBillingProcessorImpl(
    amoyakSyncRequestsService,
    cabinetClient
  )

  private def setDomain(
      domain: BillingDomain,
      event: CampaignStateChangeEvent
  ) =
    event.toBuilder.setDomain(domain).build

  private def addGood(
      good: Good,
      event: CampaignStateChangeEvent
  ): CampaignStateChangeEvent = {
    val eventBuilder = event.toBuilder

    val campaignHeaderBuilder = eventBuilder.getCampaignHeader.toBuilder
    val productBuilder = campaignHeaderBuilder.getProduct.toBuilder

    eventBuilder
      .setCampaignHeader(
        campaignHeaderBuilder.setProduct(productBuilder.addGoods(good))
      )
      .build
  }

  private val defaultVersion = 1

  private val defaultCost =
    Cost.newBuilder.setVersion(defaultVersion)

  "AmoyakBillingProcessorImpl" when {
    "received CampaignStateChangeEvent" should {
      "do nothing when domain is incorrect" in {
        val noDomainEvent =
          CampaignStateChangeEventGen.apply.next.toBuilder.clearDomain.build

        val wrongDomainEvent =
          CampaignStateChangeEventGen.apply.next.toBuilder
            .setDomain(BillingDomain.AUTOPARTS)
            .build

        amoyakBillingProcessor
          .process(noDomainEvent)
          .success
          .value should be(())

        amoyakBillingProcessor
          .process(wrongDomainEvent)
          .success
          .value should be(())
      }

      "do nothing if it is not a call campaign event" in {
        val premiumPlacement =
          PremiumPlacement.newBuilder
            .setCost(defaultCost)

        val premiumGood =
          Good.newBuilder
            .setVersion(defaultVersion)
            .setPremiumPlacement(premiumPlacement)
            .build

        val campaignStateChangeEvent =
          CampaignStateChangeEventGen.apply
            .map(setDomain(BillingDomain.AUTORU, _))
            .map(addGood(premiumGood, _))
            .next

        amoyakBillingProcessor
          .process(campaignStateChangeEvent)
          .success
          .value should be(())
      }

      "create sync request" in {

        val callCustom =
          Custom.newBuilder
            .setCost(defaultCost)
            .setId("call")

        val callGood =
          Good.newBuilder
            .setVersion(defaultVersion)
            .setCustom(callCustom)
            .build

        val campaignStateChangeEvent =
          CampaignStateChangeEventGen.apply
            .map(setDomain(BillingDomain.AUTORU, _))
            .map(addGood(callGood, _))
            .next

        val clientId =
          campaignStateChangeEvent.getCampaignHeader.getOwner.getId.getClientId

        val clientIds = java.util.Arrays.asList(Long.box(clientId))

        val amoSyncRequest =
          AmoSyncRequest.newBuilder.addAllClientIds(clientIds).build

        (amoyakSyncRequestsService.create _)
          .expects(amoSyncRequest)
          .returningZ(())

        (cabinetClient.getClientIds _)
          .expects(*)
          .returningZ(
            ClientIdsResponse.newBuilder
              .addClientsInfo(
                ClientInfo.newBuilder.setClientId(clientId)
              )
              .build
          )

        amoyakBillingProcessor
          .process(campaignStateChangeEvent)
          .success
          .value should be(())
      }
    }
  }
}
