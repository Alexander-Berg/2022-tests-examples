package ru.auto.salesman.service.quota_offers

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.environment.{endOfToday, startOfToday}
import ru.auto.salesman.model.OfferStatuses.OfferStatus
import ru.auto.salesman.model.{
  AdsRequestType,
  AgencyId,
  CityId,
  Client,
  ClientId,
  ClientStatuses,
  PaymentGroup,
  ProductId,
  QuotaId,
  RegionId,
  StoredQuota,
  _
}
import ru.auto.salesman.test.model.gens.{ClientRecordGen, OfferGen}
import ru.yandex.vertis.billing.Model.OfferBilling
import ru.yandex.vertis.billing.Model.OfferBilling.KnownCampaign
import ru.yandex.vertis.billing.model.Versions
import ru.yandex.vertis.generators.ProducerProvider.asProducer

object QuotaOffersTestData {
  val testClientId = 123L
  val testAgencyId = Option.empty[AgencyId]
  val testQuotaSize = 5

  val testOfferBilling: OfferBilling = {
    val knownCampaignBuilder =
      KnownCampaign
        .newBuilder()
        .setActiveStart(startOfToday().getMillis)
        .setActiveDeadline(endOfToday().getMillis)
    OfferBilling
      .newBuilder()
      .setVersion(Versions.OFFER_BILLING)
      .setKnownCampaign(knownCampaignBuilder)
      .build()
  }

  val testQuotaId = QuotaId(1963611)

  val testEpoch = 0

  val testQuota = StoredQuota(
    id = testQuotaId,
    clientId = testClientId,
    quotaType = ProductId.QuotaPlacementCarsNew,
    size = testQuotaSize,
    revenue = 1000L,
    price = 1000L,
    from = startOfToday(),
    to = endOfToday(),
    offerBilling = testOfferBilling,
    regionId = None,
    epoch = testEpoch
  )

  val testClient =
    Client(
      clientId = testClientId,
      agencyId = testAgencyId,
      categorizedClientId = None,
      companyId = None,
      regionId = RegionId(100L),
      cityId = CityId(1123L),
      status = ClientStatuses.Active,
      singlePayment = Set.empty[AdsRequestType],
      firstModerated = false,
      paidCallsAvailable = false,
      priorityPlacement = true
    )

  val testClientRecord =
    ClientRecordGen.next.copy(clientId = testClientId)

  val testPaymentGroup = PaymentGroup(Category.CARS, Set(Section.NEW))

  private def genOfferStatus(): OfferStatus =
    Gen.oneOf(actualizedOfferStatuses.toList).next

  def genOffers(
      size: Int,
      clientId: ClientId = testClientId,
      paymentGroup: PaymentGroup = testPaymentGroup
  ) =
    Gen
      .listOfN(size, OfferGen)
      .next
      .map(
        _.copy(
          categoryId = paymentGroup.category.flat,
          sectionId = Gen.oneOf(paymentGroup.section.toList).next,
          clientId = clientId,
          status = genOfferStatus()
        )
      )
}
