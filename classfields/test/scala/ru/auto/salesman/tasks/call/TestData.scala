package ru.auto.salesman.tasks.call

import ru.auto.salesman.dao.BalanceClientDao.BaseRecord
import ru.auto.salesman.model._
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.mockito.MockitoSupport

object TestData extends MockitoSupport {

  val ClientId = 1L
  val AgencyId = Option.empty[AgencyId]
  val BalanceClientId = 2
  val BalanceAgencyId = 3

  val AccountId = 7L
  val ClientPoiId = 88L

  val CampaignId: String = "campaignId"
  val ExistingCampaignId = CampaignId

  val paidCallsAvailable = true

  val clientBaseRecord = BaseRecord(
    ClientId,
    BalanceClientId,
    Some(BalanceAgencyId),
    paidCallsAvailable
  )

  val clientRecordActive = Client(
    ClientId,
    AgencyId,
    None,
    None,
    RegionId(1),
    CityId(1),
    ClientStatuses.Active,
    Set.empty,
    firstModerated = true,
    None,
    paidCallsAvailable,
    priorityPlacement = true
  )

  val clientRecordStopped = Client(
    ClientId,
    AgencyId,
    None,
    None,
    RegionId(1),
    CityId(1),
    ClientStatuses.Stopped,
    Set.empty,
    firstModerated = true,
    None,
    paidCallsAvailable,
    priorityPlacement = true
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

  def campaignHeader = {

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
  }
}
