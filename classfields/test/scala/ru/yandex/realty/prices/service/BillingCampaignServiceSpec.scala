package ru.yandex.realty.prices.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.billing.{BillingCampaignStorage, BillingCampaignStorageImpl}
import ru.yandex.realty.clients.balance.{BalanceClient, User}
import ru.yandex.realty.clients.billing.{
  Balance,
  BillingClient,
  Campaign,
  Goods,
  OrderProperties,
  Cost => BillingCost,
  Order => BillingOrder,
  Product => BillingProduct
}
import ru.yandex.realty.sites.campaign.CampaignParser
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.sites.{BidService, SiteCompanyBid, SitesGroupingService}
import ru.yandex.realty.time.LocalDateTimeUtils.toMillis
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.billing.Model.CampaignSettings.CallSettings
import ru.yandex.vertis.billing.Model.{
  CampaignHeader,
  CampaignSettings,
  Cost,
  CustomerHeader,
  CustomerId,
  Good,
  OfferBilling,
  OfferId,
  Order,
  Phone,
  Product
}
import ru.yandex.vertis.billing.Model.OfferBilling.KnownCampaign
import ru.yandex.vertis.billing.Model.OfferId.ServiceObject
import ru.yandex.vertis.billing.Model.OfferId.ServiceObject.ServiceObjectKind
import ru.yandex.vertis.billing.Model.Good.{Custom, Placement}
import ru.yandex.realty.util.Mappings._
import ru.yandex.vertis.billing.Model.Cost.PerCall

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class BillingCampaignServiceSpec extends AsyncSpecBase {

  private val superCallFactor = 1.2

  private val placementCampaignId = "c90ab80a-e97a-4079-96ba-09daa35400"
  private val placementClientId = 12345L
  private val placementAgencyId = None
  private val placementAgencyOrClientId = placementAgencyId.getOrElse(placementClientId)
  private val placementSiteId = 5764L
  private val placementCompanyId = 7834L
  private val placementCallPrice = 500000L
  private val placementUpdateTime = toMillis(LocalDateTime.now().minusDays(1))
  private val placementBalanceUser = User(123424124, "placement-client")
  private val placementCampaign = buildBillingClientCampaign(placementCampaignId, placementCallPrice, false)

  private val superCallCampaignId = "a907780a-e97s-4081-fcba-09da9a5401"
  private val superCallClientId = 12346L
  private val superCallAgencyId = Some(23456L)
  private val superCallAgencyOrClientId = superCallAgencyId.getOrElse(superCallClientId)
  private val superCallSiteId = 5765L
  private val superCallCompanyId = 7835L
  private val superCallCallPrice = 1500000L
  private val superCallUpdateTime = toMillis(LocalDateTime.now().minusDays(2))
  private val superCallFrom = LocalDateTime.now().minusDays(30)
  private val superCallDuration = superCallFrom.plusDays(60)
  private val superCallBalanceUser = User(2234245, "super-call-client")
  private val superCallCampaign = buildBillingClientCampaign(superCallCampaignId, superCallCallPrice, true)

  private val campaigns = buildCampaigns()

  private val billingCampaignProvider = mock[Provider[BillingCampaignStorage]]
  private val sitesService = mock[SitesGroupingService]
  private val bidService = mock[BidService]
  private val parser = new CampaignParser(sitesService, bidService)
  private val balanceClient = mock[BalanceClient]
  private val billingClient = mock[BillingClient]

  private val billingCampaignService =
    new BillingCampaignService(billingCampaignProvider, parser, balanceClient, billingClient, bidService)

  implicit private val traced: Traced = Traced.empty

  "BillingCampaignService in updateProduct" should {

    "don't update campaign if there is no matched campaign for siteId and companyId" in {
      val fakeSiteId = 1L
      val fakeCompanyId = 1L
      mockCampaigns()
      toMockFunction1(bidService.getSiteCompanyBids(_: java.util.Map[java.lang.Long, java.util.List[java.lang.Long]]))
        .expects(where { siteId2CompanyIds: java.util.Map[java.lang.Long, java.util.List[java.lang.Long]] =>
          siteId2CompanyIds.isEmpty
        })
        .returning(Seq.empty.asJava)
      billingCampaignService
        .updateProduct(fakeSiteId, fakeCompanyId)
        .futureValue
    }

    "don't update campaign if there is no balance user for matched campaign" in {
      mockCampaigns()
      val actualMinimalPrice = placementCallPrice + 100000
      val actualSuperCallMinimalPrice = (actualMinimalPrice * superCallFactor).toLong
      val siteCompanyBid =
        buildSiteCompanyBid(placementSiteId, placementCompanyId, actualMinimalPrice, actualSuperCallMinimalPrice)
      mockCampaignData(placementSiteId, placementCompanyId, siteCompanyBid)
      mockGetBalanceUsers(placementAgencyOrClientId, Seq.empty)
      billingCampaignService.updateProduct(placementSiteId, placementCompanyId).futureValue
    }

    "don't update superCall campaign if current price >= actual super call minimal price" in {
      mockCampaigns()
      val actualSuperCallMinimalPrice = superCallCallPrice - 100000
      val actualMinimalPrice = (actualSuperCallMinimalPrice / superCallFactor).toLong
      val siteCompanyBid =
        buildSiteCompanyBid(superCallSiteId, superCallCompanyId, actualMinimalPrice, actualSuperCallMinimalPrice)
      mockCampaignData(superCallSiteId, superCallCompanyId, siteCompanyBid)
      mockGetBalanceUsers(superCallAgencyOrClientId, Seq(superCallBalanceUser))
      mockGetBillingCampaign(
        superCallBalanceUser.uid,
        superCallClientId,
        superCallAgencyId,
        superCallCampaignId,
        superCallCampaign
      )
      billingCampaignService.updateProduct(superCallSiteId, superCallCompanyId).futureValue
    }

    "don't update placement campaign if current price >= actual minimal price" in {
      mockCampaigns()
      val actualMinimalPrice = placementCallPrice - 100000
      val actualSuperCallMinimalPrice = (actualMinimalPrice * superCallFactor).toLong
      val siteCompanyBid =
        buildSiteCompanyBid(placementSiteId, placementCompanyId, actualMinimalPrice, actualSuperCallMinimalPrice)
      mockCampaignData(placementSiteId, placementCompanyId, siteCompanyBid)
      mockGetBalanceUsers(placementAgencyOrClientId, Seq(placementBalanceUser))
      mockGetBillingCampaign(
        placementBalanceUser.uid,
        placementClientId,
        placementAgencyId,
        placementCampaignId,
        placementCampaign
      )
      billingCampaignService.updateProduct(placementSiteId, placementCompanyId).futureValue
    }

    "update superCall campaign if current price < actual super call minimal price" in {
      mockCampaigns()
      val actualSuperCallMinimalPrice = superCallCallPrice + 100000
      val actualMinimalPrice = (actualSuperCallMinimalPrice / superCallFactor).toLong
      val siteCompanyBid =
        buildSiteCompanyBid(superCallSiteId, superCallCompanyId, actualMinimalPrice, actualSuperCallMinimalPrice)
      mockCampaignData(superCallSiteId, superCallCompanyId, siteCompanyBid)
      mockGetBalanceUsers(superCallAgencyOrClientId, Seq(superCallBalanceUser))
      mockGetBillingCampaign(
        superCallBalanceUser.uid,
        superCallClientId,
        superCallAgencyId,
        superCallCampaignId,
        superCallCampaign
      )
      mockUpdateCampaignProduct(
        superCallBalanceUser.uid,
        superCallClientId,
        superCallAgencyId,
        superCallCampaignId,
        actualSuperCallMinimalPrice,
        true
      )
      billingCampaignService.updateProduct(superCallSiteId, superCallCompanyId).futureValue
    }

    "update placement campaign if current price < actual minimal price" in {
      mockCampaigns()
      val actualMinimalPrice = placementCallPrice + 100000
      val actualSuperCallMinimalPrice = (actualMinimalPrice * superCallFactor).toLong
      val siteCompanyBid =
        buildSiteCompanyBid(placementSiteId, placementCompanyId, actualMinimalPrice, actualSuperCallMinimalPrice)
      mockCampaignData(placementSiteId, placementCompanyId, siteCompanyBid)
      mockGetBalanceUsers(placementAgencyOrClientId, Seq(placementBalanceUser))
      mockGetBillingCampaign(
        placementBalanceUser.uid,
        placementClientId,
        placementAgencyId,
        placementCampaignId,
        placementCampaign
      )
      mockUpdateCampaignProduct(
        placementBalanceUser.uid,
        placementClientId,
        placementAgencyId,
        placementCampaignId,
        actualMinimalPrice,
        false
      )
      billingCampaignService.updateProduct(placementSiteId, placementCompanyId).futureValue
    }
  }

  private def buildCampaigns(): Map[String, OfferBilling] = {
    val placementCampaign = buildCampaign(
      placementCampaignId,
      placementClientId,
      placementAgencyId,
      placementSiteId,
      placementCompanyId,
      placementCallPrice,
      placementUpdateTime,
      false,
      None,
      None
    )
    val superCallCampaign = buildCampaign(
      superCallCampaignId,
      superCallClientId,
      superCallAgencyId,
      superCallSiteId,
      superCallCompanyId,
      superCallCallPrice,
      superCallUpdateTime,
      true,
      Some(toMillis(superCallFrom)),
      Some(toMillis(superCallDuration) - toMillis(superCallFrom))
    )
    Map(placementCampaignId -> placementCampaign, superCallCampaignId -> superCallCampaign)
  }

  // scalastyle:off
  private def buildCampaign(
    campaignId: String,
    clientId: Long,
    agencyId: Option[Long],
    siteId: Long,
    companyId: Long,
    callPrice: Long,
    campaignUpdateTime: Long,
    superCall: Boolean,
    superCallFrom: Option[Long],
    superCallDuration: Option[Long]
  ): OfferBilling = {
    val version = 1
    val b = OfferBilling
      .newBuilder()
      .setVersion(version)

    b.setKnownCampaign(
      KnownCampaign
        .newBuilder()
        .setIsActive(true)
        .setUpdateTime(campaignUpdateTime)
        .setCampaign(
          CampaignHeader
            .newBuilder()
            .setVersion(version)
            .setId(campaignId)
            .setSettings(
              CampaignSettings
                .newBuilder()
                .setVersion(version)
                .setIsEnabled(true)
                .setCallSettings(
                  CallSettings
                    .newBuilder()
                    .setPhone(Phone.newBuilder().setCountry("+7").setCode("950").setPhone("3335566"))
                )
            )
            .setOwner(
              CustomerHeader
                .newBuilder()
                .setVersion(version)
                .setId(
                  CustomerId
                    .newBuilder()
                    .setVersion(version)
                    .setClientId(clientId)
                    .applySideEffectIf(agencyId.isDefined, _.setAgencyId(agencyId.get))
                )
            )
            .setOrder(
              Order
                .newBuilder()
                .setVersion(version)
                .setId(123)
                .setActText("text")
                .setCommitAmount(100000)
                .setApproximateAmount(100000)
                .setOwner(
                  CustomerId
                    .newBuilder()
                    .setVersion(version)
                    .setClientId(clientId)
                    .applySideEffectIf(agencyId.isDefined, _.setAgencyId(agencyId.get))
                )
                .setText("text")
            )
            .setProduct(
              Product
                .newBuilder()
                .setVersion(version)
                .addGoods(buildGood(callPrice, superCall, version))
                .applySideEffectIf(superCallDuration.isDefined, _.setDuration(superCallDuration.get))
                .applySideEffectIf(superCallFrom.isDefined, _.setFrom(superCallFrom.get))
            )
        )
    )

    b.setOfferId(
      OfferId
        .newBuilder()
        .setVersion(version)
        .setServiceObject(
          ServiceObject
            .newBuilder()
            .setKind(ServiceObjectKind.NEW_BUILDING)
            .setId(siteId.toString)
            .setPartnerId(companyId.toString)
        )
    )
    b.build()
  }

  private def buildGood(callPrice: Long, superCall: Boolean, version: Int): Good = {
    if (superCall) buildSuperCallGood(callPrice, version)
    else buildPlacementGood(callPrice, version)
  }

  private def buildPlacementGood(callPrice: Long, version: Int): Good = {
    Good
      .newBuilder()
      .setVersion(version)
      .setPlacement(Placement.newBuilder().setCost(buildCost(callPrice, version)))
      .build()
  }

  private def buildSuperCallGood(callPrice: Long, version: Int): Good = {
    Good
      .newBuilder()
      .setVersion(version)
      .setCustom(Custom.newBuilder().setId("superCall").setCost(buildCost(callPrice, version)))
      .build()
  }

  private def buildCost(callPrice: Long, version: Int): Cost = {
    Cost
      .newBuilder()
      .setVersion(version)
      .setPerCall(PerCall.newBuilder().setUnits(callPrice))
      .build()
  }

  private def buildBillingClientCampaign(campaignId: String, callPrice: Long, superCall: Boolean): Campaign = {
    val cost = BillingCost(costPerCall = Some(callPrice))
    val goods = if (superCall) Goods(cost = Some(cost), id = Some("superCall")) else Goods(placement = Some(cost))
    Campaign(
      id = campaignId,
      order = buildDummyOrder(),
      product = Some(BillingProduct(Seq(goods), None))
    )
  }

  private def mockCampaigns(): Unit = {
    (billingCampaignProvider.get _).expects().once().returning(BillingCampaignStorageImpl(campaigns))
  }

  private def mockGetBalanceUsers(clientId: Long, users: Seq[User]): Unit = {
    toMockFunction2(balanceClient.getUsersByClientId(_: Long)(_: Traced))
      .expects(clientId, *)
      .returning(Future.successful(users))
  }

  private def mockGetBillingCampaign(
    uid: Long,
    clientId: Long,
    agencyId: Option[Long],
    campaignId: String,
    campaign: Campaign
  ): Unit = {
    toMockFunction5(billingClient.getCampaign(_: String, _: Int, _: Option[Int], _: String)(_: Traced))
      .expects(uid.toString, clientId.toInt, agencyId.map(_.toInt), campaignId, *)
      .once
      .returning(Future.successful(campaign))
  }

  private def mockUpdateCampaignProduct(
    uid: Long,
    clientId: Long,
    agencyId: Option[Long],
    campaignId: String,
    callPrice: Long,
    superCall: Boolean
  ): Unit = {

    def checkCallPrice(product: BillingProduct, price: Long): Boolean = {
      if (superCall) product.goods.head.cost.get.costPerCall.get == price
      else product.goods.head.placement.get.costPerCall.get == price
    }

    val dummyCampaign = Campaign(id = campaignId, order = buildDummyOrder())
    toMockFunction6(
      billingClient.updateCampaignProduct(_: String, _: Long, _: Option[Long], _: String, _: BillingProduct)(_: Traced)
    ).expects(where { (pUid, pClientId, pAgencyId, pCampaignId, product, *) =>
        pUid == uid.toString &&
        pClientId == clientId &&
        pAgencyId == agencyId &&
        pCampaignId == campaignId &&
        checkCallPrice(product, callPrice)
      })
      .once()
      .returning(Future.successful(dummyCampaign))
  }

  private def buildSiteCompanyBid(
    siteId: Long,
    companyId: Long,
    actualMinimalPrice: Long,
    actualSuperCallMinimalPrice: Long
  ): SiteCompanyBid =
    SiteCompanyBid(
      siteId = siteId,
      companyId = companyId,
      actualMinimalPrice = actualMinimalPrice,
      nextMinimalPrice = None,
      actualSuperCallMinimalPrice = actualSuperCallMinimalPrice,
      nextSuperCallMinimalPrice = None,
      actualPricePerM2 = None,
      nextPricePerM2 = None
    )

  private def mockCampaignData(
    siteId: Long,
    companyId: Long,
    siteCompanyBid: SiteCompanyBid
  ): Unit = {
    (sitesService.getSiteById _).expects(siteId).once().returning(new Site(siteId))
    toMockFunction1(bidService.getSiteCompanyBids(_: java.util.Map[java.lang.Long, java.util.List[java.lang.Long]]))
      .expects(where { siteId2CompanyIds: java.util.Map[java.lang.Long, java.util.List[java.lang.Long]] =>
        siteId2CompanyIds.keySet().size == 1 &&
        siteId2CompanyIds.keySet().contains(siteId) &&
        siteId2CompanyIds.get(siteId).size() == 1 &&
        siteId2CompanyIds.get(siteId).contains(companyId)
      })
      .once()
      .returning(Seq(siteCompanyBid).asJava)

    toMockFunction3(bidService.getSiteCompanyBidAsync(_: Long, _: Long)(_: Traced))
      .expects(siteId, companyId, *)
      .returning(Future.successful(siteCompanyBid))
  }

  private def buildDummyOrder(): BillingOrder = {
    BillingOrder(1, 1, None, OrderProperties(None, None, None), Balance(1, 1, 1, 1))
  }

}
