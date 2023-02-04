package ru.auto.api.services.review

import java.lang.System.currentTimeMillis

import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.exceptions.ReviewNotFoundException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.ModelGenerators.PersonalUserRefGen
import ru.auto.api.model._
import ru.auto.api.model.reviews.AutoReviewsFilter
import ru.auto.api.model.reviews.ReviewModelGenerators.ReviewGen
import ru.auto.api.model.reviews.ReviewModelUtils._
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.ReviewUser
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

/**
  * Created by Evgeny Veretennikov <vere10@yandex-team.ru> on 12.04.2017.
  */
//noinspection TypeAnnotation
class VosReviewClientIntTest extends HttpClientSuite with Matchers with ScalaFutures {

  override protected def config: HttpClientConfig = {
    HttpClientConfig("vos2-reviews-api-test-int.slb.vertis.yandex.net", 80)
  }

  val client = new VosReviewClient(http)
  val review: Review = ReviewGen.next

  def changeReview(f: Review.Builder => Unit): Review = {
    val builder = review.toBuilder
    f(builder)
    builder.build()
  }

  val changedUserRef = UserRef.parse("user:111")
  val reviewChangedAuto: Review = changeReview(_.getItemBuilder.getAutoBuilder.setMark("VAZ"))
  val reviewChangedUser: Review = changeReview(_.getReviewerBuilder.setId(changedUserRef.toPlain))
  val user: UserRef = UserRef.parse(review.getReviewer.getId)

  val autoFilter = AutoReviewsFilter(
    review.getMark,
    review.getModel,
    None,
    review.getCategory,
    review.getSuperGenId.map(Seq(_)),
    review.getTechParamId.map(Seq(_)),
    None,
    review.getYear,
    review.getYear,
    review.getBodyType,
    review.getTransmission,
    review.getDisplacement,
    review.getDisplacement,
    review.hasPhoto
  )
  val sorting = SortingByField("date", true)
  val paging: Paging = Paging.Default
  val excludeOfferId: Option[String] = None
  var id: String = "0"
  var idChangedAuto = "2"
  var idChangedUser = "3"
  val pro = "pro"
  val startTime = currentTimeMillis()

  implicit override val trace: Traced = Traced.empty

  implicit private val request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
    r.setUser(PersonalUserRefGen.next)
    r.setTrace(trace)
    r
  }
  test("not return unexisting review anywhere") {
    client.getReview(id).failed.futureValue should not be null
    val userReviews = client.getUserReviews(user, paging, None).futureValue
    userReviews.getReviewsCount shouldBe 0
    val userPagination = userReviews.getPagination
    userPagination.getTotalPageCount shouldBe 0
    userPagination.getTotalOffersCount shouldBe 0
    val autoReviews = client.getListing(autoFilter, paging, sorting, excludeOfferId).futureValue
    autoReviews.getReviewsCount shouldBe 0
    val autoPagination = userReviews.getPagination
    autoPagination.getTotalPageCount shouldBe 0
    autoPagination.getTotalOffersCount shouldBe 0
  }

  test("not update unexisting review") {
    client.updateReview(id, review).failed.futureValue shouldBe a[ReviewNotFoundException]
  }

  test("not delete unexisting review") {
    client.deleteReview(id, user).failed.futureValue shouldBe a[ReviewNotFoundException]
  }

  test("create and get reviews") {
    id = client.createReview(review).futureValue.getReviewId
    val vosReviewResponse = client.getUserReview(user, id).futureValue
    vosReviewResponse.getReview.getItem.getAuto shouldBe review.getItem.getAuto

    idChangedAuto = client.createReview(reviewChangedAuto).futureValue.getReviewId
    idChangedUser = client.createReview(reviewChangedUser).futureValue.getReviewId

    val gotReviewResponse = client.getUserReview(user, id).futureValue
    gotReviewResponse.getReview.getMark shouldBe review.getMark
    val gotReviewChangedAuto = client.getUserReview(user, idChangedAuto).futureValue
    gotReviewChangedAuto.getReview.getMark shouldBe reviewChangedAuto.getMark
    val gotReviewChangedUser = client.getUserReview(changedUserRef, idChangedUser).futureValue
    gotReviewChangedUser.getReview.getMark shouldBe review.getMark
  }

  test("get user reviews") {
    val listing = client.getUserReviews(user, paging, None).futureValue
    val reviews = listing.getReviewsList.asScala.map(r => r.getId)
    reviews should contain(id)
    reviews should contain(idChangedAuto)
    val pagination = listing.getPagination
    pagination.getPage shouldBe 1
    pagination.getPageSize shouldBe 10
    pagination.getTotalPageCount shouldBe 1
    pagination.getTotalOffersCount shouldBe 2
  }

  test("get listing") {
    val listing = client.getListing(autoFilter, paging, sorting, excludeOfferId).futureValue
    val pagination = listing.getPagination
    pagination.getPage shouldBe 1
    pagination.getPageSize shouldBe 10
  }

  test("update review") {
    val vosReviewResponse = client.getUserReview(user, id).futureValue
    val reviewer = ReviewUser.newBuilder().setId(review.getReviewer.getId).build()
    val updatedReview = vosReviewResponse.getReview.toBuilder.addPro(pro).setId(id).setReviewer(reviewer).build()
    client.updateReview(id, updatedReview).futureValue.getReviewId shouldBe id
  }

  test("get updated review") {
    val gotReviewResponse = client.getUserReview(user, id).futureValue
    val gotReview = gotReviewResponse.getReview
    gotReview.getId shouldBe id
    gotReview.getMark shouldBe review.getMark
    gotReview.getPro(0) shouldBe pro
  }

  test("delete reviews") {
    client.deleteReview(id, user).futureValue.getReviewId shouldBe id
    client.deleteReview(idChangedAuto, user).futureValue.getReviewId shouldBe idChangedAuto
    client.deleteReview(idChangedUser, changedUserRef).futureValue.getReviewId shouldBe idChangedUser
  }

  test("not return deleted reviews anywhere") {
    client.getReview(id).failed.futureValue should not be null
    client.getUserReviews(user, paging, None).futureValue.getReviewsCount shouldBe 0
    client.getListing(autoFilter, paging, sorting, excludeOfferId).futureValue.getReviewsCount shouldBe 0
  }
}
