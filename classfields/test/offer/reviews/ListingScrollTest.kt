package ru.auto.ara.test.offer.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.reviews.GetReviewsListingDispatcher
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.dispatchers.reviews.getReviewsRating
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class ListingScrollTest {
    private val uri = "https://auto.ru/cars/all"
    private val category = "cars"
    private val overallRating = "4.5"
    private val reviewsListingRequestWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            PostSearchOffersDispatcher("extended_availability_on_order"),
            GetReviewsListingDispatcher(category, reviewsListingRequestWatcher),
            ParseDeeplinkDispatcher.carsAll()
        )
        getOffer("1082957054-8d55bf9a")
        getReviewsCounter()
        getReviewsRating(overallRating)
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
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            interactions.onPrice().waitUntilIsCompletelyDisplayed().performClick()
        }
        performOfferCard { scrollToOverallRating() }
    }

    @Test
    fun shouldRequestCorrectPageNumberOfReviewsListing() {
        performOfferCard {
            scrollHorizontalViewOfReviews(
                titleOfReviewScrollTo = "Знакомство с немецким автопромом или на сколько мы разные с А4 1.8 T 170",
                titleOfVisibleReview = "Отзыв по автомашине Ауди а4"
            )
            reviewsListingRequestWatcher.checkQueryParameter("page", "2")
            pressBack()
        }
        performListingOffers { interactions.onPrice().waitUntilIsCompletelyDisplayed().performClick() }
        reviewsListingRequestWatcher.checkQueryParameter("page", "1")
    }
}
