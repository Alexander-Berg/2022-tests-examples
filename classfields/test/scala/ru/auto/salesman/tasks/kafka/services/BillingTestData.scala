package ru.auto.salesman.tasks.kafka.services

import ru.auto.salesman.model.{BalanceClientCore, ProductId}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.mockito.MockitoSupport

object BillingTestData extends MockitoSupport {

  val ClientId = 1
  val AgencyId = 2
  val BalanceClientId = 2
  val BalanceAgencyId = 3

  val AccountId = 7L
  val ClientPoiId = 88L

  val CampaignId: String = "campaignId"
  val ExistingCampaignId = CampaignId

  val BalanceClientCoreInfo =
    BalanceClientCore(
      ClientId,
      BalanceClientId,
      Some(BalanceAgencyId)
    )

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

  def campaignHeader(isEnabled: Boolean) = {

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
      .setIsEnabled(isEnabled)
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

}
