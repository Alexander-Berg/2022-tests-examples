package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class FeedToolbarTest {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val postSearchDispatcherHolder = DispatcherHolder()
    private val parseDeeplinkDispatcherHolder = DispatcherHolder()
    private val countDispatcherHolder = DispatcherHolder()

    private val dispatchers = listOf(
        postSearchDispatcherHolder,
        parseDeeplinkDispatcherHolder,
        countDispatcherHolder
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun checkToolbarShowCountAndPricesRange() {
        setupGenericCarsFeed()
        checkSearchFeed {
            isToolbarWithTitleAndSubtitleDisplayed(
                title = "50 826 предложений",
                subtitle = "от 15 000 до 14 828 459 \u20BD"
            )
            isToolbarNavIconWithDrawableResDisplayed(R.drawable.ic_arrow_back)
        }
    }

    @Test
    fun checkToolbarShowTitleForEmptyFeed() {
        setupEmptyCarsFeed()
        countDispatcherHolder.innerDispatcher = CountDispatcher.getEmpty(category = "cars")
        checkSearchFeed {
            isToolbarWithTitleAndNoSubtitleDisplayed(
                title = "Нет предложений"
            )
            isToolbarNavIconWithDrawableResDisplayed(R.drawable.ic_arrow_back)
        }
    }

    @Test
    fun checkToolbarShowCountForTrucks() {
        setupTrucksFeed()
        checkSearchFeed {
            isToolbarWithTitleAndNoSubtitleDisplayed(
                title = "50 826 предложений"
            )
            isToolbarNavIconWithDrawableResDisplayed(R.drawable.ic_arrow_back)
        }
    }

    @Test
    fun checkGoBackToTransportOnToolbarNavIconClick() {
        setupGenericCarsFeed()
        performSearchFeed {
            clickToolbarNavIcon()
        }
        checkMain {
            isMainTabSelected(R.string.transport)
        }
    }

    @Test
    fun shouldScrollUpAndShowFabOnTapOnActionBar() {
        setupGenericCarsFeed()
        performSearchFeed().checkResult {
            isFabDisplayed()
        }
        performSearchFeed {
            waitFirstPageLoaded(1)
            scrollDownOneScreen()
        }.checkResult {
            isFabHidden()
        }
        performSearchFeed {
            clickToolbar()
        }.checkResult {
            isFabDisplayed()
            isStateSelectorDisplayed()
        }
    }

    private fun setupGenericCarsFeed() {
        setupAndLaunch(
            parseDeeplinkDispatcher = ParseDeeplinkDispatcher(CARS_DISPATCHER_FILE, requestWatcher = null),
            postSearchOfferDispatcher = PostSearchOffersDispatcher.getGenericFeed(),
            countDispatcher = CountDispatcher("cars"),
            deeplink = CARS_URI
        )
    }

    private fun setupEmptyCarsFeed() {
        setupAndLaunch(
            parseDeeplinkDispatcher = ParseDeeplinkDispatcher(CARS_DISPATCHER_FILE, requestWatcher = null),
            postSearchOfferDispatcher = PostSearchOffersDispatcher.getEmptyFeed(),
            countDispatcher = CountDispatcher.getEmpty("cars"),
            deeplink = CARS_URI
        )
    }

    private fun setupTrucksFeed() {
        setupAndLaunch(
            parseDeeplinkDispatcher = ParseDeeplinkDispatcher(TRUCKS_DISPATCHER_FILE, requestWatcher = null),
            postSearchOfferDispatcher = PostSearchOffersDispatcher.getTrucksFeed(),
            countDispatcher = CountDispatcher("trucks"),
            deeplink = TRUCKS_URI
        )
    }

    private fun setupAndLaunch(
        parseDeeplinkDispatcher: ParseDeeplinkDispatcher,
        postSearchOfferDispatcher: PostSearchOffersDispatcher,
        countDispatcher: CountDispatcher,
        deeplink: String
    ) {
        parseDeeplinkDispatcherHolder.innerDispatcher = parseDeeplinkDispatcher
        postSearchDispatcherHolder.innerDispatcher = postSearchOfferDispatcher
        countDispatcherHolder.innerDispatcher = countDispatcher
        activityTestRule.launchDeepLinkActivity(deeplink)
    }

    companion object {
        private const val CARS_URI: String = "https://auto.ru/cars/ford/mondeo/20270679/all/"
        private const val CARS_DISPATCHER_FILE: String = "cars_ford_mondeo_20270679_all"

        private const val TRUCKS_URI: String = "https://auto.ru/trucks/all"
        private const val TRUCKS_DISPATCHER_FILE: String = "trucks_lcv_new"
    }
}
