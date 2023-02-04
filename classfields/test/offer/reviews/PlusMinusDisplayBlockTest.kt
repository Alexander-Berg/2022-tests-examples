package ru.auto.ara.test.offer.reviews

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.journal.getEmptyJournals
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsFeaturesDispatcher
import ru.auto.ara.core.dispatchers.safe_deal.getEmptySafeDealList
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.review.Feature

@RunWith(Parameterized::class)
class PlusMinusDisplayBlockTest(val testParams: TestParameter) {

    private val reviewsFeaturesRequestWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer(category = testParams.category, offerId = testParams.offerId),
            GetReviewsFeaturesDispatcher(
                category = testParams.category,
                countOfSegments = "3_segments",
                requestWatcher = reviewsFeaturesRequestWatcher
            )
        )
        getEmptySafeDealList()
        getEmptyJournals()
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
        activityTestRule.launchDeepLinkActivity(testParams.uri)
        performOfferCard { scrollToPlusMinusWithoutOverallRating() }
    }

    @Test
    fun shouldSeePlusMinusBlockWithCorrectRequests() {
        checkOfferCard {
            isPlusMinusExpandViewDisplayed("78 / 41")
            isPlusesSelectedOnSegment()
            isAllPlusMinusButtonDisplayed()
            isPlusMinusFeatureDisplayed(testParams.displayedFeatures)
        }

        reviewsFeaturesRequestWatcher.checkQueryParameters(testParams.featureRequestParams)
    }

    companion object {
        @Parameterized.Parameters(name = "index={index} {0}")
        @JvmStatic
        fun data(): Collection<Array<out Any?>> = listOf(
            TestParameter(
                uri = "https://auto.ru/cars/used/sale/1084155311-742cfbff",
                offerId = "1084155311-742cfbff",
                category = "cars",
                featureRequestParams = listOf(
                    "mark" to "AUDI",
                    "model" to "A4",
                    "super_gen" to "7754683"
                ),
                displayedFeatures = listOf(
                    Feature("Комфорт", 10, 3, ""),
                    Feature("Дизайн", 10, 0, ""),
                    Feature("Шумоизоляция", 10, 1, ""),
                    Feature("Управляемость", 9, 2, "")
                )
            ),
            TestParameter(
                uri = "https://auto.ru/trucks/used/sale/15814704-7c0f4567",
                offerId = "15814704-7c0f4567",
                category = "trucks",
                featureRequestParams = listOf(
                    "mark" to "CITROEN",
                    "model" to "BERLINGO"
                ),
                displayedFeatures = listOf(
                    Feature("Комфорт", 10, 3, ""),
                    Feature("Дизайн", 10, 0, ""),
                    Feature("Шумоизоляция", 10, 1, ""),
                    Feature("Управляемость", 9, 2, "")
                )
            ),
            TestParameter(
                uri = "https://auto.ru/moto/used/sale/3042550-19b540c6",
                offerId = "3042550-19b540c6",
                category = "moto",
                featureRequestParams = listOf(
                    "mark" to "STELS",
                    "model" to "ATV_600_LEOPARD"
                ),
                displayedFeatures = listOf(
                    Feature("Комфорт", 10, 3, ""),
                    Feature("Дизайн", 10, 0, ""),
                    Feature("Шумоизоляция", 10, 1, ""),
                    Feature("Управляемость", 9, 2, "")
                )
            )
        ).map { arrayOf(it) }
    }

    data class TestParameter(
        val uri: String,
        val offerId: String,
        val category: String,
        val featureRequestParams: List<Pair<String, String>>,
        val displayedFeatures: List<Feature>
    ) {
        override fun toString(): String = category
    }
}
