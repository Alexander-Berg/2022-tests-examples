package ru.auto.ara.test.offer.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewFeatureSnippetDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsFeaturesDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performFeatureSnippet
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.review.Feature
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class PlusMinusItemClicksTest {
    private val uri = "https://auto.ru/cars/used/sale/1084155311-742cfbff"
    private val offerId = "1084155311-742cfbff"
    private val category = "cars"
    private val reviewsFeatureSnippetRequestWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer(category, offerId),
            GetReviewsFeaturesDispatcher(category, "3_segments"),
            GetReviewFeatureSnippetDispatcher(category, "COMFORT", reviewsFeatureSnippetRequestWatcher)
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

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
        performOfferCard { scrollToPlusMinusWithoutOverallRating() }

    }

    @Test
    fun shouldHidePlusMinusBlock() {
        performOfferCard { clickPlusMinusExpandArrow() }
            .checkResult {
                isPlusMinusExpandViewDisplayed("78 / 41")
                isPlusMinusSegmentNotDisplayed()
                isAllPlusMinusButtonNotDisplayed()
                isPlusMinusFeatureNotDisplayed("Комфорт")
            }
    }

    @Test
    fun shouldUnhidePlusMinusBlock() {
        performOfferCard { clickPlusMinusExpandArrow() }.checkResult { isPlusMinusSegmentNotDisplayed() }
        performOfferCard {
            clickPlusMinusExpandArrow()
            scrollToShowAllReviews()
        }
            .checkResult {
                isPlusMinusExpandViewDisplayed("78 / 41")
                isPlusesSelectedOnSegment()
                isAllPlusMinusButtonDisplayed()
                isPlusMinusFeatureDisplayed(
                    listOf(
                        Feature("Комфорт", 10, 3, ""),
                        Feature("Дизайн", 10, 0, ""),
                        Feature("Шумоизоляция", 10, 1, ""),
                        Feature("Управляемость", 9, 2, "")
                    )
                )
            }
    }

    @Test
    fun shouldSeeMoreFeaturesOnTapShowMore() {
        waitSomething(200, TimeUnit.MILLISECONDS)
        performOfferCard { clickShowMorePlusMinusFeatures() }
        waitSomething(200, TimeUnit.MILLISECONDS)
        checkOfferCard {
            isPlusMinusFeatureDisplayed(
                listOf(
                    Feature("Динамика", 8, 2, ""),
                    Feature("Обзорность", 3, 0, "")
                )
            )
        }
        performOfferCard {
            scrollToCollapseButton()
            clickCollapseButton()
            scrollToPlusMinusWithoutOverallRating()
        }
        waitSomething(200, TimeUnit.MILLISECONDS)
        checkOfferCard {
            isAllPlusMinusButtonDisplayed()
            isPlusMinusFeatureNotDisplayed("Динамика")
            isPlusMinusFeatureNotDisplayed("Обзорность")
        }
    }

    @Test
    fun shouldOpenReviewSnippet() {
        performOfferCard { clickPlusMinusFeature("Комфорт") }
        performFeatureSnippet {}.checkResult {
            isFeatureSnippet("Комфорт")
        }
        reviewsFeatureSnippetRequestWatcher.checkQueryParameters(
            listOf(
                "mark" to "AUDI",
                "model" to "A4",
                "super_gen" to "7754683",
                "feature" to "COMFORT"
            )
        )
        performFeatureSnippet {
            pressBack()
        }
        checkOfferCard {
            isPlusMinusExpandViewDisplayed("78 / 41")
            isPlusesSelectedOnSegment()
            isAllPlusMinusButtonDisplayed()
            isPlusMinusFeatureDisplayed(
                listOf(
                    Feature("Комфорт", 10, 3, ""),
                    Feature("Дизайн", 10, 0, ""),
                    Feature("Шумоизоляция", 10, 1, ""),
                    Feature("Управляемость", 9, 2, "")
                )
            )
        }
    }
}
