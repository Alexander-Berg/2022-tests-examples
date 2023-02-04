package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersGroupDispatcher
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.GroupBy
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class EmptyFeedTest {

    private val postSearchOffersWatcher = RequestWatcher()
    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher(category = "cars"),
        PostSearchOffersDispatcher.getLadaPresetOffers(postSearchOffersWatcher),
        PostSearchOffersGroupDispatcher(fileName = "group/empty.json", interceptGroupBy = setOf(GroupBy.CONFIGURATION))
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldSeeRecommendLookCorrect() {
        openCarsSearchFeed(StateGroup.NEW)
        checkSearchFeed {
            isEmptyFeedDisplayed()
        }
    }

    private fun openCarsSearchFeed(stateGroup: StateGroup) {
        activityRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragmentBundles.searchFeedBundle(
                category = VehicleCategory.CARS,
                stateGroup = stateGroup
            )
        )
    }
}
