package ru.auto.api.routes.v1.review

import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}
import org.mockito.Mockito._
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.ErrorCode.REVIEW_NOT_FOUND
import ru.auto.api.ResponseModel.ResponseStatus.{ERROR, SUCCESS}
import ru.auto.api.ResponseModel._
import ru.auto.api.exceptions.ReviewNotFoundException
import ru.auto.api.managers.review.{CommentsManager, ReviewManager}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.reviews.ReviewModelGenerators._
import ru.auto.api.model.reviews.ReviewModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.reviews.{AutoReviewsFilter, ReviewModelUtils}
import ru.auto.api.reviews.ReviewModel.Review.{Comment, ReviewUser}
import ru.auto.api.reviews.ReviewsRequestModel.AddCommentRequest
import ru.auto.api.reviews.ReviewsResponseModel._
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by Evgeny Veretennikov <vere10@yandex-team.ru> on 13.04.2017.
  */
//noinspection TypeAnnotation
class ReviewHandlerSpec extends ApiSpec with MockedClients {
  override lazy val reviewManager = mock[ReviewManager]
  override lazy val commentManager: CommentsManager = mock[CommentsManager]

  val user = PrivateUserRefGen.next
  val review = ReviewGen.next
  val reviewSaveSuccessResponse = Future.successful(ReviewSaveResponse.newBuilder().setReviewId("1").build())
  val reviewDeleteSuccessResponse = Future.successful(ReviewDeleteResponse.newBuilder().setReviewId("1").build())
  val reviewSuccessResponse = Future.successful(ReviewModelUtils.reviewSuccessResponse(review))
  val futureNotFound = Future.failed(new ReviewNotFoundException)
  val paging = Paging(page = 3, pageSize = 10)
  val excludeOfferId: Option[String] = None
  val sortBy = SortingByField("updateDate", true)
  val counter = OfferCountResponse.newBuilder().setCount(10).setStatus(SUCCESS).build()
  val rating = ReviewsRatingResponse.newBuilder().setStatus(SUCCESS).build()

  val pagination = Pagination
    .newBuilder()
    .setPage(paging.page)
    .setPageSize(paging.pageSize)
    .setTotalOffersCount(32)
    .setTotalPageCount(3)

  val reviewListing = Future.successful {
    ReviewListingResponse
      .newBuilder()
      .addAllReviews(Seq(review, review).asJava)
      .setPagination(pagination)
      .build()
  }

  val autoFilter = AutoReviewsFilter(
    review.getMark,
    review.getModel,
    None,
    review.getCategory,
    review.getSuperGenId.map(Seq(_)),
    review.getTechParamId.map(Seq(_)),
    None
  )

  val markFilter = AutoReviewsFilter(
    review.getMark,
    None,
    None,
    review.getCategory,
    None,
    None,
    None
  )

  implicit val trace: Traced = Traced.empty

  before {
    reset(reviewManager, passportClient)
  }

