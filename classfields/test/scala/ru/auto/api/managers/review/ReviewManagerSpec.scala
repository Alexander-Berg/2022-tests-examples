package ru.auto.api.managers.review

import org.mockito.Mockito
import org.mockito.Mockito.{reset, times, verify, verifyNoMoreInteractions}
import ru.auto.api.BaseSpec
import ru.auto.api.CatalogModel.TechParam
import ru.auto.api.ResponseModel.ResponseStatus.SUCCESS
import ru.auto.api.ResponseModel._
import ru.auto.api.exceptions.ReviewNotFoundException
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.enrich.{ReviewsEnrichManager, ReviewsEnrichOptions}
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model._
import ru.auto.api.model.events.reviews.ReviewCreateUpdateTskvEvent
import ru.auto.api.model.gen.DateTimeGenerators.timestampInPast
import ru.auto.api.model.reviews.AutoReviewsFilter
import ru.auto.api.model.reviews.ReviewModelGenerators._
import ru.auto.api.model.reviews.ReviewModelUtils._
import ru.auto.api.reviews.ReviewModel.Review.Opinion
import ru.auto.api.reviews.ReviewModel.ReviewsAvailableFilters
import ru.auto.api.reviews.ReviewsResponseModel._
import ru.auto.api.services.mds.PrivateMdsClient
import ru.auto.api.services.phpapi.PhpApiClient
import ru.auto.api.services.review.{ReviewsSearcherClient, VosReviewClient}
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by Evgeny Veretennikov <vere10@yandex-team.ru> on 13.04.2017.
  */
