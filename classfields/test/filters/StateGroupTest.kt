package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class StateGroupTest {
    private val FIELD_NAME = "Год выпуска"
    private val FIELD_PARAM = "state_group"
    private val ALL_TOGGLE_FIELD = "Все"
    private val NEW_TOGGLE_FIELD = "Новые"
    private val USED_TOGGLE_FIELD = "С пробегом"
    private val ALL_TOGGLE_PARAM = "ALL"
    private val NEW_TOGGLE_PARAM = "NEW"
    private val USED_TOGGLE_PARAM = "USED"
    private val countWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("cars", countWatcher)
        )
    }
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val timeRule = SetupTimeRule()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        timeRule,
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        performMain { openFilters() }
    }

    @Test
    fun shouldCheckNewToggle() {
        performFilter { clickToggleWithOverScroll(FIELD_NAME, NEW_TOGGLE_FIELD) }
        checkFilter {
            isToggleButtonNotChecked(ALL_TOGGLE_FIELD)
            isToggleButtonChecked(NEW_TOGGLE_FIELD)
            isToggleButtonNotChecked(USED_TOGGLE_FIELD)
        }
        countWatcher.checkRequestBodyParameter(FIELD_PARAM, NEW_TOGGLE_PARAM)
    }

    @Test
    fun shouldCheckUsedToggle() {
        performFilter { clickToggleWithOverScroll(FIELD_NAME, USED_TOGGLE_FIELD) }
        checkFilter {
            isToggleButtonNotChecked(ALL_TOGGLE_FIELD)
            isToggleButtonNotChecked(NEW_TOGGLE_FIELD)
            isToggleButtonChecked(USED_TOGGLE_FIELD)
        }
        countWatcher.checkRequestBodyParameter(FIELD_PARAM, USED_TOGGLE_PARAM)
    }

    @Test
    fun shouldCacheCountCall() {
        performFilter {
            clickToggleWithOverScroll(FIELD_NAME, NEW_TOGGLE_FIELD)
            countWatcher.clearRequestWatcher()
            clickToggleWithOverScroll(FIELD_NAME, ALL_TOGGLE_FIELD)
        }
        countWatcher.checkRequestWasNotCalled()
    }

    @Test
    fun shouldCheckAllToggleAfterCountCacheExpired() {
        performFilter {
            clickToggleWithOverScroll(FIELD_NAME, NEW_TOGGLE_FIELD)
            timeRule.setTime(time = "00:01")
            clickToggleWithOverScroll(FIELD_NAME, ALL_TOGGLE_FIELD)
        }
        checkFilter {
            isToggleButtonChecked(ALL_TOGGLE_FIELD)
            isToggleButtonNotChecked(NEW_TOGGLE_FIELD)
            isToggleButtonNotChecked(USED_TOGGLE_FIELD)
        }
        countWatcher.checkRequestBodyParameter(FIELD_PARAM, ALL_TOGGLE_PARAM)
    }
}
