package ru.auto.ara.test.listing.minifilter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.data.models.FormState
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.ara.viewmodel.search.SearchFeedContext
import ru.auto.data.model.search.SearchContext

@RunWith(AndroidJUnit4::class)
abstract class BaseMinifilterListingTest(val formState: FormState) {
    val GEO_RADIUS_REQUEST_PARAM = "geo_radius"
    val RID_REQUEST_PARAM = "rid"
    val STATE_REQUEST_PARAM = "state_group"

    val watcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            PostSearchOffersDispatcher(
                fileName = "informers_extended_snippet_vin_ok_no_history",
                requestWatcher = watcher
            )
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @JvmField
    @Rule
    val rule = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun setUp() {
        activityTestRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragment.createArgs(
                SearchFeedContext(
                    context = SearchContext.DEFAULT,
                    formState = formState
                )
            )
        )
        performSearchFeed {
            waitSearchFeed()
        }
    }
}