//noinspection TypeAnnotation
class ReviewManagerSpec extends BaseSpec with MockitoSupport {
  val vosReviewClient = mock[VosReviewClient]
  val reviewSearcherClient = mock[ReviewsSearcherClient]
  val uploaderClient = mock[UploaderClient]
  val phpClient = mock[PhpApiClient]
  val enrichManager = mock[ReviewsEnrichManager]
  val statEventManager = mock[StatEventsManager]
  val offerLoader = mock[EnrichedOfferLoader]
  val mdsClient = mock[PrivateMdsClient]
  val selfAddress = "http://localhost:2600"
  val mdsHost = "avatars.mdst.yandex.net"
  val optImageTtl = None

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.reviewsNewDeliveryEnabled).thenReturn(feature)

  val brokerEventsManager: ReviewBrokerEventsManager = mock[ReviewBrokerEventsManager]

  val manager = new ReviewManager(
    statEventManager,
    reviewSearcherClient,
    phpClient,
    vosReviewClient,
    uploaderClient,
    offerLoader,
    mdsClient,
    enrichManager,
    selfAddress,
    mdsHost,
    optImageTtl,
    brokerEventsManager,
    featureManager
  )
  val review = ReviewGen.next.toBuilder.setId("1").setDtreviewed(timestampInPast.next).build()
  val reviewResponse = ReviewResponse.newBuilder().setReview(review).setStatus(SUCCESS).build()

  val listing = ReviewListingResponse
    .newBuilder()
    .addReviews(review)
    .addReviews(review)
    .setReviewsAvailableFilters(
      ReviewsAvailableFilters.newBuilder
        .addAllBodyTypes(List("SEDAN").asJava)
        .addAllTransmissions(List("MECHANICAL").asJava)
        .addAllYears(List(Integer.valueOf(2015)).asJava)
        .addAllTechParams(List(TechParam.newBuilder.setId(20550768L).build).asJava)
    )
    .setPagination(
      Pagination
        .newBuilder()
        .setPage(3)
        .setPageSize(10)
        .setTotalPageCount(3)
        .setTotalOffersCount(32)
    )
    .build()
  val user = AutoruUser(123)
  val paging = mock[Paging]
  val autoFilter = mock[AutoReviewsFilter]
  val excludeOfferId: Option[String] = None
  val sorting = SortingByField("date", desc = true)

  implicit private val trace = Traced.empty

  implicit private val request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
    r.setUser(user)
    r.setTrace(trace)
    r
  }

  before {
    reset(vosReviewClient, reviewSearcherClient, enrichManager)
  }

  after {
    verifyNoMoreInteractions(enrichManager)
  }

  "ReviewManager" should {
    "create review" in {
      val reviewResponse = ReviewSaveResponse.newBuilder().setReviewId("1").build()
      when(vosReviewClient.createReview(?)(?)).thenReturnF(reviewResponse)
      when(statEventManager.logReviewCreateUpdateEvent(?, ?)(?)).thenReturn(Future.unit)
      val response = manager.createReview(review).futureValue
      verify(vosReviewClient).createReview(?)(?)
      verify(statEventManager).logReviewCreateUpdateEvent(eq(review), eq(ReviewCreateUpdateTskvEvent.Create))(?)
      response.getReviewId shouldBe "1"
      response.getStatus shouldBe SUCCESS
      response.hasError shouldBe false
      response.hasDetailedError shouldBe false
    }

    "get review" in {
      val option = ReviewsEnrichOptions.ForSingleReview
      when(reviewSearcherClient.getReview(?)(?)).thenReturn(Future(reviewResponse))
      when(enrichManager.enrichReviews(eq(Seq(review)), eq(option))(?)).thenReturn(Future(Seq(review)))
      when(statEventManager.logReviewViewEvent(?)(?)).thenReturn(Future.unit)
      val response = manager.getReview("1").futureValue
      verify(reviewSearcherClient).getReview(?)(?)
      verify(enrichManager).enrichReviews(eq(Seq(review)), eq(option))(?)
      verifyNoMoreInteractions(vosReviewClient)
      val gotReview = response.getReview
      verify(statEventManager).logReviewViewEvent(eq(gotReview))(?)
      gotReview.getId shouldBe "1"
      gotReview.getDtreviewed shouldBe review.getDtreviewed
      gotReview.getMark shouldBe review.getMark
      response.getStatus shouldBe SUCCESS
      response.hasError shouldBe false
      response.hasDetailedError shouldBe false
    }

    "re-throw review not found in get" in {
      val e = new ReviewNotFoundException
      when(reviewSearcherClient.getReview(?)(?)).thenReturn(Future.failed(e))
      when(vosReviewClient.getReview(?)(?)).thenReturn(Future.failed(e))
      val response = manager.getReview("1").failed.futureValue
      verify(reviewSearcherClient).getReview(?)(?)
      verify(vosReviewClient).getReview(?)(?)
      response shouldBe a[ReviewNotFoundException]
    }

    "update review" in {
      val reviewResponse = ReviewSaveResponse.newBuilder().setReviewId("1").build()
      when(vosReviewClient.updateReview(?, ?)(?)).thenReturnF(reviewResponse)
      val response = manager.updateReview("1", review).futureValue
      verify(vosReviewClient).updateReview(?, ?)(?)
      verifyNoMoreInteractions(vosReviewClient)
      response.getReviewId shouldBe "1"
      response.getStatus shouldBe SUCCESS
      response.hasError shouldBe false
      response.hasDetailedError shouldBe false
    }

    "try to move review and update if not found" in {
      val e = new ReviewNotFoundException
      when(vosReviewClient.updateReview(?, ?)(?)).thenReturn(Future.failed(e))
      when(vosReviewClient.moveReviewToUser(?, ?, ?)(?)).thenReturnF(())
      val response = manager.updateReview("1", review).failed.futureValue
      verify(vosReviewClient, Mockito.times(2)).updateReview(?, ?)(?)
      verify(vosReviewClient).moveReviewToUser(?, ?, ?)(?)
      verifyNoMoreInteractions(vosReviewClient)
      response shouldBe a[ReviewNotFoundException]
    }

    "delete review" in {
      val reviewResponse = ReviewDeleteResponse.newBuilder().setReviewId("1").build()
      when(vosReviewClient.deleteReview(?, ?)(?)).thenReturnF(reviewResponse)
      when(statEventManager.logReviewDeleteEvent(?)(?)).thenReturnF(())
      val response = manager.deleteReview("1").futureValue
      verify(vosReviewClient).deleteReview(?, ?)(?)
      verify(statEventManager).logReviewDeleteEvent(eq("1"))(?)
      response.getReviewId shouldBe "1"
      response.getStatus shouldBe SUCCESS
    }

    "re-throw review not found in delete" in {
      val e = new ReviewNotFoundException
      when(vosReviewClient.deleteReview(?, ?)(?)).thenReturn(Future.failed(e))
      val response = manager.deleteReview("1").failed.futureValue
      verify(vosReviewClient).deleteReview(?, ?)(?)
      verifyNoMoreInteractions(vosReviewClient)
      response shouldBe a[ReviewNotFoundException]
    }

    "get user reviews" in {
      val option = ReviewsEnrichOptions.ForPersonalCabinet
      when(vosReviewClient.getUserReviews(?, ?, ?)(?)).thenReturn(Future(listing))
      when(enrichManager.enrichReviews(eq(listing.getReviewsList.asScala.toSeq), eq(option))(?))
        .thenReturn(Future(listing.getReviewsList.asScala.toSeq))
      when(enrichManager.enrichAvailableParams(eq(listing.getReviewsAvailableFilters), eq(option))(?))
        .thenReturn(Future(listing.getReviewsAvailableFilters))
      val response = manager.getUserReviews(paging, None).futureValue
      verify(enrichManager).enrichReviews(?, ?)(?)
      verify(enrichManager).enrichAvailableParams(?, ?)(?)
      verify(vosReviewClient).getUserReviews(?, ?, ?)(?)
      verifyNoMoreInteractions(vosReviewClient)
      response.getReviewsCount shouldBe 2
      val pagination = response.getPagination
      pagination.getPage shouldBe 3
      pagination.getPageSize shouldBe 10
      pagination.getTotalOffersCount shouldBe 32
      pagination.getTotalPageCount shouldBe 3
    }

    "get reviews listing" in {
      val option = ReviewsEnrichOptions.ForReviewListing
      when(reviewSearcherClient.getReviewsListing(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future(listing))
      when(enrichManager.enrichReviews(eq(listing.getReviewsList.asScala.toSeq), eq(option))(?))
        .thenReturn(Future(listing.getReviewsList.asScala.toSeq))
      when(statEventManager.logReviewListingEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(enrichManager.enrichAvailableParams(eq(listing.getReviewsAvailableFilters), eq(option))(?))
        .thenReturn(Future(listing.getReviewsAvailableFilters))
      val response = manager.getListing(autoFilter, paging, sorting, excludeOfferId).futureValue
      verify(reviewSearcherClient, times(1)).getReviewsListing(?, ?, ?, ?, ?, ?)(?)
      verify(enrichManager).enrichReviews(eq(listing.getReviewsList.asScala.toSeq), eq(option))(?)
      verify(enrichManager).enrichAvailableParams(eq(listing.getReviewsAvailableFilters), eq(option))(?)
      verify(statEventManager).logReviewListingEvent(?, ?, ?, ?)(?)
      verifyNoMoreInteractions(vosReviewClient)
      response.getReviewsCount shouldBe 2
      val pagination = response.getPagination
      pagination.getPage shouldBe 3
      pagination.getPageSize shouldBe 10
      pagination.getTotalOffersCount shouldBe 32
      pagination.getTotalPageCount shouldBe 3
    }

    "get enriched review" in {
      val option = ReviewsEnrichOptions.ForSingleReview
      when(reviewSearcherClient.getReview(?)(?)).thenReturn(Future(reviewResponse))
      val enrichedAuto = review.getItem.getAuto.toBuilder.setMarkName("mark").setModelName("model").build()
      val enrichedItem = review.getItem.toBuilder.setAuto(enrichedAuto).build()
      val enrichedReview = review.toBuilder.setItem(enrichedItem).build()
      when(enrichManager.enrichReviews(eq(Seq(review)), eq(option))(?)).thenReturn(Future(Seq(enrichedReview)))
      val response = manager.getReview("1").futureValue
      verify(reviewSearcherClient).getReview(?)(?)
      verify(enrichManager).enrichReviews(eq(Seq(review)), eq(option))(?)
      verifyNoMoreInteractions(vosReviewClient)
      val gotReview = response.getReview
      gotReview.getId shouldBe "1"
      gotReview.getDtreviewed shouldBe review.getDtreviewed
      gotReview.getMark shouldBe review.getMark
      gotReview.getItem.getAuto.getMarkName shouldBe enrichedAuto.getMarkName
      gotReview.getItem.getAuto.getModelName shouldBe enrichedAuto.getModelName
      response.getStatus shouldBe SUCCESS
      response.hasError shouldBe false
      response.hasDetailedError shouldBe false
    }

    "get enriched reviews listing" in {
      val option = ReviewsEnrichOptions.ForReviewListing
      when(reviewSearcherClient.getReviewsListing(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future(listing))
      val enrichedAuto = review.getItem.getAuto.toBuilder.setMarkName("mark").setModelName("model").build()
      val enrichedItem = review.getItem.toBuilder.setAuto(enrichedAuto).build()
      val enrichedReview = review.toBuilder.setItem(enrichedItem).build()
      val enrichedListing = listing.toBuilder
        .clearReviews()
        .addReviews(enrichedReview)
        .addReviews(enrichedReview)
        .build()
      when(enrichManager.enrichReviews(eq(listing.getReviewsList.asScala.toSeq), eq(option))(?))
        .thenReturn(Future(enrichedListing.getReviewsList.asScala.toSeq))
      when(enrichManager.enrichAvailableParams(eq(listing.getReviewsAvailableFilters), eq(option))(?))
        .thenReturn(Future(listing.getReviewsAvailableFilters))
      val response = manager.getListing(autoFilter, paging, sorting, excludeOfferId).futureValue
      verify(reviewSearcherClient, times(1)).getReviewsListing(?, ?, ?, ?, ?, ?)(?)
      verify(enrichManager).enrichReviews(eq(listing.getReviewsList.asScala.toSeq), eq(option))(?)
      verify(enrichManager).enrichAvailableParams(eq(listing.getReviewsAvailableFilters), eq(option))(?)
      verifyNoMoreInteractions(vosReviewClient)
      response.getReviewsCount shouldBe 2
      response.getReviews(0).getItem.getAuto.getMarkName shouldBe enrichedAuto.getMarkName
      response.getReviews(0).getItem.getAuto.getModelName shouldBe enrichedAuto.getModelName
      val pagination = response.getPagination
      pagination.getPage shouldBe 3
      pagination.getPageSize shouldBe 10
      pagination.getTotalOffersCount shouldBe 32
      pagination.getTotalPageCount shouldBe 3
    }

    "set opinion" in {
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      when(vosReviewClient.setOpinion(?, ?, ?)(?)).thenReturn(Future.unit)
      when(statEventManager.logReviewOpinionEvent(?, ?)(?)).thenReturn(Future.unit)
      when(reviewSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      manager.setOpinion(review.getId, user, Opinion.LIKE)
      verify(statEventManager).logReviewOpinionEvent(?, ?)(?)
    }
  }
}
