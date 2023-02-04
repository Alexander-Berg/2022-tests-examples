package ru.auto.ara.test.filters

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.FilterRobotChecker
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.COLOR_PARAMS
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class ColorSelectParamsTest(private val testParams: TestParameter) {
    private val WAIT_DURATION = 400L
    private val activityRule = activityScenarioRule<MainActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(testParams.dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        performMain { openFilters() }
        performFilter {
            clickFieldWithOverScroll(FIELD_NAME, COLOR_FIELD_NAME)
            clickColorOptionWithScroll(testParams.name)
        }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestsAtFilterScreen() {
        checkFilter { testParams.checkPicker(this) }
        performFilter { clickAcceptButton() }.checkResult {
            testParams.checkAtFilterScreenAndWatcher(this)
        }
    }

    @Test
    fun shouldSeeCorrectStateAtBottomSheet() {
        waitBottomSheetBeClosed()
        performFilter {
            clickAcceptButton()
            clickFieldWithOverScroll(FIELD_NAME, COLOR_FIELD_NAME)
        }
        checkFilter { testParams.checkPicker(this) }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

    companion object {
        val countWatcher = RequestWatcher()
        const val FIELD_NAME = "Владельцев по ПТС"
        const val COLOR_FIELD_NAME = "Цвет"
        const val COLOR_PARAM = "color"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = COLOR.map { arrayOf(it) }

        private val COLOR = COLOR_PARAMS.map { (name, param) ->
            TestParameter(
                name = name,
                param = param,
                dispatchers = listOf(CountDispatcher("cars", countWatcher)),
                checkAtFilterScreenAndWatcher = {
                    isContainer(COLOR_FIELD_NAME, name)
                    countWatcher.checkRequestBodyArrayParameter(COLOR_PARAM, setOf(param))
                },
                checkPicker = {
                    isCheckedColorOptionWithScrollDisplayed(name)
                }
            )
        }

        data class TestParameter(
            val name: String,
            val param: String,
            val dispatchers: List<DelegateDispatcher>,
            val checkAtFilterScreenAndWatcher: FilterRobotChecker.() -> Unit,
            val checkPicker: FilterRobotChecker.() -> Unit
        ) {
            override fun toString() = param
        }
    }
}
