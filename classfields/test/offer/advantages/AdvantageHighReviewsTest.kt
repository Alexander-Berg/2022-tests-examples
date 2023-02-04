package ru.auto.ara.test.offer.advantages

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.reviews.checkReviewsCounterQuery
import ru.auto.ara.core.dispatchers.reviews.checkReviewsListingQuery
import ru.auto.ara.core.dispatchers.reviews.checkReviewsRatingQuery
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.dispatchers.reviews.getReviewsListing
import ru.auto.ara.core.dispatchers.reviews.getReviewsRating
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.offercard.checkAdvantageHighReviews
import ru.auto.ara.core.robot.offercard.performAdvantageHighReviews
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.reviews.checkReviewsFeed
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ALL_ADVANTAGES
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER
import ru.auto.ara.core.testdata.OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE
import ru.auto.ara.core.testdata.OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE_WITH_DATA_IN_OFFER
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class AdvantageHighReviewsTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    val webServerRule = WebServerRule {
        userSetup()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        SetupAuthRule()
    )

    @Test
    fun shouldShowAdvantageHighReviewsWithDataInOffer() {
        setupReviewsRouting(
            mark = "MERCEDES",
            model = "CLS_KLASSE_AMG",
            superGen = "21321173"
        )
        openOfferCard(OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER)
        openAdvantageHighReviews(HIGH_REVIEWS_POSITION_WITH_DATA)
        checkAdvantageHighReviewsWithDataInOffer()
    }

    @Test
    fun shouldShowAdvantageHighReviewsWithoutDataInOffer() {
        setupReviewsRouting(
            mark = "MERCEDES",
            model = "CLS_KLASSE_AMG",
            superGen = "21321173"
        )
        openOfferCard(OFFER_ID_WITH_ALL_ADVANTAGES)
        openAdvantageHighReviews(HIGH_REVIEWS_POSITION_WITHOUT_DATA)
        checkAdvantageHighReviewsWithoutDataInOffer()
    }

    @Test
    fun shouldShowAdvantageHighReviewsSingleWithDataInOffer() {
        setupReviewsRouting(
            mark = "AUDI",
            model = "A5",
            superGen = "20795592"
        )
        openOfferCard(OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE_WITH_DATA_IN_OFFER)
        openAdvantageHighReviewsSingle()
        checkAdvantageHighReviewsWithDataInOffer()
    }

    @Test
    fun shouldShowAdvantageHighReviewsSingleWithoutDataInOffer() {
        setupReviewsRouting(
            mark = "NISSAN",
            model = "QASHQAI",
            superGen = "20097928"
        )
        openOfferCard(OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE)
        openAdvantageHighReviewsSingle()
        checkAdvantageHighReviewsWithoutDataInOffer()
    }

    private fun checkAdvantageHighReviewsWithDataInOffer() {
        checkAdvantageHighReviews("4.8", "Рейтинг\nпо 300 отзывам")
        checkAdvantageHighReviewsAction()
    }

    private fun checkAdvantageHighReviewsWithoutDataInOffer() {
        checkAdvantageHighReviews(AVG_RATING_TEXT_FROM_REQUEST, "Рейтинг\nпо 30 отзывам")
        checkAdvantageHighReviewsAction()
    }

    private fun checkAdvantageHighReviews(avgRatingText: String, ratingByReviewText: String) {
        checkAdvantageHighReviews {
            isShadowBoxDisplayed()
            isRatingBarDisplayed()
            isAvgRatingTextDisplayed(avgRatingText)
            isMaxRatingTextDisplayed()
            isRatingByReviewTextDisplayed(ratingByReviewText)
            isProgressNotDisplayed()
            isErrorImageNotDisplayed()
            isErrorTextNotDisplayed()
            isActionDisplayed()
        }
    }

    private fun checkAdvantageHighReviewsAction() {
        performAdvantageHighReviews { clickOnActionButton() }
        checkReviewsFeed { isReviewFeed() }
    }

    private fun setupReviewsRouting(mark: String, model: String, superGen: String) {
        webServerRule.routing {
            getReviewsCounter().watch { checkReviewsCounterQuery(mark, model, superGen) }
            getReviewsRating(AVG_RATING_TEXT_FROM_REQUEST).watch { checkReviewsRatingQuery(mark, model, superGen) }
            getReviewsListing().watch { checkReviewsListingQuery(mark, model, superGen) }
        }
    }

    private fun openOfferCard(offerId: String) {
        webServerRule.routing { getOffer(offerId) }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + offerId)
    }

    private fun openAdvantageHighReviews(pos: Int) {
        performOfferCard {
            scrollToAdvantages()
            scrollToAdvantage(pos)
            clickOnAdvantage(pos)
        }
    }

    private fun openAdvantageHighReviewsSingle() {
        performOfferCard {
            scrollToAdvantageSingle()
            clickOnAdvantageSingle()
        }
    }

    companion object {
        private const val HIGH_REVIEWS_POSITION_WITH_DATA = 8
        private const val HIGH_REVIEWS_POSITION_WITHOUT_DATA = 7
        private const val AVG_RATING_TEXT_FROM_REQUEST = "4.5"
        private const val OFFER_CARD_PATH = "https://auto.ru/cars/used/sale/"
    }
}