  "/reviews" should {
    "create review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.createReview(?)(?)).thenReturn(reviewSaveSuccessResponse)
      Post("/1.0/reviews/auto", review) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[ReviewSaveResponse](response)
            proto.getReviewId shouldBe "1"
            proto.getStatus shouldBe SUCCESS
            proto.hasError shouldBe false
            proto.hasDetailedError shouldBe false
            verify(reviewManager).createReview(eq(review))(?)
          }
        }
    }

    "get review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getReview(?)(?)).thenReturn(reviewSuccessResponse)
      Get("/1.0/reviews/auto/1") ~>
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
            verify(reviewManager).getReview(eq("1"))(?)
          }
        }
    }

    "return 404 on get review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getReview(?)(?)).thenReturn(futureNotFound)
      Get("/1.0/reviews/auto/1") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe NotFound
            val proto = Protobuf.fromJson[ErrorResponse](response)
            proto.getStatus shouldBe ERROR
            proto.getError shouldBe REVIEW_NOT_FOUND
            verify(reviewManager).getReview(eq("1"))(?)
          }
        }
    }

    "update review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.updateReview(?, ?)(?)).thenReturn(reviewSaveSuccessResponse)
      Put("/1.0/reviews/auto/1", review) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[ReviewSaveResponse](response)
            proto.getReviewId shouldBe "1"
            proto.getStatus shouldBe SUCCESS
            proto.hasError shouldBe false
            proto.hasDetailedError shouldBe false
            verify(reviewManager).updateReview(eq("1"), eq(review))(?)
          }
        }
    }

    "return 404 on update review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.updateReview(?, ?)(?)).thenReturn(futureNotFound)
      Put("/1.0/reviews/auto/1", review) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe NotFound
            val proto = Protobuf.fromJson[ErrorResponse](response)
            proto.getStatus shouldBe ERROR
            proto.getError shouldBe REVIEW_NOT_FOUND
            verify(reviewManager).updateReview(eq("1"), eq(review))(?)
          }
        }
    }

    "delete review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.deleteReview(?)(?)).thenReturn(reviewDeleteSuccessResponse)
      Delete("/1.0/reviews/auto/1") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[ReviewSaveResponse](response)
            proto.getReviewId shouldBe "1"
            proto.getStatus shouldBe SUCCESS
            proto.hasError shouldBe false
            proto.hasDetailedError shouldBe false
            verify(reviewManager).deleteReview(eq("1"))(?)
          }
        }
    }

    "return 404 in delete review" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.deleteReview(?)(?)).thenReturn(futureNotFound)
      Delete("/1.0/reviews/auto/1") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe NotFound
            val proto = Protobuf.fromJson[ErrorResponse](response)
            proto.getStatus shouldBe ERROR
            proto.getError shouldBe REVIEW_NOT_FOUND
            verify(reviewManager).deleteReview(eq("1"))(?)
          }
        }
    }

    "get user reviews" in {
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

    "get auto reviews for full filter" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getListing(?, ?, ?, ?, ?)(?)).thenReturn(reviewListing)
      Get(
        s"/1.0/reviews/auto/listing?category=${review.getCategory.get}&mark=${review.getMark.get}" +
          s"&model=${review.getModel.get}&page=${paging.page}&page_size=${paging.pageSize}" +
          s"&super_gen=${review.getSuperGenId.get}&tech_param_id=${review.getTechParamId.get}&sort=updateDate-desc"
      ) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          verify(reviewManager).getListing(eq(autoFilter), eq(paging), eq(sortBy), eq(excludeOfferId), eq(None))(?)
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
          }
        }
    }

    "get auto reviews for mark filter" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getListing(?, ?, ?, ?, ?)(?)).thenReturn(reviewListing)
      Get(
        s"/1.0/reviews/auto/listing?category=${review.getCategory.get}&mark=${review.getMark.get}" +
          s"&page=${paging.page}&page_size=${paging.pageSize}&sort=updateDate-desc"
      ) ~>
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
            verify(reviewManager).getListing(eq(markFilter), eq(paging), eq(sortBy), eq(excludeOfferId), eq(None))(?)
          }
        }
    }

    "return 400 for wrong reviews filter" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getListing(?, ?, ?, ?, ?)(?)).thenReturn(reviewListing)
      Get(
        s"/1.0/reviews/auto/listing?category=${review.getCategory.get}&mark=${review.getMark.get}" +
          s"&page=${paging.page}&page_size=${paging.pageSize}&sort=updateDate-desc&super_gen=VFR"
      ) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe BadRequest
            verifyNoMoreInteractions(reviewManager)
          }
        }
    }

    "get presets" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getPopular(?)(?)).thenReturn(reviewListing)
      when(reviewManager.getTopLike(?)(?)).thenReturn(reviewListing)
      when(reviewManager.getWeekly(?)(?)).thenReturn(reviewListing)
      when(reviewManager.getRecent(?)(?)).thenReturn(reviewListing)

      Get(s"/1.0/reviews/auto/presets/cars/popular") ~>
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
            verify(reviewManager).getPopular(?)(?)
          }
        }

      Get(s"/1.0/reviews/auto/presets/cars/top-like") ~>
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
            verify(reviewManager).getTopLike(?)(?)
          }
        }

      Get(s"/1.0/reviews/auto/presets/cars/recent") ~>
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
            verify(reviewManager).getRecent(?)(?)
          }
        }

      Get(s"/1.0/reviews/auto/presets/cars/weekly?category") ~>
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
            verify(reviewManager).getWeekly(?)(?)
          }
        }
    }

    "get aggregators" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getCounter(?, ?, ?)(?)).thenReturnF(counter)
      when(reviewManager.getRating(?, ?)(?)).thenReturnF(rating)
      Get(s"/1.0/reviews/auto/cars/rating?&mark=BMW&model=X5") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[OfferCountResponse](response)
            proto.getStatus shouldBe SUCCESS

            verify(reviewManager).getRating(?, ?)(?)
          }
        }
    }

    "get count" in {
      val mmg = MarkModelNameplateGeneration(Some("BMW"), None, None, None)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getCounter(?, ?, ?)(?)).thenReturnF(counter)
      when(reviewManager.getRating(?, ?)(?)).thenReturnF(rating)
      Get(s"/1.0/reviews/auto/cars/counter?&mark=BMW&model=") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[OfferCountResponse](response)
            proto.getStatus shouldBe SUCCESS

            verify(reviewManager).getCounter(?, eq(mmg), ?)(?)
          }
        }
    }

    "get current draft" in {
      val response = ReviewResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .setReview(review)
        .build()

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getCurrentDraft(?)).thenReturnF(response)
      Get("/1.0/reviews/auto/draft") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            val proto = Protobuf.fromJson[ReviewResponse](response)
            proto.getReview shouldBe review
            proto.getStatus shouldBe SUCCESS

            verify(reviewManager).getCurrentDraft(?)
          }
        }
    }

    "get comments" in {
      val author = ReviewUser.newBuilder().setId("1").setLogin("login").build()
      val comments = Comment.newBuilder().setAuthor(author).setMessage("message1").build()
      val response = CommentListingResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .addReviewComments(comments)
        .build()

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(commentManager.getReviewComments(?, ?)(?)).thenReturnF(response)
      Get("/1.0/reviews/auto/1/comments") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[CommentListingResponse]
          withClue(response) {
            status shouldBe OK
            response.getReviewCommentsList.asScala.head shouldBe comments
            response.getStatus shouldBe SUCCESS

            verify(commentManager).getReviewComments(?, ?)(?)
          }
        }
    }

    "post comments" in {
      val author = ReviewUser.newBuilder().setId("1").setLogin("login").build()
      val comments = Comment.newBuilder().setAuthor(author).setMessage("message1").build()
      val response = AddCommentResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .setComment(comments)
        .build()

      val request = AddCommentRequest
        .newBuilder()
        .setMessage("message")
        .build()

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(commentManager.addComment(?, ?, ?)(?)).thenReturnF(response)
      Post("/1.0/reviews/auto/1/comments", request) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[AddCommentResponse]
          withClue(response) {
            status shouldBe OK
            response.getComment shouldBe comments
            response.getStatus shouldBe SUCCESS

            verify(commentManager).addComment(?, ?, ?)(?)
          }
        }
    }

    "complain on comment" in {
      val response = SuccessResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .build()

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(commentManager.complainOnComment(?, ?)(?)).thenReturnF(response)
      Post("/1.0/reviews/auto/1/comments/123/complain?message=test") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[SuccessResponse]
          withClue(response) {
            status shouldBe OK
            response.getStatus shouldBe SUCCESS

            verify(commentManager).complainOnComment(?, ?)(?)
          }
        }
    }

    "get features snippet" in {
      val response = FeatureSnippetResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .build()

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getFeatureSnippet(?, ?, ?)(?)).thenReturnF(response)
      Get("/1.0/reviews/auto/features/CARS/snippet?mark=VAZ&model=2107&super_gen=2307270&feature=assembly_quality") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[FeatureSnippetResponse]
          withClue(response) {
            status shouldBe OK
            response.getStatus shouldBe SUCCESS

            verify(reviewManager).getFeatureSnippet(?, ?, ?)(?)
          }
        }
    }

    "get features" in {
      val response = FeaturesResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .build()

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(reviewManager.getFeatures(?, ?)(?)).thenReturnF(response)
      Get("/1.0/reviews/auto/features/CARS?mark=BMW&model=X5&super_gen=123") ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[FeaturesResponse]
          withClue(response) {
            status shouldBe OK
            response.getStatus shouldBe SUCCESS

            verify(reviewManager).getFeatures(?, ?)(?)
          }
        }
    }

  }
}
