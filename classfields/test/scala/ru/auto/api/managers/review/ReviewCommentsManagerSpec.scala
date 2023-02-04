package ru.auto.api.managers.review

import com.google.protobuf.util.Timestamps.fromSeconds
import org.mockito.Mockito.verify
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.Pagination
import ru.auto.api.ResponseModel.ResponseStatus.SUCCESS
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.model.ModelGenerators.PersonalUserRefGen
import ru.auto.api.model.comments.CommentsModelUtils._
import ru.auto.api.model.comments.TopicGroup._
import ru.auto.api.model.{Paging, RequestParams}
import ru.auto.api.reviews.ReviewModel.Review.ReviewUser.Type
import ru.auto.api.reviews.ReviewModel.Review.{Comment, ReviewUser, Status}
import ru.auto.api.reviews.ReviewsRequestModel.AddCommentRequest
import ru.auto.api.reviews.ReviewsResponseModel.{AddCommentResponse, CommentListingResponse}
import ru.auto.api.services.passport.util.UserProfileStubsProvider
import ru.auto.api.services.phpapi.PhpApiClient
import ru.auto.api.services.phpapi.model.{CommentUser, PhpComment, PhpReviewComments}
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 30/10/2017.
  */
class ReviewCommentsManagerSpec extends BaseSpec with MockitoSupport {

  val phpClient: PhpApiClient = mock[PhpApiClient]
  val statEventsManager: StatEventsManager = mock[StatEventsManager]
  val userProfileStubs: UserProfileStubsProvider = UserProfileStubsProvider.Empty

  val featureManager: FeatureManager = mock[FeatureManager]
  val brokerEventsManager: ReviewBrokerEventsManager = mock[ReviewBrokerEventsManager]

  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.reviewsNewDeliveryEnabled).thenReturn(feature)

  val reviewCommentManager: CommentsManager =
    new CommentsManager(statEventsManager, phpClient, userProfileStubs, featureManager, brokerEventsManager)
  val paging: Paging = Paging.Default
  val imageUrl = "//images.mds-proxy.dev.autoru.yandex.net/get-autoru-users/45565/84a6a0eab8db4cef6f062ddb6a58db7b"

  val images: Map[String, String] = CommentsManager.defaultAvatarSizes
    .map(size => size -> s"$imageUrl/$size")
    .toMap

  implicit private val trace: Traced = Traced.empty

  implicit private val requestImpl: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some("testUid")))
    r.setUser(PersonalUserRefGen.next)
    r.setTrace(trace)
    r.setNewSession("ffrfrrrfrf")
    r
  }

  val fullName = "vasya pupkin"

  val phpAuthor = CommentUser(
    "1",
    fullName,
    "nick",
    "http://images.mds-proxy.dev.autoru.yandex.net/get-autoru-users/45565/84a6a0eab8db4cef6f062ddb6a58db7b/24x24"
  )
  val phpComment = PhpComment("1", "1", "message", "12345678", phpAuthor, "0")
  val phpResponse = PhpReviewComments(4, List(phpComment), 4)

  val author: ReviewUser = ReviewUser
    .newBuilder()
    .setId("1")
    .setLogin(fullName)
    .setType(Type.CUSTOMER)
    .putAllAvatarUrl(images.asJava)
    .build()

  val comments: Comment = Comment
    .newBuilder()
    .setId("1")
    .setAuthor(author)
    .setMessage("message")
    .setStatus(Status.ENABLED)
    .setTimestamp(fromSeconds(12345678L))
    .build()

  val pagination: Pagination = Pagination
    .newBuilder()
    .setPage(1)
    .setTotalPageCount(1)
    .setPageSize(10)
    .setTotalOffersCount(1)
    .build()

  "CommentsReviewManager" should {
    "get comments" in {

      val response = CommentListingResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .addReviewComments(comments)
        .setPagination(pagination)
        .build()

      when(phpClient.getComments(?, ?, ?)(?)).thenReturnF(phpResponse)
      val managerResponse: CommentListingResponse =
        reviewCommentManager.getComments(REVIEWS, "1", paging).futureValue.toReviewResponse

      managerResponse shouldBe response

      verify(phpClient).getComments(?, ?, ?)(?)
    }

    "post comments" in {
      val response = AddCommentResponse
        .newBuilder()
        .setStatus(SUCCESS)
        .setComment(comments)
        .build()

      val request = AddCommentRequest
        .newBuilder()
        .setMessage("message")
        .build()

      when(phpClient.addComment(?, ?, ?, ?, ?)(?)).thenReturnF(phpComment)
      when(statEventsManager.logCommentEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      val managerResponse = reviewCommentManager.addComment(REVIEWS, "1", request).futureValue
      managerResponse shouldBe response
      verify(phpClient).addComment(?, ?, ?, ?, ?)(?)
      verify(statEventsManager).logCommentEvent(?, ?, ?, ?)(?)
    }

    "complain on comment" in {

      when(phpClient.complainOnComment(?, ?, ?)(?)).thenReturnF(())

      reviewCommentManager.complainOnComment("11", "complaint").futureValue

      verify(phpClient).complainOnComment(?, ?, ?)(?)
    }
  }

}
