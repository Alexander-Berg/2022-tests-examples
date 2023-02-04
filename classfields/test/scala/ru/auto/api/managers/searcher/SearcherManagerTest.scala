package ru.auto.api.managers.searcher

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.Materializer
import org.mockito.Mockito._
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.ResponseModel.{OfferListingResponse, Pagination}
import ru.auto.api.auth.{Application, Grants}
import ru.auto.api.broker_events.BigbEvents.BigbSearcherEvent
import ru.auto.api.exceptions.AutoruProProhibitedException
import ru.auto.api.experiments.RelatedFromRecommendationService
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.catalog.{CatalogDecayManager, CatalogManager}
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.fake.FakeManager.CheckedSearcherRequest
import ru.auto.api.managers.favorite.SavedSearchesManager
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.matchapplications.MatchApplicationsManager
import ru.auto.api.managers.searcher.SearcherManager.RelatedSorting
import ru.auto.api.model.CategorySelector.{Cars, Moto}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils.RichBoolean
import ru.auto.api.model._
import ru.auto.api.model.favorite.{OfferSavedSearchFactory, OfferSearchesDomain, SavedSearchFactoryProvider}
import ru.auto.api.model.searcher.{ApiSearchRequest, GroupBy, SearcherRequest}
import ru.auto.api.search.SearchModel.{CarsSearchRequestParameters, SearchRequestParameters}
import ru.auto.api.services.MockedHttpClient
import ru.auto.api.services.bigbrother.BigBrotherClient
import ru.auto.api.services.history.HistoryClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.paging.offerPagingStreamer
import ru.auto.api.services.recommender.RecommenderClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.testkit.TestData
import ru.auto.api.ui.UiModel
import ru.auto.api.ui.UiModel.TristateTumblerGroup
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.search.{SearchMappings, SearcherRequestMapper, SearchesUtils}
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.auto.catalog.model.api.ApiModel.{CatalogLevel, RawCatalog, SubTreeReturnMode, TechParamCard}
import ru.yandex.proto.crypta.Profile
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by sanekas on 01/06/2017.
  */
