package ru.auto.api.routes.v1.user.review

import akka.http.scaladsl.model.StatusCodes._
import org.mockito.Mockito.verify
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.ResponseStatus.SUCCESS
import ru.auto.api.ResponseModel.{Pagination, SavedSearchResponse, SavedSearchesListing, SuccessResponse}
import ru.auto.api.SearchesModel.{EmailDelivery, PushDelivery, SavedSearchCreateParams, SearchInstance}
import ru.auto.api.managers.favorite.SavedSearchesManager
import ru.auto.api.managers.review.ReviewManager
import ru.auto.api.model.ModelGenerators.SessionResultGen
import ru.auto.api.model.Paging
import ru.auto.api.model.reviews.ReviewModelGenerators.ReviewGen
import ru.auto.api.model.reviews.ReviewModelUtils
import ru.auto.api.model.reviews.ReviewModelUtils._
import ru.auto.api.reviews.ReviewsResponseModel.{ReviewListingResponse, ReviewResponse}
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 11/07/2018.
  */

class UserReviewsHandlerTest extends ApiSpec with MockedClients {

  override lazy val reviewManager: ReviewManager = mock[ReviewManager]
  override lazy val savedSearchesManager: SavedSearchesManager = mock[SavedSearchesManager]

  private val review = ReviewGen.next
  private val reviewSuccessResponse = Future.successful(ReviewModelUtils.reviewSuccessResponse(review))
  private val paging = Paging(page = 3, pageSize = 10)

  private val pagination = Pagination
    .newBuilder()
    .setPage(paging.page)
    .setPageSize(paging.pageSize)
    .setTotalOffersCount(32)
    .setTotalPageCount(3)

  private val reviewListing = Future.successful {
    ReviewListingResponse
      .newBuilder()
      .addAllReviews(Seq(review, review).asJava)
      .setPagination(pagination)
      .build()
  }

  private val savedSearch = SearchInstance
    .newBuilder()
    .setCategory(Category.CARS)
    .setTitle("titile")
    .build()

  private val searchesListing = SavedSearchesListing
    .newBuilder()
    .addSavedSearches(savedSearch)
    .setStatus(SUCCESS)
    .build()
  private val successResponse = SuccessResponse.newBuilder().setStatus(SUCCESS).build()

  private val saveSearchCreateParams = SavedSearchCreateParams
    .newBuilder()
    .setHttpQuery("mark-model-nameplate=BMW")
    .setTitle("titile")
    .build()

  private val savedSearchResponse = SavedSearchResponse
    .newBuilder()
    .setSearch(savedSearch)
    .build()

  private val pushDelivery = PushDelivery
    .newBuilder()
    .setEnabled(true)
    .build()

  private val emailDelivery = EmailDelivery
    .newBuilder()
    .setEnabled(true)
    .build()

  "/user/reviews" should {
    "get user's review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getUserReview(?)(?)).thenReturn(reviewSuccessResponse)
      Get("/1.0/user/reviews/1") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[ReviewResponse](response)
            val gotReview = proto.getReview
            gotReview.getId shouldBe review.getId
            gotReview.getMark shouldBe review.getMark
            gotReview.getModel shouldBe review.getModel
            verify(reviewManager).getUserReview(eq("1"))(?)
          }
        }
    }

    "get user's reviews" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getUserReviews(?, ?)(?)).thenReturn(reviewListing)
      Get(s"/1.0/user/reviews/?page=${paging.page}&page_size=${paging.pageSize}") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[ReviewListingResponse](response)
            proto.getReviewsCount shouldBe 2
            proto.getReviews(0) shouldBe review
            proto.getReviews(1) shouldBe review
            proto.getStatus shouldBe SUCCESS
            proto.hasError shouldBe false
            proto.hasDetailedError shouldBe false
            verify(reviewManager).getUserReviews(eq(paging), eq(None))(?)
          }
        }
    }

  }

}
