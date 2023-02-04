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
class BrandCertInformerTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val extendedOfferWithBrandCertInformerDispatcher = PostSearchOffersDispatcher("informers_extended_snippet_brand_cert")
    private val commonOfferWithBrandCertInformerDispatcher = PostSearchOffersDispatcher("informers_common_snippet_brand_cert")
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
    fun shouldSeeBrandCertInformerOnExtendedOffer() {
        dispatcherHolder.innerDispatcher = extendedOfferWithBrandCertInformerDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isBrandCertInformerDisplayed(BRAND_CERT_TEXT)
            }
    }

    @Test
    fun shouldSeeBrandCertInformerOnCommonOffer() {
        dispatcherHolder.innerDispatcher = commonOfferWithBrandCertInformerDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isBrandCertInformerDisplayed(BRAND_CERT_TEXT)
            }
    }

    companion object {
        private const val BRAND_CERT_TEXT = "Проверен BMW"
    }
}
