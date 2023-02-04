package ru.auto.salesman.api.v1.service.billing

import ru.auto.salesman.api.v1.RequiredHeaders
import ru.auto.salesman.billing.RequestContext
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.service.billingcampaign.BillingCampaignService
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.billing.Model

object BillingTestData extends BaseSpec with RequiredHeaders {

  val ClientId = 1
  val AgencyId = 1L
  val BalanceClientId = 2
  val BalanceAgencyId = 3

  val AccountId = 7L
  val ClientPoiId = 88L

  val CampaignId: String = "campaignId"
  val ExistingCampaignId = CampaignId

  val CustomerId = Model.CustomerId
    .newBuilder()
    .setVersion(1)
    .setClientId(BalanceClientId)
    .setAgencyId(BalanceAgencyId)
    .build()

  val Order = Model.Order.newBuilder
    .setVersion(1)
    .setOwner(CustomerId)
    .setId(AccountId)
    .setText("order")
    .setCommitAmount(5L)
    .setApproximateAmount(0L)
    .build

  val campaignHeader = {

    val ch = Model.CustomerHeader.newBuilder
      .setVersion(1)
      .setId(CustomerId)
      .build()

    val cost = Model.Cost
      .newBuilder()
      .setVersion(1)
      .setPerCall(Model.Cost.PerCall.newBuilder.setUnits(222L))
      .build()

    val custom = Model.Good.Custom.newBuilder
      .setId(ProductId.alias(ProductId.Call))
      .setCost(cost)
      .build()

    val good = Model.Good
      .newBuilder()
      .setVersion(1)
      .setCustom(custom)
      .build()

    val product = Model.Product.newBuilder
      .setVersion(1)
      .addGoods(good)
      .build()

    val settings = Model.CampaignSettings
      .newBuilder()
      .setVersion(1)
      .setIsEnabled(true)
      .build()

    Model.CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setOwner(ch)
      .setId(ExistingCampaignId)
      .setOrder(Order)
      .setProduct(product)
      .setSettings(settings)
      .build()
  }

  val billingCampaignService = mock[BillingCampaignService]

  implicit val rc = RequestContext(operatorContext.id)

}
