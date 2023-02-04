package ru.auto.ara.test.listing.snippet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class OldPriceTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val extendedOfferWithPriceHistoryDispatcher =
        PostSearchOffersDispatcher("informers_extended_snippet_vin_ok_no_history")
    private val commonOfferWithPriceHistoryDispatcher =
        PostSearchOffersDispatcher("informers_common_snippet_vin_ok_no_history")

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
    fun shouldSeeOldPriceOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferWithPriceHistoryDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isPriceDisplayed("1 300 000 \u20BD")
                isOldPriceDownIconDisplayed()
            }
    }

    @Test
    fun shouldSeeOldPriceOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferWithPriceHistoryDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isPriceDisplayed("1 300 000 \u20BD")
                isOldPriceDownIconDisplayed()
            }
    }
}
