package ru.yandex.vos2.reviews.stage

import io.prometheus.client.Histogram
import org.mockito.Mockito.doNothing
import org.scalatest.{FunSuite, Matchers}
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.{Moderation, Notification, Status}
import ru.auto.api.reviews.ReviewModel.Review.Notification.NotificationType
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.reviews.watching.stages.NewReviewPromocodeStage
import ru.yandex.vos2.services.promocoder.PromocoderClient
import ru.yandex.vos2.util.StageUtils
import ru.yandex.vos2.watching.ProcessingState
import ru.yandex.vos2.reviews.utils.ReviewModelUtils._
import ru.yandex.vos2.services.passport.PassportClient

import scala.collection.JavaConverters._
/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 05/10/2018.
  */
class NewReviewPromocodeStageTest extends FunSuite with Matchers with MockitoSupport with StageUtils {

  private val promocoderClient = mock[PromocoderClient]
  private val passportClient = mock[PassportClient]
  private val histogram = mock[Histogram]

  test("second promo code") {
    val notification = Notification.newBuilder().setType(NotificationType.NEW_REVIEW_PROMOCODE).build()
    val review = Review.newBuilder().addNotification(notification).build()
    val offer = OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_REVIEW)
      .setTimestampUpdate(0)
      .setUserRef("user:123")
      .setReview(review)
      .build()
    val state = ProcessingState(offer, offer)
    val stage = new NewReviewPromocodeStage(promocoderClient, passportClient, histogram)

    review.getNotificationList.asScala.head.getTimestampCancel shouldBe 0
    review.hasNotificationByType(NotificationType.NEW_REVIEW_PROMOCODE) shouldBe true
    doNothing().when(promocoderClient).createPromocode(?, ?)
    when(passportClient.getModeration(?)).thenReturn(Some(UserModerationStatus.newBuilder().build()))

    stage.process(state).offer.getReview.getNotificationCount shouldBe 1

  }

  test("should process") {

    val moderation = Moderation.newBuilder().setStatus(Moderation.Status.ACCEPTED).build()
    val origin = Review.Origin.newBuilder().setCampaignId("prmcd_test").build()
    val review = Review.newBuilder().addModerationHistory(moderation).setOrigin(origin).setStatus(Status.ENABLED)

    val offer = OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_REVIEW)
      .setTimestampUpdate(0)
      .setTimestampCreate(System.currentTimeMillis())
      .setUserRef("user:123")
      .setReview(review)
      .build()

    val stage = new NewReviewPromocodeStage(promocoderClient, passportClient, histogram)

    doNothing().when(promocoderClient).createPromocode(?, ?)
    when(passportClient.getModeration(?)).thenReturn(Some(UserModerationStatus.newBuilder().build()))

    assert(stage.shouldProcess(offer))
  }

}
