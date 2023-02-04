package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(AndroidJUnit4::class)
class SellerGroupTest {
    private val FIELD_NAME = "Срок владения"
    private val FIELD_PARAM = "seller_group"
    private val ALL_TOGGLE_FIELD = "Все"
    private val COMMERCIAL_TOGGLE_FIELD = "Компания"
    private val PRIVATE_TOGGLE_FIELD = "Частник"
    private val ALL_TOGGLE_PARAM = "ANY_SELLER"
    private val COMMERCIAL_TOGGLE_PARAM = "COMMERCIAL"
    private val PRIVATE_TOGGLE_PARAM = "PRIVATE"
    private val countWatcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", countWatcher)
    )
    private val activityRule = activityScenarioRule<MainActivity>()
    private val timeRule = SetupTimeRule()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        timeRule,
        activityRule
    )

    @Before
    fun setUp() {
        performMain { openFilters() }
    }

    @Test
    fun shouldCheckCommercialToggle() {
        performFilter { clickToggleWithOverScroll(FIELD_NAME, COMMERCIAL_TOGGLE_FIELD) }
        checkFilter {
            isToggleButtonNotChecked(ALL_TOGGLE_FIELD)
            isToggleButtonChecked(COMMERCIAL_TOGGLE_FIELD)
            isToggleButtonNotChecked(PRIVATE_TOGGLE_FIELD)
        }
        countWatcher.checkRequestBodyArrayParameter(FIELD_PARAM, setOf(COMMERCIAL_TOGGLE_PARAM))
    }

    @Test
    fun shouldCheckPrivateToggle() {
        performFilter { clickToggleWithOverScroll(FIELD_NAME, PRIVATE_TOGGLE_FIELD) }
        checkFilter {
            isToggleButtonNotChecked(ALL_TOGGLE_FIELD)
            isToggleButtonNotChecked(COMMERCIAL_TOGGLE_FIELD)
            isToggleButtonChecked(PRIVATE_TOGGLE_FIELD)
        }
        countWatcher.checkRequestBodyArrayParameter(FIELD_PARAM, setOf(PRIVATE_TOGGLE_PARAM))
    }

    @Test
    fun shouldCacheCountCalls() {
        performFilter {
            clickToggleWithOverScroll(FIELD_NAME, COMMERCIAL_TOGGLE_FIELD)
            countWatcher.clearRequestWatcher()
            clickToggleWithOverScroll(FIELD_NAME, ALL_TOGGLE_FIELD)
        }
        countWatcher.checkRequestWasNotCalled()
    }

    @Test
    fun shouldCheckAllToggleAfterCacheExpired() {
        performFilter {
            clickToggleWithOverScroll(FIELD_NAME, COMMERCIAL_TOGGLE_FIELD)
            timeRule.setTime(time = "00:01")
            clickToggleWithOverScroll(FIELD_NAME, ALL_TOGGLE_FIELD)
        }
        checkFilter {
            isToggleButtonChecked(ALL_TOGGLE_FIELD)
            isToggleButtonNotChecked(COMMERCIAL_TOGGLE_FIELD)
            isToggleButtonNotChecked(PRIVATE_TOGGLE_FIELD)
        }
        countWatcher.checkRequestBodyArrayParameter(FIELD_PARAM, setOf(ALL_TOGGLE_PARAM))
    }
}
