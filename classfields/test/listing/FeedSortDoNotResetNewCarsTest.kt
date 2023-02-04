package ru.auto.ara.test.listing

import android.content.Intent
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup

/**
 * @author themishkun on 06/02/2019.
 */
@RunWith(Parameterized::class)
class FeedSortDoNotResetNewCarsTest(private val parameter: TestParameter) {

    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSearchOffersDispatcher.getGenericFeed()
    )
    var activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule
    @JvmField
    val ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun openParameters() {
        activityTestRule.launchActivity(Intent())
        performMain {
            openFilters()
        }
    }

    @Test
    fun shouldNotResetSortWhenSwitchingStateFromFilters() {
        openSortForGivenCategory(parameter.stateFrom)
        performFeedSort {
            selectSort(parameter.sortOptionFrom)
        }
        performSearchFeed {
            openParameters()
        }
        openSortForGivenCategory(parameter.stateTo)

        performFeedSort().checkResult {
            checkSortSelected(parameter.sortOptionTo)
        }
    }

    @Test
    fun shouldNotResetSortWhenSwitchingStateFromFeedSegment() {
        openSortForGivenCategory(parameter.stateFrom)
        performFeedSort {
            selectSort(parameter.sortOptionFrom)
        }
        performSearchFeed {
            selectState(parameter.stateTo)
            openSort()
        }

        performFeedSort().checkResult {
            checkSortSelected(parameter.sortOptionTo)
        }
    }

    private fun openSortForGivenCategory(stateGroup: StateGroup) {
        performFilter {
            selectCategory(VehicleCategory.CARS)
            selectState(stateGroup)
            doSearch()
        }
        performSearchFeed {
            openSort()
        }
    }

    companion object {
        private val DEFAULT_SORT_OPTION: String = getResourceString(R.string.sort_relevance)

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): Collection<Array<out Any?>> = (COMMON_OPTIONS + ONLY_USED_CARS_OPTIONS).map { arrayOf(it) }

        private val COMMON_OPTIONS = listOf(
            R.string.sort_relevance,
            R.string.sort_price_asc,
            R.string.sort_price_desc,
            R.string.sort_name
        ).map(::getResourceString).flatMap {
            listOf(
                TestParameter(StateGroup.NEW, it, StateGroup.ALL, it),
                TestParameter(StateGroup.ALL, it, StateGroup.NEW, it)
            )
        }

        private val ONLY_USED_CARS_OPTIONS = listOf(
            R.string.sort_date,
            R.string.sort_year_desc,
            R.string.sort_year_asc,
            R.string.sort_run
        ).map(::getResourceString).flatMap {
            listOf(
                TestParameter(StateGroup.ALL, it, StateGroup.NEW, DEFAULT_SORT_OPTION)
            )
        }


    }

    data class TestParameter(
        val stateFrom: StateGroup,
        val sortOptionFrom: String,
        val stateTo: StateGroup,
        val sortOptionTo: String
    )
}
