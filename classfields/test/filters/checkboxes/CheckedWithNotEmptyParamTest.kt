package ru.auto.ara.test.filters.checkboxes

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.CHECKED_CHECKBOXES_WITH_NOT_EMPTY_PARAM
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class CheckedWithNotEmptyParamTest(private val testParams: TestParameter) {
    private val WAIT_DURATION = 400L
    private val webServerRule = WebServerRule {
        delegateDispatchers(testParams.dispatchers)
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
        timeRule.setTime(time = "00:00")
        activityRule.launchActivity()
        performMain { openFilters() }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestsAfterUncheck() {
        performFilter { clickCheckBoxWithOverScroll(testParams.fieldScrollTo, testParams.fieldClickTo) }.checkResult{
            isCheckedWithOverScroll(testParams.fieldScrollTo, testParams.fieldClickTo, false)
            countWatcher.checkRequestBodyParameter(testParams.param, testParams.uncheckedParamValue)
        }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestsAfterCheck() {
        performFilter {
            clickCheckBoxWithOverScroll(testParams.fieldScrollTo, testParams.fieldClickTo)
            waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)
            timeRule.setTime(time = "00:01")
            clickCheckBoxWithOverScroll(testParams.fieldScrollTo, testParams.fieldClickTo)
        }.checkResult{
            isCheckedWithOverScroll(testParams.fieldScrollTo, testParams.fieldClickTo, true)
            countWatcher.checkRequestBodyParameter(testParams.param, testParams.checkedParamValue)
        }
    }

    companion object {
        val countWatcher = RequestWatcher()

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = CHECKED_WITH_NOT_EMPTY_PARAM.map { arrayOf(it) }

        private val CHECKED_WITH_NOT_EMPTY_PARAM = CHECKED_CHECKBOXES_WITH_NOT_EMPTY_PARAM.map { checkBoxParam ->
                TestParameter(
                    fieldScrollTo = checkBoxParam.fieldScrollTo,
                    fieldClickTo = checkBoxParam.fieldClickTo,
                    param = checkBoxParam.param,
                    checkedParamValue = checkBoxParam.checkedParamValue,
                    uncheckedParamValue = checkBoxParam.uncheckedParamValue,
                    dispatchers = listOf(CountDispatcher("cars", countWatcher))
                )
            }

        data class TestParameter(
            val fieldScrollTo: String,
            val fieldClickTo: String,
            val param: String,
            val checkedParamValue: String,
            val uncheckedParamValue: String,
            val dispatchers: List<DelegateDispatcher>
        ) {
            override fun toString() = param
        }
    }
}
