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
import ru.auto.ara.core.testdata.TRANSMISSION_PARAMS
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class TransmissionSelectParamsTest(private val testParams: TestParameter) {
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
            clickFieldWithOverScroll(FIELD_NAME, TRANSMISSION_FIELD_NAME)
            clickMultiSelectOptionWithScroll(testParams.nameInBottomSheet)
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
            clickFieldWithOverScroll(FIELD_NAME, TRANSMISSION_FIELD_NAME)
        }
        checkFilter { testParams.checkPicker(this) }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

    companion object {
        val countWatcher = RequestWatcher()
        private const val FIELD_NAME = "Мощность, л.с."
        private const val TRANSMISSION_FIELD_NAME = "Коробка"
        private const val TRANSMISSION_PARAM = "cars_params.transmission"


        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = TRANSMISSION.map { arrayOf(it) }

        private val TRANSMISSION = TRANSMISSION_PARAMS.map { transmissionParam ->
            TestParameter(
                nameInBottomSheet = transmissionParam.nameInBottomSheet,
                param = transmissionParam.param,
                nameInFilters = transmissionParam.nameInFilters,
                description = transmissionParam.description,
                dispatchers = listOf(CountDispatcher("cars", countWatcher)),
                checkAtFilterScreenAndWatcher = {
                    isContainer(TRANSMISSION_FIELD_NAME, transmissionParam.nameInFilters)
                    countWatcher.checkRequestBodyArrayParameter(TRANSMISSION_PARAM, transmissionParam.param)
                },
                checkPicker = {
                    isCheckedOptionInMultiselectWithScrollDisplayed(transmissionParam.nameInBottomSheet)
                }
            )
        }

        data class TestParameter(
            val nameInBottomSheet: String,
            val nameInFilters: String,
            val param: Set<String>,
            val description: String,
            val dispatchers: List<DelegateDispatcher>,
            val checkAtFilterScreenAndWatcher: FilterRobotChecker.() -> Unit,
            val checkPicker: FilterRobotChecker.() -> Unit
        ) {
            override fun toString() = description
        }
    }
}
