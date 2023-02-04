package ru.auto.salesman.service.billingcampaign

import ru.auto.salesman.billing.BootstrapClient
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.billing.BillingCampaignClient
import ru.auto.salesman.client.moisha.NewMoishaClient
import ru.auto.salesman.dao.{ClientDao, PriorityPlacementPeriodDao}
import ru.auto.salesman.model.payment_model.PaymentModelChecker
import ru.auto.salesman.model.{
  AdsRequestType,
  CityId,
  Client,
  ClientStatuses,
  DetailedClient,
  ProductId,
  RegionId
}
import ru.auto.salesman.service._
import ru.auto.salesman.service.call.price.CallPriceService
import ru.auto.salesman.service.client.{ClientService, ClientServiceImpl}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.OperatorContext
import ru.yandex.vertis.billing.Model

object BillingTestData extends BaseSpec {

  val ClientId = 1
  val AgencyId = 1L
  val BalanceClientId = 2
  val BalanceAgencyId = 3

  val AccountId = 7L
  val ClientPoiId = 88L

  val CampaignId: String = "campaignId"
  val ExistingCampaignId = CampaignId

  val client = Client(
    ClientId,
    Some(AgencyId),
    None,
    None,
    RegionId(100L),
    CityId(1123L),
    ClientStatuses.Active,
    Set(),
    firstModerated = true,
    paidCallsAvailable = true,
    priorityPlacement = true
  )

  val ClientDetails =
    DetailedClient(
      ClientId,
      Some(AgencyId),
      BalanceClientId,
      Some(BalanceAgencyId),
      None,
      None,
      RegionId(100L),
      CityId(1123L),
      AccountId,
      isActive = true,
      firstModerated = false,
      singlePayment = Set.empty[AdsRequestType],
      paidCallsAvailable = true
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

  val Owner = Model.CustomerHeader.newBuilder
    .setVersion(1)
    .setId(CustomerId)
    .build()

  val Settings = Model.CampaignSettings
    .newBuilder()
    .setVersion(1)
    .setIsEnabled(true)
    .build()

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

  val moishaClient = mock[NewMoishaClient]

  val vosClient = mock[VosClient]

  val billingService = mock[BillingService]

  val billingBootstrapClient = mock[BootstrapClient]

  val billingCampaignClient = mock[BillingCampaignClient]

  val clientDao: ClientDao = mock[ClientDao]

  val clientService: ClientService = new ClientServiceImpl(clientDao)

  val detailedClientSource: DetailedClientSource = mock[DetailedClientSource]

  val priorityPlacementPeriodDao: PriorityPlacementPeriodDao =
    mock[PriorityPlacementPeriodDao]

  val priceRequestCreator: PriceRequestCreator =
    mock[PriceRequestCreator]

  val priceServiceChooserTest = mock[CallPriceService]

  val paymentModelChecker = mock[PaymentModelChecker]

  val operatorUid = "10"
  val operatorContext = OperatorContext("foo", operatorUid)
}
