package ru.yandex.vos2.reviews.api.handlers

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.scalacheck.Gen
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.{Item, ReviewUser}
import ru.auto.api.reviews.ReviewModel.Review.Item.Auto
import ru.auto.api.reviews.ReviewsResponseModel.{MigratedReviewInfoResponse, ReviewResponse, ReviewSaveResponse}
import ru.yandex.vos2.reviews.model.{MigrationOpinion, MigrationOpinionList}
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf
import spray.json._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 05/10/2017.
  */
class MigrationHandlerTest extends BaseReviewHandlerTest with SprayJsonSupport {

  initReviewsDbs()

  test("migrate review") {
    val reviewer = ReviewUser.newBuilder().setId("user:1234").build()
    val auto = Auto.newBuilder().setMark("BMW").setModel("X5").build()
    val id = Gen.posNum[Int].sample
    val item = Item.newBuilder().setAuto(auto)
    val review = Review.newBuilder()
      .setMigrationId(123)
      .setId(id.toString)
      .setItem(item)
      .setReviewer(reviewer)
      .build()
    var saveReview: ReviewSaveResponse = null
    val migrateReq = Post("/api/v1/reviews/migration", review)
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(review)))
    migrateReq ~> route ~> check {
      withClue(responseAs[ReviewSaveResponse]) {
        status shouldBe StatusCodes.OK
        saveReview = responseAs[ReviewSaveResponse]
      }
    }
    val reviewReq = Get(s"/api/v1/reviews/offers/${saveReview.getReviewId}")
    reviewReq ~> route ~> check {
      withClue(responseAs[ReviewResponse]) {
        status shouldBe StatusCodes.OK
        val reviewRes = responseAs[ReviewResponse]
        reviewRes.getReview.getMigrationId shouldBe review.getMigrationId
        reviewRes.getReview.getItem shouldBe review.getItem
      }
    }
    val migrationInfoReq = Get(s"/api/v1/reviews/migration/${review.getMigrationId}")
    migrationInfoReq ~> route ~> check {
      withClue(responseAs[MigratedReviewInfoResponse]) {
        status shouldBe StatusCodes.OK
        val migrationInfoRes = responseAs[MigratedReviewInfoResponse]
        migrationInfoRes.getMark shouldBe review.getItem.getAuto.getMark
        migrationInfoRes.getModel shouldBe review.getItem.getAuto.getModel
        migrationInfoRes.getSuperGenId shouldBe review.getItem.getAuto.getSuperGenId
        migrationInfoRes.getOldId shouldBe review.getMigrationId
        migrationInfoRes.getNewId shouldBe saveReview.getReviewId
      }
    }
  }

  test("migrate likes") {
    val id = Gen.posNum[Int].sample
    val reviewer = ReviewUser.newBuilder().setId("user:1234").build()
    val auto = Auto.newBuilder().setMark("BMW").setModel("X5").build()
    val item = Item.newBuilder().setAuto(auto)
    val review = Review.newBuilder()
      .setId(id.toString)
      .setMigrationId(1234)
      .setItem(item)
      .setReviewer(reviewer)
      .build()
    var saveReview: ReviewSaveResponse = null
    val migrateReq = Post("/api/v1/reviews/migration", review.toByteArray)
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(review)))
    migrateReq ~> route ~> check {
      withClue(responseAs[ReviewSaveResponse]) {
        status shouldBe StatusCodes.OK
        saveReview = responseAs[ReviewSaveResponse]
      }
    }

    val useful1 = MigrationOpinion(saveReview.getReviewId, 123, 1)
    val useful2 = MigrationOpinion(saveReview.getReviewId, 1234, 2)
    val usefulList = MigrationOpinionList(Seq(useful1, useful2))
    val migrateLikesReq = Post(s"/api/v1/reviews/migration/useful")
      .withEntity(HttpEntity(ContentTypes.`application/json`, usefulList.toJson.toString))
    migrateLikesReq ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
