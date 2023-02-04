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

/**
 * @author themishkun on 06/02/2019.
 */

@RunWith(Parameterized::class)
class FeedSortSaveInCategoryTest(private val testParams: TestParameter) {

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            PostSearchOffersDispatcher.getGenericFeed()
        )
    }
    private val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule
    @JvmField
    val ruleChain = baseRuleChain(
        webServerRule,
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
    fun shouldSaveSortByCategory() {
        selectCategoryAndOpenSort(testParams.category)
        performFeedSort {
            selectSort(DUMMY_SORT_1)
        }
        performSearchFeed {
            openParameters()
        }
        selectCategoryAndOpenSort(testParams.switchCategory)
        performFeedSort {
            selectSort(DUMMY_SORT_2)
        }
        performSearchFeed {
            openParameters()
        }
        selectCategoryAndOpenSort(testParams.category)
        performFeedSort().checkResult {
            checkSortSelected(DUMMY_SORT_1)
        }
    }

    private fun selectCategoryAndOpenSort(vehicleCategory: VehicleCategory) {
        performFilter {
            selectCategory(vehicleCategory)
            doSearch()
        }
        performSearchFeed {
            openSort()
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = listOf(
            TestParameter(category = VehicleCategory.CARS, switchCategory = VehicleCategory.MOTO),
            TestParameter(category = VehicleCategory.MOTO, switchCategory = VehicleCategory.TRUCKS),
            TestParameter(category = VehicleCategory.TRUCKS, switchCategory = VehicleCategory.CARS)
        ).map { arrayOf(it) }

        private val DUMMY_SORT_1 = getResourceString(R.string.sort_name)
        private val DUMMY_SORT_2 = getResourceString(R.string.sort_run)

        data class TestParameter(
            val category: VehicleCategory,
            val switchCategory: VehicleCategory
        ) {
            override fun toString(): String = "$category by $switchCategory"
        }
    }
}
