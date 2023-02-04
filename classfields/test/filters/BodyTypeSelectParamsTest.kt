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
import ru.auto.ara.core.testdata.BODY_TYPE_PARAMS
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class BodyTypeSelectParamsTest(private val testParams: TestParameter) {
    private val WAIT_DURATION = 400L
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(testParams.dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        performMain { openFilters() }
        performFilter {
            clickFieldWithOverScroll(FIELD_NAME, BODY_TYPE_FIELD_NAME)
            clickMultiSelectOptionWithScroll(testParams.name)
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
            clickFieldWithOverScroll(FIELD_NAME, BODY_TYPE_FIELD_NAME)
        }
        checkFilter { testParams.checkPicker(this) }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

    companion object {
        val watcher = RequestWatcher()
        const val FIELD_NAME = "Привод"
        const val BODY_TYPE_FIELD_NAME = "Кузов"
        const val BODY_TYPE_PARAM = "cars_params.body_type_group"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = BODY_TYPE.map { arrayOf(it) }

        private val BODY_TYPE = BODY_TYPE_PARAMS.map { bodyTypeParam ->
            TestParameter(
                name = bodyTypeParam.name,
                param = bodyTypeParam.param,
                dispatchers = listOf(CountDispatcher("cars", watcher)),
                checkAtFilterScreenAndWatcher = {
                    isContainer(BODY_TYPE_FIELD_NAME, bodyTypeParam.name)
                    watcher.checkRequestBodyArrayParameter(BODY_TYPE_PARAM, setOf(bodyTypeParam.param))
                },
                checkPicker = {
                    isCheckedOptionInMultiselectWithScrollDisplayed(bodyTypeParam.name)
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
            override fun toString() = "body_type_group=$param"
        }
    }
}
