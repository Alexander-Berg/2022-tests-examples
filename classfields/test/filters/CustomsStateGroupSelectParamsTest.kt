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
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.CUSTOMS_STATE_GROUP_PARAMS
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class CustomsStateGroupSelectParamsTest(private val testParams: TestParameter) {
    private val WAIT_DURATION = 400L
    private val webServerRule = WebServerRule {
        delegateDispatchers(testParams.dispatchers)
    }
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        performMain { openFilters() }
        performFilter {
            clickFieldWithHintWithOverScroll(FIELD_NAME, CUSTOMS_STATE_GROUP_FIELD_NAME)
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
        performFilter { clickFieldWithHintWithOverScroll(FIELD_NAME, CUSTOMS_STATE_GROUP_FIELD_NAME) }
        checkFilter { testParams.checkPicker(this) }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

    companion object {
        val countWatcher = RequestWatcher()
        const val FIELD_NAME = "Только с фото"
        const val CUSTOMS_STATE_GROUP_FIELD_NAME = "Таможня"
        const val CUSTOMS_STATE_GROUP_PARAM = "customs_state_group"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = CUSTOMS_STATE_GROUP.map { arrayOf(it) }

        private val CUSTOMS_STATE_GROUP = CUSTOMS_STATE_GROUP_PARAMS.map { (name, param) ->
            TestParameter(
                name = name,
                param = param,
                dispatchers = listOf(CountDispatcher("cars", countWatcher)),
                checkAtFilterScreenAndWatcher = {
                    isInputContainer(CUSTOMS_STATE_GROUP_FIELD_NAME, name)
                    countWatcher.checkRequestBodyParameter(CUSTOMS_STATE_GROUP_PARAM, param)
                },
                checkPicker = {
                    CUSTOMS_STATE_GROUP_PARAMS.map { (optionName) ->
                        if (optionName == name) {
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
            override fun toString() = "customs_state_group=$param"
        }
    }
}
