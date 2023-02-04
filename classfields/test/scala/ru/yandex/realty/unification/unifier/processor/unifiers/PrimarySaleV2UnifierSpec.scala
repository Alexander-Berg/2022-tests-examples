package ru.yandex.realty.unification.unifier.processor.unifiers

import akka.http.scaladsl.model.StatusCode
import com.google.protobuf.util.Timestamps
import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.billing.{BillingCampaignStorage, BillingCampaignStorageImpl}
import ru.yandex.realty.clients.feedprocessor.FeedProcessorClient
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.errors.ProtoErrorResponseException
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.feedprocessor.FeedUpdateTime
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.model.billing.RichOfferBilling
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer._
import ru.yandex.realty.model.raw.RawOffer
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.model.sites._
import ru.yandex.realty.proto.api.error.{Error, ErrorCode}
import ru.yandex.realty.regions.NewTargetCallRegionsStorage
import ru.yandex.realty.sites.{CompaniesStorage, SitePresenceStorage, SitesGroupingService}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.unification.unifier.processor.services.PrimarySaleCampaignStatusResolver
import ru.yandex.vertis.billing.Model.OfferBilling.KnownCampaign
import ru.yandex.vertis.billing.Model._

import java.util.{Date, UUID}
import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PrimarySaleV2UnifierSpec
  extends AsyncSpecBase
  with OneInstancePerTest
  with FeaturesStubComponent
  with PropertyChecks {

  private val offer: Offer = mock[Offer]
  private val rawOffer: RawOffer = mock[RawOffer]

  implicit val trace: Traced = Traced.empty

  private val rgProvider: Provider[RegionGraph] = mock[Provider[RegionGraph]]
  private val sitesService: SitesGroupingService = mock[SitesGroupingService]
  private val sitePresenceProvider: Provider[SitePresenceStorage] = mock[Provider[SitePresenceStorage]]
  private val companiesProvider: Provider[CompaniesStorage] = mock[Provider[CompaniesStorage]]
  private val newTargetCallRegionsProvider: Provider[NewTargetCallRegionsStorage] =
    ProviderAdapter.create(NewTargetCallRegionsStorage(Set.empty))
  private val billingCampaignProvider: Provider[BillingCampaignStorage] = mock[Provider[BillingCampaignStorage]]
  private val primarySaleCampaignStatusResolver: PrimarySaleCampaignStatusResolver =
    new PrimarySaleCampaignStatusResolver(billingCampaignProvider)

  private val feedProcessorClient: FeedProcessorClient = mock[FeedProcessorClient]
  private val psUnifier =
    new PrimarySaleV2Unifier(
      rgProvider,
      sitesService,
      sitePresenceProvider,
      newTargetCallRegionsProvider,
      primarySaleCampaignStatusResolver,
      feedProcessorClient,
      features
    )

  private val rgid: java.lang.Long = Long.box(42L)
  private val node = new Node
  node.setGeoId(rgid.toInt)
  node.setId(rgid)

  private val regionGraphMock = mock[RegionGraph]
  (regionGraphMock
    .getNodeById(_: java.lang.Long))
    .expects(rgid)
    .anyNumberOfTimes()
    .returning(node)

  (rgProvider.get _)
    .expects()
    .anyNumberOfTimes()
    .returning(regionGraphMock)

  private val sitePresenceStorage = new SitePresenceStorage(Map.empty[java.lang.Long, SitePresence].asJava) {
    override def existsInSonOfCountry(node: Node, regionGraph: RegionGraph): Boolean = true
  }

  (sitePresenceProvider.get _).expects().anyNumberOfTimes().returning(sitePresenceStorage)

  (billingCampaignProvider.get _).expects().anyNumberOfTimes().returning(BillingCampaignStorageImpl(Map.empty))

  private val location: Location = new Location()
  location.setRegionGraphId(node.getId)

  "PrimarySaleV2Unifier" should {
    "in the absence of a siteId, conclude to primarySale=false" in {
      (offer.getId _).expects().anyNumberOfTimes().returning("23780")
      (offer.isFromFeed _).expects().anyNumberOfTimes().returning(true)
      (offer.getBuildingInfo _).expects().anyNumberOfTimes().returning(new BuildingInfo())
      (offer.getCategoryType _).expects().anyNumberOfTimes().returning(CategoryType.APARTMENT)
      (offer.getOfferType _).expects().anyNumberOfTimes().returning(OfferType.SELL)
      (offer.getPartnerId _).expects().anyNumberOfTimes().returning(-1L)
      (offer.getLocation _).expects().anyNumberOfTimes().returning(location)
      (offer.getApartmentInfo _).expects().anyNumberOfTimes().returning(new ApartmentInfo)

      val transaction: Transaction = new Transaction()
      transaction.setDealStatus(DealStatus.SALE)

      (offer.getTransaction _).expects().anyNumberOfTimes().returning(transaction)

      (offer.setPrimarySaleV2 _).expects(java.lang.Boolean.FALSE)
      (offer.isPrimarySaleV2 _).expects().anyNumberOfTimes().returning(false)
      psUnifier.unify(new OfferWrapper(rawOffer, offer, OfferHistory.justArrived())).futureValue
    }

    "for ZAD partner, conclude to primarySale=true" in {
      (offer.getId _).expects().anyNumberOfTimes().returning("6789")
      (offer.isFromFeed _).expects().anyNumberOfTimes().returning(true)

      val partnerId = 12678L
      val siteId: Long = 12356L
      val zadPriority = 1

      val buildingInfo = new BuildingInfo()
      buildingInfo.setSiteId(siteId)

      val site = new Site(siteId)
      site.setSaleStatus(SaleStatus.ON_SALE)
      site.setPhases(List.empty.asJava)
      site.setPartnerSellers(Seq(new PartnerSeller(partnerId, null, zadPriority)).asJava)
      (sitesService.getSiteById _).expects(siteId).anyNumberOfTimes().returning(site)

      (offer.getSaleAgent _).expects().anyNumberOfTimes().returning(null)
      (offer.getBuildingInfo _).expects().anyNumberOfTimes().returning(buildingInfo)
      (offer.getCategoryType _).expects().anyNumberOfTimes().returning(CategoryType.APARTMENT)
      (offer.getOfferType _).expects().anyNumberOfTimes().returning(OfferType.SELL)
      (offer.getPartnerId _).expects().anyNumberOfTimes().returning(partnerId)
      (offer.getLocation _).expects().anyNumberOfTimes().returning(location)
      (offer.getCompanyIds _).expects().anyNumberOfTimes().returning(List.empty.asJava)
      toMockFunction4(
        offer
          .addError(_: IndexingError, _: ErrorSource, _: Class[_], _: String)
      ).expects(IndexingError.SITE_OFFER_BASE_FIELD_NOT_SET, *, *, *)
        .anyNumberOfTimes()
        .returning()

      val transaction: Transaction = new Transaction()
      transaction.setDealStatus(DealStatus.PRIMARY_SALE)

      (offer.getTransaction _).expects().anyNumberOfTimes().returning(transaction)

      val apartmentInfo = new ApartmentInfo()
      apartmentInfo.setFlatType(FlatType.NEW_FLAT)
      (offer.getApartmentInfo _).expects().anyNumberOfTimes().returning(apartmentInfo)

      (offer.getOfferState _).expects().anyNumberOfTimes().returning(new OfferState())
      (offer.getArea _).expects().anyNumberOfTimes().returning(AreaInfo.create(AreaUnit.SQUARE_METER, 2f))

      apartmentInfo.setFloors(Seq(Int.box(3)).asJava)
      buildingInfo.setFloorsTotal(4)

      (offer.setPrimarySaleV2 _).expects(java.lang.Boolean.TRUE)
      (offer.setZadPriority _).expects(Int.box(zadPriority))
      (offer.isPrimarySaleV2 _).expects().anyNumberOfTimes().returning(true)
      psUnifier.unify(new OfferWrapper(rawOffer, offer, OfferHistory.justArrived())).futureValue
    }

    "for reassignment, conclude to primarySale=false" in {
      (offer.getId _).expects().anyNumberOfTimes().returning("56789")
      (offer.isFromFeed _).expects().anyNumberOfTimes().returning(false)

      val partnerId = 3456L
      val siteId: Long = 57861L
      val zadPriority = 2

      val buildingInfo = new BuildingInfo()
      buildingInfo.setSiteId(siteId)

      val site = new Site(siteId)
      site.setSaleStatus(SaleStatus.ON_SALE)
      site.setPhases(List.empty.asJava)
      site.setPartnerSellers(Seq(new PartnerSeller(partnerId, null, zadPriority)).asJava)
      (sitesService.getSiteById _).expects(siteId).anyNumberOfTimes().returning(site)

      (offer.getSaleAgent _).expects().anyNumberOfTimes().returning(null)
      (offer.getBuildingInfo _).expects().anyNumberOfTimes().returning(buildingInfo)
      (offer.getCategoryType _).expects().anyNumberOfTimes().returning(CategoryType.APARTMENT)
      (offer.getOfferType _).expects().anyNumberOfTimes().returning(OfferType.SELL)
      (offer.getPartnerId _).expects().anyNumberOfTimes().returning(partnerId + 1)
      (offer.getLocation _).expects().anyNumberOfTimes().returning(location)
      (offer.getCompanyIds _).expects().anyNumberOfTimes().returning(List.empty.asJava)
      toMockFunction4(
        offer
          .addError(_: IndexingError, _: ErrorSource, _: Class[_], _: String)
      ).expects(IndexingError.SITE_OFFER_BASE_FIELD_NOT_SET, *, *, *)
        .anyNumberOfTimes()
        .returning()

      val transaction: Transaction = new Transaction()
      transaction.setDealStatus(DealStatus.REASSIGNMENT)

      (offer.getTransaction _).expects().anyNumberOfTimes().returning(transaction)

      val apartmentInfo = new ApartmentInfo()
      apartmentInfo.setFlatType(FlatType.NEW_FLAT)
      (offer.getApartmentInfo _).expects().anyNumberOfTimes().returning(apartmentInfo)

      (offer.getOfferState _).expects().anyNumberOfTimes().returning(new OfferState())

      (offer.setPrimarySaleV2 _).expects(java.lang.Boolean.FALSE)
      (offer.isPrimarySaleV2 _).expects().anyNumberOfTimes().returning(false)
      psUnifier.unify(new OfferWrapper(rawOffer, offer, OfferHistory.justArrived())).futureValue
    }

    "for non-authorized primary sale, conclude to a ban" in {
      val partnerId = 9356L
      val siteId: Long = 8356L
      val zadPriority = 1

      val buildingInfo = new BuildingInfo()
      buildingInfo.setSiteId(siteId)

      val site = new Site(siteId)
      site.setSaleStatus(SaleStatus.ON_SALE)
      site.setPhases(List.empty.asJava)
      site.setPartnerSellers(Seq(new PartnerSeller(partnerId, null, zadPriority)).asJava)
      (sitesService.getSiteById _).expects(siteId).anyNumberOfTimes().returning(site)

      val offer: Offer = new Offer()
      offer.setId(215078197L)
      offer.setBuildingInfo(buildingInfo)
      offer.setCategoryType(CategoryType.APARTMENT)
      offer.setOfferType(OfferType.SELL)
      offer.setPartnerId(partnerId + 1)
      offer.setLocation(location)
      offer.setCompanyIds(List.empty.asJava)

      val transaction: Transaction = new Transaction()
      transaction.setDealStatus(DealStatus.PRIMARY_SALE)
      offer.setTransaction(transaction)

      val apartmentInfo = new ApartmentInfo()
      apartmentInfo.setFlatType(FlatType.NEW_FLAT)
      offer.setApartmentInfo(apartmentInfo)

      psUnifier.unify(new OfferWrapper(rawOffer, offer, OfferHistory.justArrived())).futureValue
      offer.getOfferState.getErrors.asScala
        .exists(oe => oe.getError == IndexingError.INVALID_PRIMARY_SALE) shouldBe true
    }

    "for campaign-enabled partner, conclude to primarySale=true" in {
      (offer.getId _).expects().anyNumberOfTimes().returning("6789")
      (offer.isFromFeed _).expects().anyNumberOfTimes().returning(true)

      val partnerId = 12678L
      val siteId: Long = 12356L
      val companyId: Long = 23412L
      val zadPriority = 1

      val campaign = generateOfferBilling(siteId, companyId)

      val campaigns = Seq(campaign)
      (billingCampaignProvider.get _)
        .expects()
        .anyNumberOfTimes()
        .returning(
          BillingCampaignStorageImpl(
            campaigns
              .map(campaign => campaign.campaignId -> campaign)
              .toMap
          )
        )

      val company = new Company(companyId)
      company.setBuildings(List(Long.box(siteId)).asJava)
      val companies = Seq(company).asJavaCollection
      (companiesProvider.get _)
        .expects()
        .anyNumberOfTimes()
        .returning(new CompaniesStorage(companies))

      val buildingInfo = new BuildingInfo()
      buildingInfo.setSiteId(siteId)

      val site = new Site(siteId)
      site.setSaleStatus(SaleStatus.ON_SALE)
      site.setPhases(List.empty.asJava)
      site.setPartnerSellers(Seq(new PartnerSeller(partnerId, null, 1)).asJava)
      (sitesService.getSiteById _).expects(siteId).anyNumberOfTimes().returning(site)

      (offer.getSaleAgent _).expects().anyNumberOfTimes().returning(null)
      (offer.getBuildingInfo _).expects().anyNumberOfTimes().returning(buildingInfo)
      (offer.getCategoryType _).expects().anyNumberOfTimes().returning(CategoryType.APARTMENT)
      (offer.getOfferType _).expects().anyNumberOfTimes().returning(OfferType.SELL)
      (offer.getPartnerId _).expects().anyNumberOfTimes().returning(partnerId)
      (offer.getLocation _).expects().anyNumberOfTimes().returning(location)
      (offer.getCompanyIds _).expects().anyNumberOfTimes().returning(List.empty.asJava)
      toMockFunction4(
        offer
          .addError(_: IndexingError, _: ErrorSource, _: Class[_], _: String)
      ).expects(IndexingError.SITE_OFFER_BASE_FIELD_NOT_SET, *, *, *)
        .anyNumberOfTimes()
        .returning()

      val transaction: Transaction = new Transaction()
      transaction.setDealStatus(DealStatus.PRIMARY_SALE)

      (offer.getTransaction _).expects().anyNumberOfTimes().returning(transaction)

      val apartmentInfo = new ApartmentInfo()
      apartmentInfo.setFlatType(FlatType.NEW_FLAT)
      (offer.getApartmentInfo _).expects().anyNumberOfTimes().returning(apartmentInfo)

      (offer.getOfferState _).expects().anyNumberOfTimes().returning(new OfferState())
      (offer.getArea _).expects().anyNumberOfTimes().returning(AreaInfo.create(AreaUnit.SQUARE_METER, 2f))

      apartmentInfo.setFloors(Seq(Int.box(3)).asJava)
      buildingInfo.setFloorsTotal(4)

      (offer.setPrimarySaleV2 _).expects(java.lang.Boolean.TRUE)
      (offer.setZadPriority _).expects(Int.box(zadPriority))
      (offer.isPrimarySaleV2 _).expects().anyNumberOfTimes().returning(true)
      psUnifier.unify(new OfferWrapper(rawOffer, offer, OfferHistory.justArrived())).futureValue
    }
  }

  "PrimarySaleV2Unifier in isOfferFeedUpdateDateNotExpired" should {
    val partnerId = 123L
    val offer = new Offer()
    offer.setPartnerId(partnerId)

    "return false if feed not found" in {
      val error = Error
        .newBuilder()
        .setCode(ErrorCode.NOT_FOUND)
        .setMessage("not found")
        .build()
      val statusCode = StatusCode.int2StatusCode(404)

      toMockFunction2(feedProcessorClient.getFeedLastUpdateDate(_: Long)(_: Traced))
        .expects(partnerId, *)
        .anyNumberOfTimes()
        .returning(Future.failed(ProtoErrorResponseException(statusCode, error)))
      psUnifier.isOfferFeedUpdateDateNotExpired(offer) shouldBe false
    }

    "return false if last feed update date is expired" in {
      toMockFunction2(feedProcessorClient.getFeedLastUpdateDate(_: Long)(_: Traced))
        .expects(partnerId, *)
        .anyNumberOfTimes()
        .returning(Future.successful(FeedUpdateTime.getDefaultInstance))
      psUnifier.isOfferFeedUpdateDateNotExpired(offer) shouldBe false
    }

    "return true if last feed update date is not expired" in {
      val timestamp = Timestamps.fromMillis(new Date().getTime)
      toMockFunction2(feedProcessorClient.getFeedLastUpdateDate(_: Long)(_: Traced))
        .expects(partnerId, *)
        .anyNumberOfTimes()
        .returning(
          Future.successful(
            FeedUpdateTime
              .newBuilder()
              .setTimestamp(timestamp)
              .build()
          )
        )
      psUnifier.isOfferFeedUpdateDateNotExpired(offer) shouldBe true
    }
  }

  private val inactiveFeedData =
    Table(
      ("description", "feature", "subjectFederationId", "saleAgentCategory", "primarySaleV2"),
      ("feature disabled for owner in Tatarstan", false, Regions.TATARSTAN, SalesAgentCategory.OWNER, false),
      (
        "feature disabled for developer in Tver",
        false,
        Regions.TVERSKAYA_OBLAST,
        SalesAgentCategory.DEVELOPER,
        false
      ),
      ("feature enabled for owner in Tatarstan", true, Regions.TATARSTAN, SalesAgentCategory.OWNER, true),
      ("feature enabled for developer in Tver", true, Regions.TVERSKAYA_OBLAST, SalesAgentCategory.DEVELOPER, true),
      ("offer in banned region - Msk", true, Regions.MSK_AND_MOS_OBLAST, SalesAgentCategory.OWNER, false),
      ("offer in banned region - Spb", true, Regions.SPB_AND_LEN_OBLAST, SalesAgentCategory.DEVELOPER, false),
      ("offer from agency in Tatarstan", true, Regions.TATARSTAN, SalesAgentCategory.AGENCY, false),
      ("offer from agency in Yaroslavl", true, Regions.YAROSLAVSKAYA_OBLAST, SalesAgentCategory.AGENT, false)
    )

  "PrimarySaleV2Unifier in case no active campaign and recent feed update" should {

    forAll(inactiveFeedData) {
      (
        description: String,
        feature: Boolean,
        subjectFederationId: Int,
        salesAgentCategory: SalesAgentCategory,
        primarySaleV2: Boolean
      ) =>
        "check inactive feed continuation for " + description in {
          features.ContinueInactiveFeeds.setNewState(feature)

          val partnerId = 9357L
          val siteId: Long = 8356L
          val zadPriority = 1

          val buildingInfo = new BuildingInfo()
          buildingInfo.setSiteId(siteId)

          val site = new Site(siteId)
          site.setSaleStatus(SaleStatus.ON_SALE)
          site.setPhases(List.empty.asJava)
          (sitesService.getSiteById _).expects(siteId).anyNumberOfTimes().returning(site)

          val offer: Offer = new Offer()
          offer.setId(215141237L)
          offer.setBuildingInfo(buildingInfo)
          offer.setCategoryType(CategoryType.APARTMENT)
          offer.setOfferType(OfferType.SELL)
          offer.setPartnerId(partnerId)
          offer.setCompanyIds(List.empty.asJava)

          val location: Location = new Location()
          location.setRegionGraphId(node.getId)
          location.setSubjectFederation(subjectFederationId, subjectFederationId)
          offer.setLocation(location)

          val transaction: Transaction = new Transaction()
          transaction.setDealStatus(DealStatus.PRIMARY_SALE)
          offer.setTransaction(transaction)

          val apartmentInfo = new ApartmentInfo()
          apartmentInfo.setFlatType(FlatType.NEW_FLAT)
          offer.setApartmentInfo(apartmentInfo)

          val saleAgent = offer.createAndGetSaleAgent()
          saleAgent.setCategory(salesAgentCategory)

          (billingCampaignProvider.get _)
            .expects()
            .anyNumberOfTimes()
            .returning(BillingCampaignStorageImpl(Map()))

          val timestamp = Timestamps.fromMillis(new Date().getTime)
          toMockFunction2(feedProcessorClient.getFeedLastUpdateDate(_: Long)(_: Traced))
            .expects(partnerId, *)
            .anyNumberOfTimes()
            .returning(
              Future.successful(
                FeedUpdateTime
                  .newBuilder()
                  .setTimestamp(timestamp)
                  .build()
              )
            )

          psUnifier.unify(new OfferWrapper(rawOffer, offer, OfferHistory.justArrived())).futureValue

          if (primarySaleV2) {
            offer.isPrimarySaleV2 shouldBe true
          } else {
            offer.getOfferState.getErrors.asScala
              .exists(oe => oe.getError == IndexingError.INVALID_PRIMARY_SALE) shouldBe true
          }
        }
    }
  }

  private def generateOfferBilling(siteId: Long, companyId: Long): OfferBilling = {
    OfferBilling
      .newBuilder()
      .setOfferId(
        OfferId
          .newBuilder()
          .setServiceObject(
            OfferId.ServiceObject
              .newBuilder()
              .setId(siteId.toString)
              .setPartnerId(companyId.toString)
              .build()
          )
          .setVersion(1)
          .build()
      )
      .setKnownCampaign(
        KnownCampaign
          .newBuilder()
          .setCampaign(
            CampaignHeader
              .newBuilder()
              .setId(UUID.randomUUID().toString)
              .setOwner(
                CustomerHeader
                  .newBuilder()
                  .setVersion(1)
                  .build()
              )
              .setVersion(1)
              .setOrder(
                Order
                  .newBuilder()
                  .setVersion(1)
                  .setId(1)
                  .setOwner(
                    CustomerId
                      .newBuilder()
                      .setClientId(1)
                      .setVersion(1)
                      .build()
                  )
                  .setText("")
                  .setCommitAmount(100)
                  .setApproximateAmount(100)
                  .build()
              )
              .setProduct(
                Product
                  .newBuilder()
                  .setVersion(1)
                  .build()
              )
              .setSettings(
                CampaignSettings
                  .newBuilder()
                  .setVersion(1)
                  .setIsEnabled(true)
                  .build()
              )
              .build()
          )
          .build()
      )
      .setVersion(1)
      .build()
  }

}
