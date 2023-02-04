package ru.auto.salesman.service.tskv

import org.joda.time.DateTime
import ru.auto.salesman.service.BillingEvent
import ru.auto.salesman.service.billingcampaign.BillingTestData
import ru.yandex.vertis.billing.Model.Good.Custom
import ru.yandex.vertis.billing.Model.OfferBilling.KnownCampaign
import ru.yandex.vertis.billing.Model.{CampaignHeader, Cost, Good, OfferBilling, Product}
import ru.yandex.vertis.billing.model.Versions

package object billing {

  val product =
    Product
      .newBuilder()
      .setVersion(1)
      .addGoods(
        Good
          .newBuilder()
          .setVersion(1)
          .setCustom(
            Custom
              .newBuilder()
              .setId("id")
              .setCost(Cost.newBuilder().setVersion(1))
          )
      )

  val header =
    CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setId("1")
      .setOrder(BillingTestData.Order)
      .setSettings(BillingTestData.Settings)
      .setOwner(BillingTestData.Owner)
      .setProduct(product)
      .build()

  def knownCampaignBuilder(now: DateTime) =
    KnownCampaign
      .newBuilder()
      .setIsActive(true)
      .setActiveStart(now.getMillis)
      .setActiveDeadline(now.plusDays(3).getMillis)
      .setHold("1")
      .setCampaign(header)
      .build()

  def offerBilling(now: DateTime) =
    OfferBilling
      .newBuilder()
      .setTimestamp(now.getMillis)
      .setVersion(Versions.OFFER_BILLING)
      .setKnownCampaign(knownCampaignBuilder(now))
      .build()

  def billingEvent(now: DateTime) = BillingEvent(offerBilling(now))

}
