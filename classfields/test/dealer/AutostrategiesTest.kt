package ru.auto.ara.test.dealer

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.AutoApplication
import ru.auto.ara.core.robot.dealerslk.performAutostrategy
import ru.auto.ara.core.rules.DynamicRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchAutostrategiesFragment
import ru.auto.ara.ui.activity.SimpleSecondLevelActivity


@RunWith(AndroidJUnit4::class)
class AutostrategiesTest {

    val activityTestRule = lazyActivityScenarioRule<SimpleSecondLevelActivity>()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        WebServerRule(),
        activityTestRule,
        DynamicRule(afterRule = { AutoApplication.COMPONENT_MANAGER.clearAutostrategiesFactory() }),
    )

    @Before
    fun before() {
        activityTestRule.launchAutostrategiesFragment()
        performAutostrategy {}.waitAutostrategies()
    }

    @Test
    fun shouldSelectStartDate() {
        performAutostrategy {
            openStartDate()
            selectDateToday()
        }.checkResult {
            isStartDateSelectedToday()
        }
    }

    @Test
    fun shouldSelectEndDate() {
        performAutostrategy {
            openEndDate()
            selectDateToday()
        }.checkResult {
            isEndDateSelectedToday()
        }
    }

    @Test
    fun shouldClearEndDateIfStartingDateIsGreater() {
        performAutostrategy {
            openEndDate()
            selectDateToday()
            openStartDate()
            selectDateNextMonth(NEXT_MONTH_SELECT_DAY)
        }.checkResult {
            isEndDateNotSelected()
        }
    }

    @Test
    fun shouldContainCountAndPriceInDailyLimitSelection() {
        performAutostrategy {
            openDailyLimits()
            selectFirstItem()
        }.checkResult {
            isDailyLimitSelected()
        }
    }

}

private const val NEXT_MONTH_SELECT_DAY = 5
