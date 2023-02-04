package ru.auto.ara.test.filters.geo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class ClearTest {
    private val RID_REQUEST_PARAMETER = "rid"

    private val watcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", watcher)
    )

    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    private val timeRule = SetupTimeRule()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        timeRule,
        activityTestRule
    )

    @Before
    fun setUp() {
        timeRule.setTime(time = "00:00")
        activityTestRule.launchActivity()
        performMain { openFilters() }
        performFilter { clickRegionFilter() }
    }

    @Test
    fun shouldClearSelectedRegions() {
        val indexRegion = 8
        performMultiGeo { clickCheckBoxWithIndex(indexRegion) }.checkResult { isGeoItemWithIndexChecked(indexRegion) }
        timeRule.setTime(time = "00:01")
        performMultiGeo { clickClearButton() }.checkResult { isGeoItemWithIndexNotChecked(indexRegion) }
        watcher.checkNotRequestBodyParameter(RID_REQUEST_PARAMETER)
    }

    @Test
    fun shouldClearRegionSelectedFromExpandedList() {
        val indexRegion = 8
        val indexCity = 9
        performMultiGeo {
            clickExpandArrowWithGeoTitle("Санкт-Петербург и Ленинградская область")
            clickCheckBoxWithIndex(indexCity)
            timeRule.setTime(time = "00:01")
            clickClearButton()
        }.checkResult {
            isGeoItemWithIndexNotChecked(indexCity)
            isSelectedRegionsCountDisplayed(indexRegion, "")
        }
        watcher.checkNotRequestBodyParameter(RID_REQUEST_PARAMETER)
    }
}
