package ru.auto.ara.test.offer.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.reviews.getReview
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.dispatchers.reviews.getReviewsListing
import ru.auto.ara.core.dispatchers.reviews.getReviewsRating
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.reviews.performReview
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OverallRatingClickItemsTest {

    private val uri = "https://auto.ru/cars/used/sale/1084155311-742cfbff"
    private val offerId = "1084155311-742cfbff"
    private val overallRating = "4.5"
    private val reviewRating = "5.0"
    private val title = "Отзыв по автомашине Ауди а4"
    private val reviewId = "4469251119274136739"

    private val webServerRule = WebServerRule {
        getOffer(offerId)
        getReviewsListing()
        getReviewsCounter()
        getReviewsRating(overallRating)
        getReview(reviewId = reviewId, type = "card")
    }

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity(uri)
        performOfferCard { scrollToOverallRating() }
    }

    @Test
    fun shouldHideOverallRatingBlock() {
        waitSomething(200, TimeUnit.MILLISECONDS)
        performOfferCard { clickOverallReviewsExpandArrow() }
        waitSomething(200, TimeUnit.MILLISECONDS)
        performOfferCard { scrollToOverallRating() }
        checkOfferCard {
            isOverallRatingTitleDisplayed()
            isOverallRatingSubtitleDisplayed("по 30 отзывам")
            isOverallRatingExpandArrowDisplayed()
            isOverallRatingValueDisplayed(overallRating)
            isOverallRatingHidden(title, reviewRating)
        }
    }

    @Test
    fun shouldUnhideOverallRatingBlock() {
        waitSomething(200, TimeUnit.MILLISECONDS)
        performOfferCard { clickOverallReviewsExpandArrow() }
        waitSomething(200, TimeUnit.MILLISECONDS)
        checkOfferCard { isOverallRatingHidden(title, reviewRating) }
        performOfferCard {
            clickOverallReviewsExpandArrow()
            waitSomething(200, TimeUnit.MILLISECONDS)
            scrollToAllReviewsButton()
        }
        checkOfferCard {
            isOverallRatingTitleDisplayed()
            isOverallRatingSubtitleDisplayed("по 30 отзывам")
            isOverallRatingExpandArrowDisplayed()
            isReviewSubtitleDisplayed(title)
            isReviewImageDisplayed(title)
            isReviewRatingDisplayed(reviewRating)
            isAllReviewsButtonDisplayed()
        }
    }

    @Test
    fun shouldOpenReview() {
        performOfferCard { clickReview(title) }
        performReview {}.checkResult { isReviewToolbarDisplayed(); isTitleWithTextVisible("Audi A4 IV (B8) Рестайлинг") }
    }
}
