package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class ClearAllTest {

    private val webServerRule = WebServerRule {
        delegateDispatcher(CountDispatcher("cars"))
    }
    private val activityTestRule = activityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun setUp() {
        performMain { openFilters() }
        performFilter {
            selectState(StateGroup.NEW)
            clearAll()
        }
    }

    @Test
    fun shouldClearAll() {
        performFilter {
            clickYes()
        }.checkResult {
            isToggleButtonChecked(R.string.all)
        }
    }

    @Test
    fun shouldCancelClearAll() {
        performFilter {
            clickNo()
        }.checkResult {
            isToggleButtonChecked(R.string.new_vehicles)
        }
    }
}
