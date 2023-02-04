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
import ru.auto.ara.core.testdata.OWNING_TIME_PARAMS
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class OwningTimeSelectParamsTest(private val testParams: TestParameter) {
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
            clickFieldWithHintWithOverScroll(FIELD_NAME, OWNING_TIME_FIELD_NAME)
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
        performFilter { clickFieldWithHintWithOverScroll(FIELD_NAME, OWNING_TIME_FIELD_NAME) }
        checkFilter { testParams.checkPicker(this) }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

    companion object {
        val watcher = RequestWatcher()
        const val FIELD_NAME = "Таможня"
        const val OWNING_TIME_FIELD_NAME = "Срок владения"
        const val OWNING_TIME_PARAM = "owning_time_group"
        const val DEFAULT_OWNING_TIME_NAME = "Неважно"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = OWNING_TIME.map { arrayOf(it) }

        private val OWNING_TIME = OWNING_TIME_PARAMS.map { (nameInBottomSheet, nameInListing, param) ->
            TestParameter(
                name = nameInBottomSheet,
                param = param,
                dispatchers = listOf(CountDispatcher("cars", watcher)),
                checkAtFilterScreenAndWatcher = {
                    isInputContainer(OWNING_TIME_FIELD_NAME, nameInListing)
                    if (nameInBottomSheet == DEFAULT_OWNING_TIME_NAME) {
                        watcher.checkNotRequestBodyParameter(OWNING_TIME_PARAM)
                    } else {
                        watcher.checkRequestBodyParameter(OWNING_TIME_PARAM, param)
                    }
                },
                checkPicker = {
                    OWNING_TIME_PARAMS.map { (optionName) ->
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
            override fun toString() = "owning_time_group=$param"
        }
    }
}
