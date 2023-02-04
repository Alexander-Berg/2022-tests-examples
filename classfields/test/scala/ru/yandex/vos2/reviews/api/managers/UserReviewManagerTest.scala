package ru.yandex.vos2.reviews.api.managers

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSuite, Matchers}
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.LogbookEntry
import ru.yandex.vos2.api.model.Paging
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.utils.InitReviewTestDb
import ru.yandex.vos2.util.log.Logging

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-01-17.
  */

@RunWith(classOf[JUnitRunner])
class UserReviewManagerTest
  extends FunSuite with Matchers with InitReviewTestDb with Logging with BaseReviewsTest with ScalaFutures {

  implicit private val patience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(15, Millis))

  initReviewsDbs()

  private val userManager = new UserReviewsManager(offerDao)
  private val crudManager = new ReviewCrudManager(offerDao, userDao, features, reviewValidator)

  private val oldUserRef = UserRef.from("a_1")
  private val newUserRef = UserRef.from("a_2")

  test("move review to another user") {

    val response = crudManager.create(createOffer1.getReview).futureValue

    userManager.getUserReview(oldUserRef, response.getReviewId).futureValue
      .getReview.getId shouldBe response.getReviewId

    userManager.moveReviewToUser(response.getReviewId, oldUserRef, newUserRef).futureValue

    userManager.getUserReview(newUserRef, response.getReviewId).futureValue
      .getReview.getId shouldBe response.getReviewId
  }

  test("filter entries") {

    val logbookEntry = createOffer1.getReview.toBuilder
        .setKind(Review.Kind.LOGBOOK)
        .setLogbookEntry(LogbookEntry.newBuilder()
          .setKind(LogbookEntry.Kind.MAINTENANCE)
          .setCost(10000)
          .setMileage(120000)
          .build())
      .build()

    crudManager.create(createOffer1.getReview).futureValue
    crudManager.create(logbookEntry).futureValue

    val response1 = userManager
      .getUserReviews(oldUserRef, Paging(), None, includeRemoved = false, Set(Review.Kind.REVIEW, Review.Kind.LOGBOOK))
      .futureValue
    val response2 = userManager
      .getUserReviews(oldUserRef, Paging(), None, includeRemoved = false, Set(Review.Kind.LOGBOOK))
      .futureValue

    response1.getReviewsList.size() shouldBe 2
    response2.getReviewsList.size() shouldBe 1
    response2.getPagination.getTotalOffersCount shouldBe 1
    response2.getReviewsList.get(0).getKind shouldBe Review.Kind.LOGBOOK
  }

}
