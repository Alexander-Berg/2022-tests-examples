package ru.yandex.realty.handlers.sitelike

import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalatest.PrivateMethodTester
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.CommonConstants.{PIK_DEVELOPER_ID, SAMOLET_DEVELOPER_ID}
import ru.yandex.realty.adsource.{AdSource, AdSourceType}
import ru.yandex.realty.api.Slice.Full
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.clients.recommendations.RecommendationsClient
import ru.yandex.realty.clients.recommendations.model.{SiteRecommendationResponse, SiteRecommendationResult}
import ru.yandex.realty.context.street.StreetStorage
import ru.yandex.realty.context.v2.{AuctionResultStorage, HeatmapStorage}
import ru.yandex.realty.geocoder.LocationUnifierService
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.handlers.UserInputContext
import ru.yandex.realty.handlers.search.SearchUserInput
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.sites.{SimpleSiteStatisticsResult, Site}
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.school.SchoolStorage
import ru.yandex.realty.search.model.{AuctionedItem, FoundItem, NewbuildingStats}
import ru.yandex.realty.search.proposal.RealtyProposalService
import ru.yandex.realty.search.{SearchFacade, SearchQuery, Searcher, SiteLikeFacade}
import ru.yandex.realty.services.mortgage.CompletedSiteMortgageMatcher
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.sites.{
  CompaniesStorage,
  ExtendedSiteStatisticsStorage,
  SiteStatisticsStorage,
  SitesGroupingService
}
import ru.yandex.realty.storage.{
  AirportStorage,
  BankStorage,
  CurrencyStorage,
  DeveloperWithChatStorage,
  ExpectedMetroStorage,
  ParkStorage,
  PondStorage
}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.HighwayLocator
import ru.yandex.realty.util.VerbaDescriptionsProvider.VerbaDescriptions

import java.lang
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class SiteLikeManagerSpec extends AsyncSpecBase with PrivateMethodTester {

  private val recommendationsClient: RecommendationsClient = mock[RecommendationsClient]
  private val facade: SiteLikeFacade = mock[SiteLikeFacade]
  private val searcher: Searcher[Iterable[FoundItem]] = mock[Searcher[Iterable[FoundItem]]]
  private val piksiteId = 2L
  private val pikCampaign = new Campaign(
    "222",
    piksiteId,
    333L,
    "target",
    java.util.Map.of(),
    java.util.List.of(),
    java.util.List.of(),
    2L,
    1L,
    true,
    false,
    444L,
    555L,
    Instant.now(),
    java.util.Map.of(),
    null,
    null
  )
  private val manager = new SiteLikeManager(
    searchFacade = mock[SearchFacade],
    searchLikeFacade = facade,
    searcher = searcher,
    recommendationsClient = recommendationsClient,
    sitesService = mock[SitesGroupingService],
    realtyProposalService = mock[RealtyProposalService],
    auctionResultProvider = () => mock[AuctionResultStorage],
    currencyProvider = () => mock[CurrencyStorage],
    siteStatisticsProvider = () => mock[SiteStatisticsStorage],
    heatmapProvider = () => mock[HeatmapStorage],
    extStatisticsProvider = () => mock[ExtendedSiteStatisticsStorage],
    verbaDescriptionsProvider = () => mock[VerbaDescriptions],
    companiesProvider = () => mock[CompaniesStorage],
    campaignProvider = () => new CampaignStorage(Seq(pikCampaign).asJava),
    regionGraphProvider = () => mock[RegionGraph],
    expectedMetroStorageProvider = () => mock[ExpectedMetroStorage],
    parkStorageProvider = () => mock[ParkStorage],
    pondStorageProvider = () => mock[PondStorage],
    airportStorageProvider = () => mock[AirportStorage],
    schoolStorage = mock[SchoolStorage],
    highwayLocatorProvider = () => mock[HighwayLocator],
    developerWithChatProvider = () => mock[DeveloperWithChatStorage],
    locationUnifierService = mock[LocationUnifierService],
    streetStorage = mock[StreetStorage],
    geohubClient = mock[GeohubClient],
    completedSiteMortgageMatcher = mock[CompletedSiteMortgageMatcher],
    bankStorage = () => mock[BankStorage]
  )(ExecutionContext.global)

  private val response: SiteRecommendationResponse[String] = new SiteRecommendationResponse[String](
    new SiteRecommendationResult[String]("methodName", "methodDetails", Seq("1").toList)
  )
  (recommendationsClient
    .getSiteRecommendations(_: Long, _: Iterable[String])(_: Traced))
    .expects(*, *, *)
    .returning(Future.successful(response))

  (facade
    .search(_: SearchUserInput)(_: Traced))
    .expects(*, *)
    .returning(Future.successful(Seq.empty[AuctionedItem]))

  (searcher
    .search(_: SearchQuery, _: Option[UserRef]))
    .expects(*, *)
    .returning(
      scala.util.Success(
        Seq(
          initFoundItem(1L, 12341L),
          initFoundItem(piksiteId, PIK_DEVELOPER_ID),
          initFoundItem(3L, SAMOLET_DEVELOPER_ID),
          initFoundItem(4L, 109236L),
          initFoundItem(5L, SAMOLET_DEVELOPER_ID)
        )
      )
    )

  "SiteLikeManager" should {
    "mixedSearch sort samolet first" in {
      val mixedSearch = PrivateMethod[Future[Seq[AuctionedItem]]]('mixedSearch)
      val input = SearchUserInput(
        siteId = Seq(1L),
        siteContext = UserInputContext(currency = None)
      )
      val adSource = Some(AdSource(AdSourceType.YANDEX_DIRECT, 1652902472590L))
      val slice = Full
      val result: Seq[AuctionedItem] =
        manager.invokePrivate(mixedSearch(input, adSource, slice, Traced.empty)).futureValue
      result.length shouldBe 5
      result.head.found.site.getId shouldBe 3L
      result.head.found.site.getBuilders.get(0) shouldBe SAMOLET_DEVELOPER_ID
      result(1).found.site.getId shouldBe 5L
      result(1).found.site.getBuilders.get(0) shouldBe SAMOLET_DEVELOPER_ID
      result(2).found.site.getId shouldBe 2L
      result(2).found.site.getBuilders.get(0) shouldBe PIK_DEVELOPER_ID
      result(3).found.site.getId shouldBe 1L
      result(3).found.site.getBuilders.get(0) shouldBe 12341L
      result(4).found.site.getId shouldBe 4L
      result(4).found.site.getBuilders.get(0) shouldBe 109236L
    }
  }

  private def initFoundItem(siteId: Long, developerId: lang.Long) = {
    val site = new Site(siteId)
    site.setBuilders(Seq(developerId).asJava)

    FoundItem(
      site,
      exactMatch = true,
      isActual = true,
      NewbuildingStats(
        SimpleSiteStatisticsResult.EMPTY,
        SimpleSiteStatisticsResult.EMPTY
      )
    )
  }
}
