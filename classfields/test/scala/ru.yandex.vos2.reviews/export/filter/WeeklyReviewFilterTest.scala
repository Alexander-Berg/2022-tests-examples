package ru.yandex.vos2.reviews.export.filter

import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.utils.ReviewModelUtils._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 15/12/2017.
  */
class WeeklyReviewFilterTest extends BaseReviewsTest {

  val filter = new WeeklyReviewFilter(Some(Category.CARS))

  test("weekly reviews") {
    filter.apply(createOffer1, 0) shouldBe true
    filter.apply(createOffer2, 0) shouldBe false
  }

  test("moderation hash") {
    val review = createOffer1.getReview
    review.getCurrentModerationHash shouldBe review.getCurrentModerationHash
  }
}
