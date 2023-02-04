package ru.yandex.vos2.reviews.`export`.filter

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.{LogbookEntry, Moderation}
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.OfferService
import ru.yandex.vos2.reviews.BaseReviewsTest

class EdsReviewsAndLogbookFilterTest extends BaseReviewsTest {

  private val filter = new EdsReviewsAndLogbookFilter

  private val offerBuilder = OfferModel.Offer.newBuilder()
    .setOfferService(OfferService.OFFER_AUTO)
    .setUserRef("user")
    .setTimestampCreate(new DateTime(System.currentTimeMillis()).plusDays(4).getMillis)
    .setTimestampUpdate(4)

  test("should process previous accepted review") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val moderationProgress = Moderation.newBuilder()
      .setStatus(Moderation.Status.IN_PROGRESS)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(1).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(System.currentTimeMillis))
      .addModerationHistory(moderationOk)
      .addModerationHistory(moderationProgress)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe true
  }

  test("should process accepted review") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(System.currentTimeMillis))
      .addModerationHistory(moderationOk)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe true
  }


  test("should process declined and then accepted review") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(1).getMillis))

    val moderationDeclined = Moderation.newBuilder()
      .setStatus(Moderation.Status.DECLINED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(System.currentTimeMillis))
      .addModerationHistory(moderationOk)
      .addModerationHistory(moderationDeclined)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe true
  }


  test("should process not accepted review when delay is expired") {

    val moderationProgress = Moderation.newBuilder()
      .setStatus(Moderation.Status.IN_PROGRESS)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(1).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(4).getMillis))
      .addModerationHistory(moderationProgress)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe true
  }

  test("should not process not accepted review when delay is not expired") {
    val moderationProgress = Moderation.newBuilder()
      .setStatus(Moderation.Status.IN_PROGRESS)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(1).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))
      .addModerationHistory(moderationProgress)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe false
  }

  test("should process previous declined review when delay is expired") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(3).getMillis))

    val moderationDeclined = Moderation.newBuilder()
      .setStatus(Moderation.Status.DECLINED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val moderationProgress = Moderation.newBuilder()
      .setStatus(Moderation.Status.IN_PROGRESS)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(1).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(4).getMillis))
      .addModerationHistory(moderationProgress)
      .addModerationHistory(moderationDeclined)
      .addModerationHistory(moderationOk)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe true
  }

  test("should not process previous declined review when delay is not expired") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(3).getMillis))

    val moderationDeclined = Moderation.newBuilder()
      .setStatus(Moderation.Status.DECLINED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val moderationProgress = Moderation.newBuilder()
      .setStatus(Moderation.Status.IN_PROGRESS)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(1).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))
      .addModerationHistory(moderationProgress)
      .addModerationHistory(moderationDeclined)
      .addModerationHistory(moderationOk)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe false
  }

  test("should not process accepted draft") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.DRAFT)
      .setUpdated(Timestamps.fromMillis(System.currentTimeMillis))
      .addModerationHistory(moderationOk)
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe false
  }

  test("should process accepted public logbook") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(System.currentTimeMillis))
      .addModerationHistory(moderationOk)
      .setKind(Review.Kind.LOGBOOK)
      .setLogbookEntry(Review.LogbookEntry
        .newBuilder()
        .setVisibility(LogbookEntry.Visibility.PUBLIC)
        .build())
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe true
  }

  test("should not process accepted private logbook") {

    val moderationOk = Moderation.newBuilder()
      .setStatus(Moderation.Status.ACCEPTED)
      .setTime(Timestamps.fromMillis(new DateTime(System.currentTimeMillis()).minusHours(2).getMillis))

    val review = Review.newBuilder()
      .setStatus(Review.Status.ENABLED)
      .setUpdated(Timestamps.fromMillis(System.currentTimeMillis))
      .addModerationHistory(moderationOk)
      .setKind(Review.Kind.LOGBOOK)
      .setLogbookEntry(Review.LogbookEntry
        .newBuilder()
        .setVisibility(LogbookEntry.Visibility.PRIVATE)
        .build())
    filter.apply(offerBuilder.setReview(review).build(), 0) shouldBe false
  }

}
