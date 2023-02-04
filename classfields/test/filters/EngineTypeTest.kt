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
import ru.auto.ara.core.testdata.ENGINE_TYPE_PARAMS
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EngineTypeTest {
    private val WAIT_DURATION = 400L
    private val FIELD_NAME = "Мощность, л.с."
    private val ENGINE_FIELD_NAME = "Двигатель"
    private val ENGINE_PARAM = "cars_params.engine_group"
    private val ENGINE_PARENT_PARAM = "cars_params"
    private val FIRST_OPTION = ENGINE_TYPE_PARAMS[0]
    private val SECOND_OPTION = ENGINE_TYPE_PARAMS[1]
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, ENGINE_FIELD_NAME) }
    }

    @Test
    fun shouldSeeEngineTypePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isMultiselectNotCheckedOptionsDisplayed(ENGINE_TYPE_PARAMS.map { it.first() })
            isBottomSheetTitleDisplayed(ENGINE_FIELD_NAME)
        }
    }

    @Test
    fun shouldSeeClearButtonWhenValueSelected() {
        performFilter { clickMultiSelectOptionWithScroll(FIRST_OPTION[0]) }.checkResult {
            isAcceptButtonDisplayed()
            isClearButtonDisplayed()
            isCloseIconDisplayed()
        }
    }

    @Test
    fun shouldClearValuesByClearButton() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION[0])
            clickAcceptButton()
            waitBottomSheetBeClosed()
            checkFilter { isFilterScreen() }
            clickFieldWithOverScroll(FIELD_NAME, ENGINE_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(ENGINE_FIELD_NAME, "")
            countWatcher.checkNotRequestBodyParameter(ENGINE_PARENT_PARAM)
        }
    }

    @Test
    fun shouldSelectMultiValues() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION[0])
            clickMultiSelectOptionWithScroll(SECOND_OPTION[0])
            clickAcceptButton()
        }.checkResult {
            isContainer(ENGINE_FIELD_NAME, "${FIRST_OPTION[0]}, ${SECOND_OPTION[0]}")
            countWatcher.checkRequestBodyArrayParameter(ENGINE_PARAM, setOf(FIRST_OPTION[1], SECOND_OPTION[1]))
        }
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION[0])
            closeDesignBottomSheetBySwipe()
        }.checkResult {
            isContainer(ENGINE_FIELD_NAME, FIRST_OPTION[0])
            countWatcher.checkRequestBodyArrayParameter(ENGINE_PARAM, setOf(FIRST_OPTION[1]))
        }
    }

    @Test
    fun shouldNotApplyValueWhenClosedByCross() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION[0])
            clickCloseIcon()
        }.checkResult {
            isContainer(ENGINE_FIELD_NAME, "")
            countWatcher.checkNotRequestBodyParameter(ENGINE_PARENT_PARAM)
        }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)
}
