package ru.auto.ara.test.listing.snippet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.checkListingOffers
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
class NewBadgeTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            PostSearchOffersDispatcher("extended_availability_in_stock"),
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

    @Before
    fun setupDispatchers() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFirstSnippet() }
    }

    @Test
    fun shouldSeeNewBadgeOnSnippetInformer() {
        checkListingOffers {
            isGalleryBadgeDisplayed(
                badgeText = R.string.new_auto,
                badgeColor = R.color.auto_primary_analogous_emphasis_high
            )
        }
    }

}
