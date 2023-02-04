package ru.yandex.vos2.reviews.stage

import io.prometheus.client.Histogram
import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vos2.reviews.utils.ReviewGenerators._
import ru.yandex.vos2.reviews.watching.stages.SendToModerationStage
import ru.yandex.vos2.services.moderation.ModerationClient
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.watching.ProcessingState
import org.mockito.Mockito._
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.Moderation
import ru.yandex.vos2.reviews.utils.ReviewModelUtils._
import ru.yandex.vos2.OfferModel.OfferService
import ru.yandex.vos2.reviews.kafka.ReviewsKafkaSender
import ru.yandex.vos2.reviews.utils.ReviewModelUtils._
import ru.yandex.vos2.util.StageUtils

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 24/11/2017.
  */
class SendToModerationStageTest extends FunSuite with Matchers with MockitoSupport with StageUtils {

  private val kafkaSender = mock[ReviewsKafkaSender]
  private val histogram = mock[Histogram]
  private val stage = new SendToModerationStage(kafkaSender, histogram)


  test("review shouldnt process") {
    val genReview = ReviewGen.sample.get
    val review = genReview.toBuilder.setModerationHash(genReview.getCurrentModerationHash).build()
    val offer = OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_REVIEW)
      .setTimestampUpdate(0)
      .setUserRef("user:123")
      .setReview(review)
      .build()

    stage.shouldProcess(offer) shouldBe false
  }

  test("review should process") {
    val genReview = ReviewGen.sample.get
    val review = genReview.toBuilder
      .setModerationHash(genReview.getCurrentModerationHash)
      .setTitle("title")
      .build()
    val offer = OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_REVIEW)
      .setTimestampUpdate(0)
      .setUserRef("user:123")
      .setReview(review)
      .build()

    stage.shouldProcess(offer) shouldBe true
  }

  test("review client invocation") {

    doNothing().when(kafkaSender.send(?))

    val review = ReviewGen.sample.get
    val offer = OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_REVIEW)
      .setTimestampUpdate(0)
      .setUserRef("user:123")
      .setReview(review)
      .build()
    val processingState = ProcessingState(offer, offer)

    val res = asyncProcess(stage, processingState)

    res.offer.getReview.getModerationStatus shouldBe Moderation.Status.IN_PROGRESS

    verify(kafkaSender.send(?))
  }

  test("reviewsr moderaion id") {
    val genReview = ReviewGen.sample.get
    val builder = genReview.toBuilder
    val reviewer = builder.getReviewerBuilder.setId("a_123")
    builder.setReviewer(reviewer)

    val review = builder.build()

    review.getUserPureId shouldBe "123"

  }
}
