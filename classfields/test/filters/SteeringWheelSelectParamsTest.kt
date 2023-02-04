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
import ru.auto.ara.core.testdata.STEERING_WHEEL_PARAMS
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class SteeringWheelSelectParamsTest(private val testParams: TestParameter) {
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
            clickFieldWithHintWithOverScroll(FIELD_NAME, STEERING_WHEEL_FIELD_NAME)
            clickOption(testParams.name)
        }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestsAtFilterScreen() {
        checkFilter { testParams.checkAtFilterScreenAndWatcher(this) }
    }

    @Test
    fun shouldSeeCorrectStateAtBottomSheet() {
        waitBottomSheetBeClosed()
        performFilter {
            clickFieldWithHintWithOverScroll(
                FIELD_NAME,
                STEERING_WHEEL_FIELD_NAME
            )
        }
        checkFilter { testParams.checkPicker(this) }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

    companion object {
        val watcher = RequestWatcher()
        const val FIELD_NAME = "Срок владения"
        const val STEERING_WHEEL_FIELD_NAME = "Расположение руля"
        const val STEERING_WHEEL_PARAM = "cars_params.steering_wheel"
        const val STEERING_WHEEL_PARENT_PARAM = "cars_params"
        const val DEFAULT_STEERING_WHEEL_NAME = "Любой руль"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = STEERING_WHEELS.map { arrayOf(it) }

        private val STEERING_WHEELS = STEERING_WHEEL_PARAMS.map { (nameInBottomSheet, nameInListing, param) ->
            TestParameter(
                name = nameInBottomSheet,
                param = param,
                dispatchers = listOf(CountDispatcher("cars", watcher)),
                checkAtFilterScreenAndWatcher = {
                    isInputContainer(STEERING_WHEEL_FIELD_NAME, nameInListing)
                    if (nameInBottomSheet == DEFAULT_STEERING_WHEEL_NAME) {
                        watcher.checkNotRequestBodyParameter(STEERING_WHEEL_PARENT_PARAM)
                    } else {
                        watcher.checkRequestBodyParameter(STEERING_WHEEL_PARAM, param)
                    }
                },
                checkPicker = {
                    STEERING_WHEEL_PARAMS.map { (optionName) ->
                        if (optionName == nameInBottomSheet) {
                            isCheckedOptionDisplayed(optionName)
                        } else {
                            isNotCheckedOptionDisplayed(optionName)
                        }
                    }
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
            override fun toString() = "steeringWheel=$param"
        }
    }
}
