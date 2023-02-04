package ru.auto.ara.test.offer.reviews

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.journal.getEmptyJournals
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.dispatchers.reviews.getReviewsListing
import ru.auto.ara.core.dispatchers.reviews.getReviewsRating
import ru.auto.ara.core.dispatchers.safe_deal.getEmptySafeDealList
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.dispatchers.stub.StubGetCatalogAllDictionariesDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class OverallRatingTest(private val testParams: TestParameter) {


    private val webServerRule = WebServerRule {
        getOffer(testParams.offerId, testParams.category)
        getReviewsListing(testParams.category).watch {
            checkQueryParameters(testParams.listingParams)
        }
        getReviewsCounter(testParams.category).watch {
            checkQueryParameters(testParams.counterOrRatingParams)
        }
        getReviewsRating(testParams.overallRating, testParams.category).watch {
            checkQueryParameters(testParams.counterOrRatingParams)
        }
        postSearchOffers()
        getOfferCount()
        delegateDispatchers(
            StubGetCatalogAllDictionariesDispatcher
        )
        getEmptySafeDealList()
        getEmptyJournals()
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
        activityTestRule.launchDeepLinkActivity(testParams.uri)
    }

    @Test
    fun shouldSeeReviewBlockWithCorrectRequests() {
        performOfferCard { scrollToOverallRating() }
        checkOfferCard {
            isOverallRatingTitleDisplayed()
            isOverallRatingSubtitleDisplayed("по 30 отзывам")
            isOverallRatingExpandArrowDisplayed()
            isOverallRatingValueDisplayed(testParams.overallRating)
        }
        performOfferCard { scrollToAllReviewsButton() }
        checkOfferCard {
            isReviewSubtitleDisplayed(testParams.title)
            isReviewImageDisplayed(testParams.title)
            isReviewRatingDisplayed(testParams.reviewRating)
            isAllReviewsButtonDisplayed()
        }
    }

    companion object {
        @Parameterized.Parameters(name = "index={index} {0}")
        @JvmStatic
        fun data(): Collection<Array<out Any?>> = listOf(
            TestParameter(
                uri = "https://auto.ru/cars/used/sale/1084155311-742cfbff",
                offerId = "1084155311-742cfbff",
                category = "cars",
                overallRating = "4.5",
                reviewRating = "5.0",
                title = "Отзыв по автомашине Ауди а4",
                listingParams = listOf(
                    "category" to "CARS",
                    "page" to "1",
                    "sort" to "relevance-exp1-desc",
                    "mark" to "AUDI",
                    "model" to "A4",
                    "super_gen" to "7754683",
                    "page_size" to "10"
                ),
                counterOrRatingParams = listOf(
                    "mark" to "AUDI",
                    "model" to "A4",
                    "super_gen" to "7754683"
                )
            ),

            TestParameter(
                uri = "https://auto.ru/trucks/used/sale/15814704-7c0f4567",
                offerId = "15814704-7c0f4567",
                category = "trucks",
                overallRating = "2.3",
                reviewRating = "4.0",
                title = "Трудяга француз",
                listingParams = listOf(
                    "category" to "TRUCKS",
                    "page" to "1",
                    "sort" to "relevance-exp1-desc",
                    "mark" to "CITROEN",
                    "model" to "BERLINGO",
                    "page_size" to "10"
                ),
                counterOrRatingParams = listOf(
                    "mark" to "CITROEN",
                    "model" to "BERLINGO"
                )
            ),
            TestParameter(
                uri = "https://auto.ru/moto/used/sale/3042550-19b540c6",
                offerId = "3042550-19b540c6",
                category = "moto",
                overallRating = "2.6",
                reviewRating = "2.6",
                title = "Важно!",
                listingParams = listOf(
                    "category" to "MOTO",
                    "page" to "1",
                    "sort" to "relevance-exp1-desc",
                    "mark" to "STELS",
                    "model" to "ATV_600_LEOPARD",
                    "page_size" to "10"
                ),
                counterOrRatingParams = listOf(
                    "mark" to "STELS",
                    "model" to "ATV_600_LEOPARD"
                )
            )
        ).map { arrayOf(it) }
    }

    data class TestParameter(
        val uri: String,
        val offerId: String,
        val category: String,
        val overallRating: String,
        val reviewRating: String,
        val title: String,
        val listingParams: List<Pair<String, String>>,
        val counterOrRatingParams: List<Pair<String, String>>
    ) {
        override fun toString(): String = "offerId = $offerId"
    }
}
