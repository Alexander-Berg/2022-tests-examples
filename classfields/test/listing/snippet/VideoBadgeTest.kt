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
class VideoBadgeTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val extendedOfferWithVideoDispatcher =
        PostSearchOffersDispatcher("informers_extended_snippet_vin_ok_no_history")
    private val commonOfferWithVideoDispatcher =
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
    fun shouldSeeVideoBadgeOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferWithVideoDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToPhotoWithIndex(1)
        }.checkResult {
            isVideoBadgeDisplayed()
        }
    }

    @Test
    fun shouldSeeVideoBadgeOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferWithVideoDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isVideoBadgeDisplayed()
            }
    }
}
