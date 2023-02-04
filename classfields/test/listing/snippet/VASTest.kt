package ru.auto.ara.test.listing.snippet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class VASTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val extendedOfferDispatcher = PostSearchOffersDispatcher("informers_extended_snippet_vin_ok_no_history")
    private val commonOfferDispatcher = PostSearchOffersDispatcher("informers_common_snippet_vin_ok_no_history")

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            ParseDeeplinkDispatcher.carsAll()
        )
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldSeeTopIconOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isTopIconDisplayed()
            }
    }

    @Test
    fun shouldSeePriceHighlightedOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isHighlightedPriceDisplayed()
            }
    }

    @Test
    fun shouldSeePriceHighlightedOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isHighlightedPriceDisplayed()
            }
    }

    @Test
    fun shouldSeeFreshIconOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isFreshIconDisplayed()
            }
    }
}
