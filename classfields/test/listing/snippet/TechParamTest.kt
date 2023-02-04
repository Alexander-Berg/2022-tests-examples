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
class TechParamTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val extendedOfferDispatcher =
        PostSearchOffersDispatcher("informers_extended_snippet_vin_ok_no_history")
    private val commonOfferDispatcher =
        PostSearchOffersDispatcher("informers_common_snippet_vin_ok_no_history")
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
    fun shouldSeeTechParamsOnExtendedOffer() {
        dispatcherHolder.innerDispatcher = extendedOfferDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isTechParam1Displayed("150 000 км")
                isTechParam2Displayed("3 л / 269 л.с. / Дизель")
                isTechParam3Displayed("Автоматическая")
                isTechParam4Displayed("Черный")
                isTechParam5Displayed("Полный")
                isTechParam6Displayed("Внедорожник 5 дв.")
            }
    }

    @Test
    fun shouldSeeTechParamsOnCommonOffer() {
        dispatcherHolder.innerDispatcher = commonOfferDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isTechParam1Displayed("150 000 км")
                isTechParam2Displayed("3 л / 269 л.с. / Дизель")
                isTechParam3Displayed("Автоматическая")
                isTechParam4Displayed("Черный")
                isTechParam5Displayed("Полный")
                isTechParam6Displayed("Внедорожник 5 дв.")
            }
    }
}
