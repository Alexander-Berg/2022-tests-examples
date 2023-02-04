package ru.auto.ara.test.listing.snippet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class AutoRuExclusiveInformerTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"

    private val searchFeedHasCarfaxInformersDispatcher =
        PostSearchOffersDispatcher(fileName = "informers_search_feed_has_carfax")
    private val searchFeedAutoRuInformerDispatcher =
        PostSearchOffersDispatcher(fileName = "informers_search_feed_autoru")
    private val searchFeedAutoRuHasCarfaxInformersDispatcher =
        PostSearchOffersDispatcher(fileName = "informers_search_feed_autoru_has_carfax")
    private val searchFeedNoInformersDispatcher =
        PostSearchOffersDispatcher(fileName = "informers_search_feed_no_informers")

    private val dispatcherHolder = DispatcherHolder()

    private val dispatchers: List<DelegateDispatcher> = listOf(
        dispatcherHolder,
        ParseDeeplinkDispatcher.carsAll()
    )

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun checkHasCarfaxByVinInformerSearchFeed() {
        dispatcherHolder.innerDispatcher = searchFeedHasCarfaxInformersDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isAutoRuExclusiveInformerNotDisplayed()
                isHasCarfaxByVinInformerDisplayed()
            }
    }

    @Test
    fun checkAutoRuInformerSearchFeed() {
        dispatcherHolder.innerDispatcher = searchFeedAutoRuInformerDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isAutoRuExclusiveInformerDisplayed()
                isHasCarfaxByVinInformerNotDisplayed()
            }
    }

    @Test
    fun checkAutoRuHasCarfaxInformerSearchFeed() {
        dispatcherHolder.innerDispatcher = searchFeedAutoRuHasCarfaxInformersDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isAutoRuExclusiveInformerDisplayed()
                isHasCarfaxByVinInformerDisplayed()
            }
    }

    @Test
    fun checkNoInformersSearchFeed() {
        dispatcherHolder.innerDispatcher = searchFeedNoInformersDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isAutoRuExclusiveInformerNotDisplayed()
                isHasCarfaxByVinInformerNotDisplayed()
            }
    }
}
