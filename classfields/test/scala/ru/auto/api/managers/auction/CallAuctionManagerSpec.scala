package ru.auto.api.managers.auction

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.{OfferListingResponse, Pagination}
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.CallAuctionException
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.auction.CallAuctionManager.AuctionSearchTag
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.personalization.PersonalizationManager.BigBrotherProfile
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.searcher.ApiSearchRequest
import ru.auto.api.model._
import ru.auto.api.search.SearchModel
import ru.auto.api.search.SearchModel.{CatalogFilter, SearchRequestParameters}
import ru.auto.api.services.auction.{AuctionAutoStrategyClient, AuctionClient}
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.yandex.vertis.feature.model.{Feature => VerticsFeature}
import ru.yandex.vertis.mockito.MockitoSupport
import vsmoney.auction.CommonModel.{AuctionContext, CriteriaValue}
import vsmoney.auction_auto_strategy.Settings
import vsmoney.auction_auto_strategy.Settings.AutoStrategyListResponse

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class CallAuctionManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  val vosClientMock = mock[VosClient]
  val searchClientMock = mock[SearcherClient]
  val auctionClientMock = mock[AuctionClient]
  val dataServiceMock = mock[DataService]
  val catalogManagerMock = mock[CatalogManager]
  val featureManagerMock = mock[FeatureManager]
  val auctionAutoStrategyClientMock = mock[AuctionAutoStrategyClient]
  val auctionProtoConverterMock = mock[AuctionProtoConverter]
  val defaultsMapper = new DefaultsMapper(featureManagerMock)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManagerMock)

  when(featureManagerMock.dealerBoostCoefficient).thenReturn(VerticsFeature("", _ => 1.1f))
  when(featureManagerMock.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(VerticsFeature("", _ => true))

  private val DefaultUser = PrivateUserRefGen.next.asRegistered

  implicit private val defaultRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(DefaultUser)
    r
  }

  implicit val bbProfile = BigBrotherProfile.Empty

  private val userInfo = UserInfo(
    ip = "1.1.1.1",
    deviceUid = None,
    optSessionID = None,
    session = None,
    varityResolution = VarityResolution.Human,
    AutoruDealer(2134),
    dealerRef = None,
    dealerUserRole = DealerUserRoles.Unknown
  )

  private val auctionBackContext = {
    val criteriaValues = List(
      CriteriaValue.newBuilder().setKey(CallAuctionManager.RegionCriteriaName).setValue("42").build(),
      CriteriaValue.newBuilder().setKey(CallAuctionManager.MarkCriteriaName).setValue("BMW").build(),
      CriteriaValue.newBuilder().setKey(CallAuctionManager.ModelCriteriaName).setValue("X6").build()
    )

    val criteriaContext = AuctionContext.CriteriaContext
      .newBuilder()
      .addAllCriteriaValues(criteriaValues.asJava)
      .build()

    AuctionContext
      .newBuilder()
      .setCriteriaContext(criteriaContext)
      .build()
  }

  val callAuctionManager = new CallAuctionManager(
    vosClient = vosClientMock,
    searcherClient = searchClientMock,
    searchMappings = searchMappings,
    auctionClient = auctionClientMock,
    auctionAutoStrategyClient = auctionAutoStrategyClientMock,
    dataService = dataServiceMock,
    catalogManager = catalogManagerMock,
    featureManager = featureManagerMock,
    auctionProtoConverter = auctionProtoConverterMock
  )

  "CallAuctionManager" should {
    "return success result and once calling searcher" in {
      mockOldOptionsSearchMapping()
      mockAllowSearcherRequestEnrichmentWithExpFlags()

      val apiRequest = ApiSearchRequest(CategorySelector.Cars, generateSearchRequestParameters())

      val searcherRequest = searchMappings.fromApiToSearcher(apiRequest, Some(Application.iosApp))

      when(searchClientMock.searchOffers(eq(searcherRequest), eq(Paging.DefaultSavedSearches), ?, ?, ?)(?, ?))
        .thenReturn(
          Future.successful(
            generateOfferListingResponse(
              countGenerateOffers = 15,
              totalOffersCount = 15,
              totalPage = 1,
              currentPage = 1
            )
          )
        )

      val result = callAuctionManager.dealersWithAuctionOffers(searcherRequest).await
      result.size shouldBe 15
    }

    "return success result twice calling searcher and distinct filtered dealers" in {
      mockOldOptionsSearchMapping()
      mockAllowSearcherRequestEnrichmentWithExpFlags()

      val apiRequest = ApiSearchRequest(CategorySelector.Cars, generateSearchRequestParameters())

      val searcherRequest = searchMappings.fromApiToSearcher(apiRequest, Some(Application.iosApp))

      when(searchClientMock.searchOffers(eq(searcherRequest), eq(Paging.DefaultSavedSearches), ?, ?, ?)(?, ?))
        .thenReturn(
          Future.successful(
            generateOfferListingResponse(
              countGenerateOffers = 100,
              totalOffersCount = 128,
              totalPage = 2,
              currentPage = 1
            )
          )
        )

      when(
        searchClientMock.searchOffers(
          eq(searcherRequest),
          eq(Paging.DefaultSavedSearches.copy(page = 1 + Paging.DefaultSavedSearches.page)),
          ?,
          ?,
          ?
        )(?, ?)
      ).thenReturn(
        Future.successful(
          generateOfferListingResponse(
            countGenerateOffers = 28,
            totalOffersCount = 128,
            totalPage = 2,
            currentPage = 2,
            prefix = "user_2:"
          )
        )
      )

      val result = callAuctionManager.dealersWithAuctionOffers(searcherRequest).await
      result.size shouldBe 128
    }

    "return success result twice calling searcher and distinct filtered dealers and distinct dublicate dealers" in {
      mockOldOptionsSearchMapping()
      mockAllowSearcherRequestEnrichmentWithExpFlags()

      val apiRequest = ApiSearchRequest(CategorySelector.Cars, generateSearchRequestParameters())

      val searcherRequest = searchMappings.fromApiToSearcher(apiRequest, Some(Application.iosApp))

      when(searchClientMock.searchOffers(eq(searcherRequest), eq(Paging.DefaultSavedSearches), ?, ?, ?)(?, ?))
        .thenReturn(
          Future.successful(
            generateOfferListingResponse(
              countGenerateOffers = 100,
              totalOffersCount = 128,
              totalPage = 2,
              currentPage = 1
            )
          )
        )

      when(
        searchClientMock.searchOffers(
          eq(searcherRequest),
          eq(Paging.DefaultSavedSearches.copy(page = 1 + Paging.DefaultSavedSearches.page)),
          ?,
          ?,
          ?
        )(?, ?)
      ).thenReturn(
        Future.successful(
          generateOfferListingResponse(
            countGenerateOffers = 28,
            totalOffersCount = 128,
            totalPage = 2,
            currentPage = 2
          )
        )
      )

      val result = callAuctionManager.dealersWithAuctionOffers(searcherRequest).await
      result.size shouldBe 100
    }
    "should load auto strategies if feature enableAuctionAutoStrategy turn on" in {
      when(featureManagerMock.enableAuctionAutoStrategy)
        .thenReturn(VerticsFeature("", _ => true))

      val autoStrategySettings = Settings.UserAutoStrategySettings
        .newBuilder()
        .build()

      val responseAutoStrategy = AutoStrategyListResponse
        .newBuilder()
        .addAllSettings(List(autoStrategySettings).asJava)
        .build()

      when(auctionAutoStrategyClientMock.settingsForUser(?)(eq(defaultRequest)))
        .thenReturn(Future.successful(responseAutoStrategy))

      val result = callAuctionManager.getUserAutoStrategies(userInfo, featureManagerMock).await
      result.size shouldBe 1
    }
    "should not call auctionAutoStrategy client if feature enableAuctionAutoStrategy turn off" in {
      when(featureManagerMock.enableAuctionAutoStrategy)
        .thenReturn(VerticsFeature("", _ => false))

      val result = callAuctionManager.getUserAutoStrategies(userInfo, featureManagerMock).await
      result.size shouldBe 0
    }
    "should generate exception if feature enableAuctionAutoStrategy turn on and context exists in enable auto strategy" in {
      when(featureManagerMock.enableAuctionAutoStrategy)
        .thenReturn(VerticsFeature("", _ => true))
      val autoStrategySettings = Settings.UserAutoStrategySettings
        .newBuilder()
        .setContext(auctionBackContext)
        .build()

      val responseAutoStrategy = AutoStrategyListResponse
        .newBuilder()
        .addAllSettings(List(autoStrategySettings).asJava)
        .build()
      when(auctionAutoStrategyClientMock.settingsForUser(?)(eq(defaultRequest)))
        .thenReturn(Future.successful(responseAutoStrategy))

      val resultError = callAuctionManager.onlyIfNoEnableAutoStrategy(userInfo, auctionBackContext).failed.futureValue
      resultError shouldBe an[CallAuctionException]
    }

    "should not generate exception if feature enableAuctionAutoStrategy turn on and context not exists in enable auto strategy" in {
      when(featureManagerMock.enableAuctionAutoStrategy)
        .thenReturn(VerticsFeature("", _ => true))
      val responseAutoStrategy = AutoStrategyListResponse
        .newBuilder()
        .build()
      when(auctionAutoStrategyClientMock.settingsForUser(?)(eq(defaultRequest)))
        .thenReturn(Future.successful(responseAutoStrategy))

      callAuctionManager.onlyIfNoEnableAutoStrategy(userInfo, auctionBackContext).await
    }

    "should not generate exception and not call auctionAutoStrategyClient if feature enableAuctionAutoStrategy turn off" in {
      when(featureManagerMock.enableAuctionAutoStrategy)
        .thenReturn(VerticsFeature("", _ => false))
      callAuctionManager.onlyIfNoEnableAutoStrategy(userInfo, auctionBackContext).await
    }

  }

  private def mockOldOptionsSearchMapping(): Unit = {
    when(featureManagerMock.oldOptionsSearchMapping)
      .thenReturn(new VerticsFeature[Boolean] {
        override def name: String = "old_options_search_mapping"
        override def value: Boolean = false
      })
  }

  private def mockAllowSearcherRequestEnrichmentWithExpFlags(): Unit = {
    when(featureManagerMock.allowSearcherRequestEnrichmentWithExpFlags)
      .thenReturn(new VerticsFeature[Boolean] {
        override def name: String = "allow_searcher_request_enrichment_with_exp_flags"

        override def value: Boolean = false
      })
  }

  private def generateSearchRequestParameters(): SearchRequestParameters = {
    val searchParams = SearchRequestParameters
      .newBuilder()
      .addRid(1)
      .addState(SearchModel.State.NEW)
      .addCatalogFilter(CatalogFilter.newBuilder().setMark("BMW").setModel("X5").build())

    searchParams.addSearchTag(AuctionSearchTag)
    searchParams.build()
  }

  private def generateOfferListingResponse(countGenerateOffers: Int,
                                           totalOffersCount: Int,
                                           totalPage: Int,
                                           currentPage: Int,
                                           prefix: String = "user:"): OfferListingResponse = {
    OfferListingResponse
      .newBuilder()
      .addAllOffers(generateOffers(countGenerateOffers, prefix).asJava)
      .setPagination(
        Pagination
          .newBuilder()
          .setPage(currentPage)
          .setPageSize(Paging.DefaultSavedSearches.pageSize)
          .setTotalOffersCount(totalOffersCount)
          .setTotalPageCount(totalPage)
          .build()
      )
      .build()
  }

  private def generateOffers(offersNumber: Int, prefix: String): Iterable[ApiOfferModel.Offer] = {
    (0 until offersNumber).map { number =>
      ApiOfferModel.Offer
        .newBuilder()
        .setUserRef(prefix + number)
        .build()
    }

  }

}