class SearcherManagerTest
  extends BaseSpec
  with MockitoSupport
  with MockedHttpClient
  with AsyncTasksSupport
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  private val searcherClient = mock[SearcherClient]
  private val enrichManager = mock[EnrichManager]
  private val decayManager = mock[DecayManager]
  private val savedSearchManager = mock[SavedSearchesManager]
  private val historyClient = mock[HistoryClient]
  private val appsFeaturesManager = mock[AppsFeaturesManager]
  private val matchApplicationsManager = mock[MatchApplicationsManager]
  private val featureManager: FeatureManager = mock[FeatureManager]
  private val recommenderClient: RecommenderClient = mock[RecommenderClient]
  private val catalogManager: CatalogManager = mock[CatalogManager]
  private val searchRequestMapper = new SearcherRequestMapper(appsFeaturesManager, featureManager)

  private val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)
  when(featureManager.allowSearcherRequestEnrichmentWithExpFlags).thenReturn(Feature("", _ => true))
  when(featureManager.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(Feature("", _ => true))
  when(featureManager.dealerBoostCoefficient).thenReturn(Feature("", _ => 1.1f))

  private val maxRecommendedTechParamsFeature: Feature[Int] = mock[Feature[Int]]
  when(featureManager.maxRecommendedTechParamsForListing).thenReturn(maxRecommendedTechParamsFeature)

  when(matchApplicationsManager.checkIfCanShowMatchApplicationForm(?, ?, ?)(?)).thenReturnF(false)

  private val defaultsMapper = new DefaultsMapper(featureManager)
  private val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  private val savedSearchFactoryProvider = new SavedSearchFactoryProvider(searchMappings)

  private val bigBrotherClient = mock[BigBrotherClient]
  when(bigBrotherClient.getProfile(?)(?)).thenReturn(Future.successful(Profile.getDefaultInstance))

  private val brokerClient = mock[BrokerClient]
  when(brokerClient.send(any[String](), any[BigbSearcherEvent]())(?)).thenReturn(Future.unit)
  val fakeManager: FakeManager = mock[FakeManager]

  private val searcherManager = new SearcherManager(
    searcherClient,
    enrichManager,
    decayManager,
    fakeManager,
    savedSearchManager,
    offerPagingStreamer,
    searchRequestMapper,
    searchMappings,
    new OfferSavedSearchFactory(searchMappings),
    matchApplicationsManager,
    recommenderClient,
    catalogManager,
    bigBrotherClient,
    brokerClient,
    TestData.tree,
    featureManager,
    new CatalogDecayManager,
    TestData.electroPromoLandingInfo
  )

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: Materializer = Materializer.createMaterializer(system)

  private val maxPoster = Application.external(
    "maxposter",
    RateLimit.PerApplication(300),
    Grants.Breadcrumbs,
    Grants.Catalog,
    Grants.PassportLogin,
    Grants.Search
  )

  "SearcherManager" should {

    "enrich & decay offers" in {
      val r = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(maxPoster)

      val mockedResponse = listingResponseGen(OfferGen).next
      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)
      when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(mockedResponse)
      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(mockedResponse)

      when(enrichManager.enrich(any[OfferListingResponse](), eq.apply(EnrichOptions.ForSearchListing))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))
      when(decayManager.decay(any[OfferListingResponse](), eq.apply(DecayOptions.full))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))

      val apiRequest = ApiSearchRequest(Cars, SearchRequestParameters.getDefaultInstance)
      val mappedReq = searchMappings.fromApiToSearcher(apiRequest, Some(r.application))(r)
      when(fakeManager.checkSearcherRequest(?, ?)(?))
        .thenReturn(CheckedSearcherRequest(mappedReq, mappedReq, NoSorting, NoSorting))

      val response = searcherManager
        .getListing(
          apiRequest,
          Paging.Default,
          NoSorting,
          SearchRequestContext.Empty,
          GroupBy.NoGrouping
        )(r)
        .futureValue

      response.getOffersCount shouldBe mockedResponse.getOffersCount
    }

    "handle with_pro=ONLY without access" in {
      val r = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(maxPoster)

      val mockedResponse = listingResponseGen(OfferGen).next

      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(mockedResponse)

      when(enrichManager.enrich(any[OfferListingResponse](), eq.apply(EnrichOptions.ForSearchListing))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))
      when(decayManager.decay(any[OfferListingResponse](), eq.apply(DecayOptions.full))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))

      val params = SearchRequestParameters.newBuilder()
      params.setWithAutoruExpert(TristateTumblerGroup.ONLY)

      val apiRequest = ApiSearchRequest(Cars, params.build())
      val mappedReq = searchMappings.fromApiToSearcher(apiRequest, Some(r.application))(r)
      when(fakeManager.checkSearcherRequest(?, ?)(?))
        .thenReturn(CheckedSearcherRequest(mappedReq, mappedReq, NoSorting, NoSorting))

      intercept[AutoruProProhibitedException] {
        searcherManager
          .getListing(
            apiRequest,
            Paging.Default,
            NoSorting,
            SearchRequestContext.Empty,
            GroupBy.NoGrouping
          )(r)
          .futureValue
      }
    }

    "handle with_pro=ONLY with access" in {
      implicit val r: RequestImpl = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setSession(ResellerSessionResultGen.next)
      r.setApplication(Application.iosApp)
      val user = PrivateUserRefGen.next
      r.setUser(user)

      val mockedResponse = listingResponseGen(OfferGen).next
      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)
      when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(mockedResponse)

      when(savedSearchManager.savedSearch(?, ?, ?)(?)).thenReturnF(None)
      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(mockedResponse)

      when(enrichManager.enrich(any[OfferListingResponse](), eq.apply(EnrichOptions.ForSearchListing))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))
      when(decayManager.decay(any[OfferListingResponse](), eq.apply(DecayOptions.full))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))

      val params = SearchRequestParameters.newBuilder()
      params.setWithAutoruExpert(TristateTumblerGroup.ONLY)

      val apiRequest = ApiSearchRequest(Cars, params.build())
      val mappedReq = searchMappings.fromApiToSearcher(apiRequest, Some(r.application))(r)
      when(fakeManager.checkSearcherRequest(?, ?)(?))
        .thenReturn(CheckedSearcherRequest(mappedReq, mappedReq, NoSorting, NoSorting))

      val response = searcherManager
        .getListing(
          apiRequest,
          Paging.Default,
          NoSorting,
          SearchRequestContext.Empty,
          GroupBy.NoGrouping
        )(r)
        .futureValue
      response.getOffersCount shouldBe mockedResponse.getOffersCount

      verify(savedSearchManager).savedSearch(eq(OfferSearchesDomain), eq(user), ?)(eq(r))
    }

    "handle with_pro=ONLY for moderator" in {
      implicit val r: Request = PrivateModeratorRequestGen.next

      val mockedResponse = listingResponseGen(OfferGen).next
      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)
      when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(mockedResponse)

      when(savedSearchManager.savedSearch(?, ?, ?)(?)).thenReturnF(None)
      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(mockedResponse)

      when(enrichManager.enrich(any[OfferListingResponse](), eq.apply(EnrichOptions.ForSearchListing))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))
      when(decayManager.decay(any[OfferListingResponse](), eq.apply(DecayOptions.full))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))

      val params = SearchRequestParameters.newBuilder()
      params.setWithAutoruExpert(TristateTumblerGroup.ONLY)

      val apiRequest = ApiSearchRequest(Cars, params.build())
      val mappedReq = searchMappings.fromApiToSearcher(apiRequest, Some(r.application))(r)
      when(fakeManager.checkSearcherRequest(?, ?)(?))
        .thenReturn(CheckedSearcherRequest(mappedReq, mappedReq, NoSorting, NoSorting))

      val response = searcherManager
        .getListing(
          apiRequest,
          Paging.Default,
          NoSorting,
          SearchRequestContext.Empty,
          GroupBy.NoGrouping
        )(r)
        .futureValue
      response.getOffersCount shouldBe mockedResponse.getOffersCount

      verify(savedSearchManager).savedSearch(eq(OfferSearchesDomain), eq(r.user.personalRef), ?)(eq(r))
    }

    "add additional params to SavedSearch" in {
      implicit val r: RequestImpl = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(Application.iosApp)
      r.setUser(PersonalUserRefGen.next)
      val additionalParams = SearchRequestParameters.newBuilder().setCreationDateFrom(111).build()
      val savedSearchResponse = {
        val b = SavedSearchResponseGen.next.toBuilder
        b.getSearchBuilder.getParamsBuilder.clearCreationDateFrom()
        b.build()
      }
      val mockedResponse = listingResponseGen(OfferGen).next
      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)
      when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(mockedResponse)

      when(savedSearchManager.get(?, ?, ?)(?)).thenReturnF(savedSearchResponse)
      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(mockedResponse)

      val response = searcherManager
        .getListingBySearchId(
          r.user.personalRef,
          "testId",
          additionalParams,
          Paging.Default,
          NoSorting,
          GroupBy.NoGrouping
        )
        .futureValue

      response.getSearchParameters.getCreationDateFrom shouldBe 111

      verify(savedSearchManager).get(OfferSearchesDomain, r.user.personalRef, "testId")(r)
    }

    "search for all offers" in {
      forAll(Gen.listOfN(5, OfferGen), searcherParamsGen, sortingGen) { (offers, params, sorting) =>
        reset(searcherClient)
        implicit val request: Request = {
          val r = new RequestImpl
          r.setRequestParams(RequestParams.empty)
          r.setApplication(Application.iosApp)
          r.setUser(PersonalUserRefGen.next)
          r
        }
        val List(offer1, offer2, offer3, offer4, offer5) = offers: @unchecked
        val page1 = OfferListingResponse
          .newBuilder()
          .addAllOffers(List(offer1, offer2).asJava)
          .setPagination(
            // we don't actually care about page_size & total_offers_count in this tests, but they are required fields
            Pagination.newBuilder().setPage(1).setTotalPageCount(3).setPageSize(20).setTotalOffersCount(55)
          )
          .build()
        val page2 = OfferListingResponse
          .newBuilder()
          .addAllOffers(List(offer3, offer4).asJava)
          .setPagination(
            Pagination.newBuilder().setPage(2).setTotalPageCount(3).setPageSize(20).setTotalOffersCount(55)
          )
          .build()
        val page3 = OfferListingResponse
          .newBuilder()
          .addAllOffers(List(offer5).asJava)
          .setPagination(
            Pagination.newBuilder().setPage(3).setTotalPageCount(3).setPageSize(20).setTotalOffersCount(55)
          )
          .build()
        when(
          searcherClient.searchOffers(eq(params), eq(Paging(1, 20)), eq(sorting), eq(GroupBy.NoGrouping), eq(None))(
            ?,
            ?
          )
        ).thenReturnF(page1)
        when(
          searcherClient.searchOffers(eq(params), eq(Paging(2, 20)), eq(sorting), eq(GroupBy.NoGrouping), eq(None))(
            ?,
            ?
          )
        ).thenReturnF(page2)
        when(
          searcherClient.searchOffers(eq(params), eq(Paging(3, 20)), eq(sorting), eq(GroupBy.NoGrouping), eq(None))(
            ?,
            ?
          )
        ).thenReturnF(page3)
        val result = searcherManager
          .searchOffers(params, sorting)
          .toMat(Sink.seq)(Keep.right)
          .run()
          .futureValue
        val expected = Seq(offer1, offer2, offer3, offer4, offer5)
        // separate id comparison for simpler error message in case of failure
        result.map(_.getId) shouldBe expected.map(_.getId)
        result shouldBe expected
        verify(searcherClient).searchOffers(
          eq(params),
          eq(Paging(1, 20)),
          eq(sorting),
          eq(GroupBy.NoGrouping),
          eq(None)
        )(
          ?,
          ?
        )
        verify(searcherClient).searchOffers(
          eq(params),
          eq(Paging(2, 20)),
          eq(sorting),
          eq(GroupBy.NoGrouping),
          eq(None)
        )(
          ?,
          ?
        )
        verify(searcherClient).searchOffers(
          eq(params),
          eq(Paging(3, 20)),
          eq(sorting),
          eq(GroupBy.NoGrouping),
          eq(None)
        )(
          ?,
          ?
        )
        verifyNoMoreInteractions(searcherClient)
      }
    }

    "add additional params in catalog_filter" in {
      val r = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(Application.iosApp)

      val techInfo = TechInfoGen.values.take(11).toArray
      val mmng = techInfo.map(MarkModelNameplateGeneration.from)

      when(searcherClient.getSimilar(?, ?, ?, ?)(?)).thenReturnF(mmng.toSeq.take(10))

      val b = SearchRequestParameters.getDefaultInstance.toBuilder
      b.addCatalogFilter(mmng(10).toCatalogFilter())
      //b.addCatalogFilter(mmng(10).toCatalogFilter())

      val response = searcherManager
        .relatedMMMG(ApiSearchRequest(Cars, b.build()), includeOriginalFilter = true)(r)
        .futureValue

      response.params.getCatalogFilterCount shouldBe mmng.length
    }

    "get related search with experiment and stateGroup=NEW" in {
      val techInfos = Gen.listOfN(5, TechInfoGen).next
      val relatedOffers = Gen.listOfN(3, OfferGen).next
      val subtreeTechParams = Gen.listOfN(3, OfferGen).next
      val relatedTechParams = Gen.listOfN(3, OfferGen).next
      reset(searcherClient, enrichManager, decayManager, catalogManager, recommenderClient)
      val category = Cars
      val catalogFilters = techInfos.map(MarkModelNameplateGeneration.from).map(_.toCatalogFilter()).distinct
      val subtreeTechParamIds = subtreeTechParams.map(_.getCarInfo.getTechParamId).distinct
      val relatedIds = relatedTechParams.map(_.getCarInfo.getTechParamId).distinct

      implicit val request: Request = {
        val r = new RequestImpl
        r.setRequestParams(
          RequestParams.construct("1.1.1.1", experiments = Set(RelatedFromRecommendationService.desktopExp))
        )
        r.setApplication(Application.web)
        r.setToken(TokenServiceImpl.web)
        r.setUser(PersonalUserRefGen.next)
        r
      }

      val searchRequest = SearchRequestParameters
        .newBuilder()
        .setStateGroup(UiModel.StateGroup.NEW)
        .addAllCatalogFilter(catalogFilters.asJava)
        .build()

      val response = OfferListingResponse
        .newBuilder()
        .addAllOffers(relatedOffers.asJava)
        .build()

      val apiRequest = ApiSearchRequest(
        category,
        SearchRequestParameters.newBuilder
          .setStateGroup(UiModel.StateGroup.NEW)
          .setCarsParams(
            CarsSearchRequestParameters.newBuilder
              .addAllTechParamId(relatedIds.map(_.toString).asJava)
          )
          .build()
      )
      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)
      when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(response)

      val searcherRequest = searchMappings.constructRelatedRequest(apiRequest, Paging.Default, false)
      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(response)
      when(enrichManager.enrich(any[OfferListingResponse](), ?)(?)).thenReturnF(response)
      when(decayManager.decay(any[OfferListingResponse](), ?)(?)).thenReturnF(response)

      val techParamMap = subtreeTechParamIds
        .map(_.toString)
        .map(_ -> TechParamCard.getDefaultInstance)
        .toMap
        .asJava
      val rawCatalog = RawCatalog.newBuilder.putAllTechParam(techParamMap).build
      when(catalogManager.subtreeByCatalogFilter(?, ?, ?, ?, ?, ?)(?)).thenReturnF(rawCatalog)

      when(recommenderClient.getTechParams(?)(?)).thenReturnF(relatedIds)

      reset(maxRecommendedTechParamsFeature)
      when(maxRecommendedTechParamsFeature.value).thenReturn(10)

      val relatedResponse = searcherManager
        .getRelatedSearch(
          ApiSearchRequest(category, searchRequest),
          Paging.Default,
          SearchRequestContext.Empty,
          GroupBy.NoGrouping
        )
        .futureValue

      verify(maxRecommendedTechParamsFeature).value
      reset(maxRecommendedTechParamsFeature)

      val searchId = SearchesUtils.generateId(searcherRequest)
      relatedResponse shouldBe searcherManager.addMetaTo(
        response,
        searcherRequest,
        searchId,
        sorting = Some(RelatedSorting)
      )

      verify(catalogManager).subtreeByCatalogFilter(
        category,
        None,
        catalogFilters,
        SubTreeReturnMode.newBuilder.setFrom(CatalogLevel.MARK).setTo(CatalogLevel.TECH_PARAM).build(),
        failNever = false,
        legacyMode = true
      )

      verify(recommenderClient, times(subtreeTechParamIds.size)).getTechParams(?)(?)
      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)

      verify(searcherClient).searchOffers(
        eq(searcherRequest),
        eq(Paging.Default),
        eq(RelatedSorting),
        eq(GroupBy.NoGrouping),
        eq(Some(searchId))
      )(?, ?)
      verify(enrichManager).enrich(response, EnrichOptions.ForSearchListing)
      verify(decayManager).decay(response, DecayOptions.full)
    }

    "get related search with experiment" in {
      forAll(
        Gen.listOfN(5, TechInfoGen),
        Gen.listOfN(3, OfferGen),
        Gen.listOfN(3, OfferGen),
        Gen.listOfN(3, OfferGen)
      ) {
        case (techInfos, relatedOffers, subtreeTechParams, relatedTechParams) =>
          reset(searcherClient, enrichManager, decayManager, catalogManager, recommenderClient)
          val category = Cars
          val catalogFilters = techInfos.map(MarkModelNameplateGeneration.from).map(_.toCatalogFilter()).distinct
          val subtreeTechParamIds = subtreeTechParams.map(_.getCarInfo.getTechParamId).distinct
          val relatedIds = relatedTechParams.map(_.getCarInfo.getTechParamId).distinct

          implicit val request: Request = {
            val r = new RequestImpl
            r.setRequestParams(
              RequestParams.construct("1.1.1.1", experiments = Set(RelatedFromRecommendationService.desktopExp))
            )
            r.setApplication(Application.web)
            r.setToken(TokenServiceImpl.web)
            r.setUser(PersonalUserRefGen.next)
            r
          }

          val searchRequest = SearchRequestParameters
            .newBuilder()
            .addAllCatalogFilter(catalogFilters.asJava)
            .build()

          val response = OfferListingResponse
            .newBuilder()
            .addAllOffers(relatedOffers.asJava)
            .build()

          val apiRequest = ApiSearchRequest(
            category,
            SearchRequestParameters.newBuilder
              .setCarsParams(
                CarsSearchRequestParameters.newBuilder
                  .addAllTechParamId(relatedIds.map(_.toString).asJava)
              )
              .build()
          )
          val searcherRequest = searchMappings.constructRelatedRequest(apiRequest, Paging.Default, false)
          when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(response)
          when(enrichManager.enrich(any[OfferListingResponse](), ?)(?)).thenReturnF(response)
          when(decayManager.decay(any[OfferListingResponse](), ?)(?)).thenReturnF(response)

          val techParamMap = subtreeTechParamIds
            .map(_.toString)
            .map(_ -> TechParamCard.getDefaultInstance)
            .toMap
            .asJava
          val rawCatalog = RawCatalog.newBuilder.putAllTechParam(techParamMap).build
          when(catalogManager.subtreeByCatalogFilter(?, ?, ?, ?, ?, ?)(?)).thenReturnF(rawCatalog)

          when(recommenderClient.getTechParams(?)(?)).thenReturnF(relatedIds)
          when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
          when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)
          when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(response)

          reset(maxRecommendedTechParamsFeature)
          when(maxRecommendedTechParamsFeature.value).thenReturn(10)

          val relatedResponse = searcherManager
            .getRelatedSearch(
              ApiSearchRequest(category, searchRequest),
              Paging.Default,
              SearchRequestContext.Empty,
              GroupBy.NoGrouping
            )
            .futureValue

          verify(maxRecommendedTechParamsFeature).value
          reset(maxRecommendedTechParamsFeature)

          val searchId = SearchesUtils.generateId(searcherRequest)
          relatedResponse shouldBe searcherManager.addMetaTo(
            response,
            searcherRequest,
            searchId,
            sorting = Some(RelatedSorting)
          )

          verify(catalogManager).subtreeByCatalogFilter(
            category,
            None,
            catalogFilters,
            SubTreeReturnMode.newBuilder.setFrom(CatalogLevel.MARK).setTo(CatalogLevel.TECH_PARAM).build(),
            failNever = false,
            legacyMode = true
          )

          verify(recommenderClient, times(subtreeTechParamIds.size)).getTechParams(?)(?)

          verify(searcherClient).searchOffers(
            eq(searcherRequest),
            eq(Paging.Default),
            eq(RelatedSorting),
            eq(GroupBy.NoGrouping),
            eq(Some(searchId))
          )(?, ?)
          verify(enrichManager).enrich(response, EnrichOptions.ForSearchListing)
          verify(decayManager).decay(response, DecayOptions.full)
      }
    }

    "get related search without experiment" in {
      forAll(
        Gen.listOfN(5, TechInfoGen),
        Gen.listOfN(3, OfferGen),
        Gen.listOfN(3, TechInfoGen)
      ) {
        case (techInfos, relatedOffers, relatedTechInfos) =>
          reset(searcherClient, enrichManager, decayManager, catalogManager, recommenderClient)
          val category = Cars
          val catalogFilters = techInfos.map(MarkModelNameplateGeneration.from).map(_.toCatalogFilter()).distinct
          val relatedMMNG = relatedTechInfos.map(MarkModelNameplateGeneration.from)

          implicit val request: Request = {
            val r = new RequestImpl
            r.setRequestParams(RequestParams.construct("1.1.1.1"))
            r.setApplication(Application.iosApp)
            r.setUser(PersonalUserRefGen.next)
            r
          }

          val searchRequestParams = SearchRequestParameters
            .newBuilder()
            .addAllCatalogFilter(catalogFilters.asJava)
            .build()

          val actualSearchRequest = SearchRequestParameters
            .newBuilder()
            .addAllCatalogFilter(relatedMMNG.map(m => m.toCatalogFilter(m.generation.isDefined)).asJava)
            .build()

          val response = OfferListingResponse
            .newBuilder()
            .addAllOffers(relatedOffers.asJava)
            .build()

          val apiRequest = ApiSearchRequest(category, actualSearchRequest)
          val searcherRequest = searchMappings.constructRelatedRequest(apiRequest, Paging.Default, false)
          when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(response)
          when(enrichManager.enrich(any[OfferListingResponse](), ?)(?)).thenReturnF(response)
          when(decayManager.decay(any[OfferListingResponse](), ?)(?)).thenReturnF(response)
          when(searcherClient.getSimilar(?, ?, ?, ?)(?)).thenReturnF(relatedMMNG)

          val relatedResponse = searcherManager
            .getRelatedSearch(
              ApiSearchRequest(category, searchRequestParams),
              Paging.Default,
              SearchRequestContext.Empty,
              GroupBy.NoGrouping
            )
            .futureValue

          val searchId = SearchesUtils.generateId(searcherRequest)
          relatedResponse shouldBe searcherManager.addMetaTo(
            relatedResponse,
            searcherRequest,
            searchId,
            sorting = Some(RelatedSorting)
          )

          val mmng = MarkModelNameplateGeneration.from(catalogFilters.head).get
          val yearFrom = searchRequestParams.hasYearFrom.toOption(searchRequestParams.getYearFrom)
          val yearTo = searchRequestParams.hasYearTo.toOption(searchRequestParams.getYearFrom)
          verify(searcherClient).getSimilar(
            eq(mmng),
            eq(searchRequestParams.getRidList.asScala.toSeq),
            eq(yearFrom),
            eq(yearTo)
          )(?)
          verify(enrichManager).enrich(response, EnrichOptions.ForSearchListing)
          verify(decayManager).decay(response, DecayOptions.full)
          verifyNoMoreInteractions(catalogManager)
          verifyNoMoreInteractions(recommenderClient)
      }
    }

    "get related search with experiment but moto category" in {
      forAll(Gen.listOfN(5, TechInfoGen)) { techInfos =>
        reset(catalogManager, recommenderClient, searcherClient)
        val category = Moto
        val catalogFilters = techInfos.map(MarkModelNameplateGeneration.from).map(_.toCatalogFilter()).distinct

        implicit val request: Request = {
          val r = new RequestImpl
          r.setRequestParams(
            RequestParams.construct("1.1.1.1", experiments = Set(RelatedFromRecommendationService.desktopExp))
          )
          r.setApplication(Application.iosApp)
          r.setUser(PersonalUserRefGen.next)
          r
        }

        val searchRequestParams = SearchRequestParameters
          .newBuilder()
          .addAllCatalogFilter(catalogFilters.asJava)
          .build()

        val response = SearcherManager.EmptyListing
        val apiRequest = ApiSearchRequest(category, searchRequestParams)
        val searchReq = searchMappings.fromApiToSearcher(apiRequest, None)

        val relatedResponse = searcherManager
          .getRelatedSearch(
            ApiSearchRequest(category, searchRequestParams),
            Paging.Default,
            SearchRequestContext.Empty,
            GroupBy.NoGrouping
          )
          .futureValue

        val searchId = SearchesUtils.generateId(searchReq)
        relatedResponse shouldBe searcherManager.addMetaTo(
          response,
          searchReq,
          searchId,
          sorting = Some(RelatedSorting)
        )

        verifyNoMoreInteractions(catalogManager)
        verifyNoMoreInteractions(recommenderClient)
        verifyNoMoreInteractions(searcherClient)
      }
    }

    "use Moscow rid for Bryansk in web" in {
      val BryanskRid = 191
      val r = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(Application.web)

      val b = SearchRequestParameters.getDefaultInstance.toBuilder
      b.addRid(BryanskRid)
      b.setGeoRadius(50)

      val (_, oRid) = searcherManager.setupNearestFederalCity(ApiSearchRequest(Cars, b.build()), r)

      oRid shouldBe Some(SearcherManager.MoscowRid)
    }

    "don't use other city for old client" in {
      val BryanskRid = 191
      val r = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(Application.androidApp)

      val b = SearchRequestParameters.getDefaultInstance.toBuilder
      b.addRid(BryanskRid)
      b.setGeoRadius(50)

      val (_, oRid) = searcherManager.setupNearestFederalCity(ApiSearchRequest(Cars, b.build()), r)

      oRid shouldBe None
    }

    "use Moscow rid for Bryansk for new client" in {
      val BryanskRid = 191
      val r = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(Application.androidApp)
      r.setClientFeatures(Set(ClientFeature.RECOMMEND_NEW_IN_STOCK_FEDERAL_CITY))

      val b = SearchRequestParameters.getDefaultInstance.toBuilder
      b.addRid(BryanskRid)
      b.setGeoRadius(50)

      val (_, oRid) = searcherManager.setupNearestFederalCity(ApiSearchRequest(Cars, b.build()), r)

      oRid shouldBe Some(SearcherManager.MoscowRid)
    }

    "replaceDefaultNameplateForSearcher should replace only default nameplates" in {
      val searchRequest = SearcherRequest.apply(
        Cars,
        Map(
          "catalog_filter" -> Set(
            "mark=KIA,model=RIO,nameplate_name=x-line",
            "mark=AUDI,model=A8,nameplate_name=a",
            "mark=AUDI,model=A8,nameplate_name=a8",
            "mark=AUDI,model=A8,nameplate_name=a80",
            "mark=AUDI,model=R8,nameplate_name=r,vendor=AAA",
            "mark=AUDI,model=R8,nameplate_name=r8,vendor=AAA",
            "mark=AUDI,model=R8,nameplate_name=r80,vendor=AAA",
            "mark=VOLKSWAGEN,model=POLO",
            "mark=BMW"
          )
        )
      )
      val updatedSearchRequest = searcherManager.replaceDefaultNameplateForSearcher(searchRequest)
      updatedSearchRequest.params.get("catalog_filter") shouldBe Some(
        Set(
          "mark=KIA,model=RIO,nameplate_name=x-line",
          "mark=AUDI,model=A8,nameplate_name=a",
          "mark=AUDI,model=A8,nameplate_name=--",
          "mark=AUDI,model=A8,nameplate_name=a80",
          "mark=AUDI,model=R8,nameplate_name=r,vendor=AAA",
          "mark=AUDI,model=R8,nameplate_name=--,vendor=AAA",
          "mark=AUDI,model=R8,nameplate_name=r80,vendor=AAA",
          "mark=VOLKSWAGEN,model=POLO",
          "mark=BMW"
        )
      )
    }
  }
}
