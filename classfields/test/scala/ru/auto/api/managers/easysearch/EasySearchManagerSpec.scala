package ru.auto.api.managers.easysearch

import org.mockito.Mockito.{reset, times, verify, verifyNoMoreInteractions}
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.api.auth.{Application, Grants}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.personalization.PersonalizationManager.BigBrotherProfile
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model._
import ru.auto.api.model.searcher.ApiSearchRequest
import ru.auto.api.reviews.ReviewsResponseModel.{FeaturesResponse, ReviewsRatingResponse}
import ru.auto.api.search.SearchModel.SearchRequestParameters
import ru.auto.api.services.MockedHttpClient
import ru.auto.api.services.review.ReviewClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.ui.UiModel.TristateTumblerGroup
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.RequestImpl
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class EasySearchManagerSpec
  extends BaseSpec
  with MockitoSupport
  with MockedHttpClient
  with AsyncTasksSupport
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  private val searcherClient = mock[SearcherClient]
  private val decayManager = mock[DecayManager]
  private val featureManager: FeatureManager = mock[FeatureManager]
  private val vosReviewClient: ReviewClient = mock[ReviewClient]

  private val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)
  when(featureManager.allowSearcherRequestEnrichmentWithExpFlags).thenReturn(Feature("", _ => true))
  when(featureManager.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(Feature("", _ => true))
  when(featureManager.dealerBoostCoefficient).thenReturn(Feature("", _ => 1.1f))

  private val defaultsMapper = new DefaultsMapper(featureManager)
  private val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)

  private val easySearchManager = new EasySearchManager(searcherClient, searchMappings, decayManager, vosReviewClient)

  private val maxPoster = Application.external(
    "maxposter",
    RateLimit.PerApplication(300),
    Grants.Breadcrumbs,
    Grants.Catalog,
    Grants.PassportLogin,
    Grants.Search,
    Grants.EasySearch
  )

  "EasySearchManager.groupsListing" should {

    "return 10 groups" in {
      val r = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      r.setApplication(maxPoster)

      val respBuilder = OfferListingResponse.newBuilder()
      respBuilder.addAllOffers(Gen.listOfN(10, OfferGen).next.asJava)
      val mockedResponse = respBuilder.build

      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(mockedResponse)

      when(decayManager.decay(any[OfferListingResponse](), eq.apply(DecayOptions.full))(?))
        .thenAnswer(i => Future.successful(i.getArgument[OfferListingResponse](0)))

      when(vosReviewClient.getRating(?, ?)(?)).thenReturn(Future.successful(ReviewsRatingResponse.getDefaultInstance))
      when(vosReviewClient.getFeatures(?, ?)(?)).thenReturn(Future.successful(FeaturesResponse.getDefaultInstance))

      val params = SearchRequestParameters.newBuilder()
      params.setWithAutoruExpert(TristateTumblerGroup.ONLY)

      val apiRequest = ApiSearchRequest(Cars, params.build())

      val response = easySearchManager
        .groupsListing(
          apiRequest,
          Paging.Default,
          Seq.empty,
          Seq.empty
        )(r)
        .futureValue

      response.getOfferGroupsCount shouldBe 10

      verify(searcherClient).searchOffers(
        ?,
        eq(Paging(1, 100)),
        eq(SortingByField("fresh_relevance_1", desc = true)),
        eq(Seq.empty),
        eq(None)
      )(eq(r), eq(BigBrotherProfile.Empty))

      verify(searcherClient, times(10)).searchOffers(
        ?,
        eq(Paging.Default),
        eq(SortingByField("fresh_relevance_1", desc = true)),
        eq(Seq.empty),
        eq(None)
      )(eq(r), eq(BigBrotherProfile.Empty))

      verifyNoMoreInteractions(searcherClient)
      reset(searcherClient)
    }
  }
}
