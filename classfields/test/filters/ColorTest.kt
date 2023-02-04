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
import ru.auto.ara.core.testdata.COLOR_PARAMS
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ColorTest {
    private val WAIT_DURATION = 400L
    private val FIELD_NAME = "Владельцев по ПТС"
    private val COLOR_NAME = "Цвет"
    private val COLOR_PARAM = "color"
    private val FIRST_OPTION = COLOR_PARAMS[0]
    private val SECOND_OPTION = COLOR_PARAMS[1]
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, COLOR_NAME) }
    }

    @Test
    fun shouldSeeEngineTypePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isNotCheckedColorOptionsDisplayed(COLOR_PARAMS.map { it.first() })
            isBottomSheetTitleDisplayed(COLOR_NAME)
        }
    }

    @Test
    fun shouldSeeClearButtonWhenValueSelected() {
        performFilter { clickColorOptionWithScroll(FIRST_OPTION[0]) }.checkResult {
            isAcceptButtonDisplayed()
            isClearButtonDisplayed()
            isCloseIconDisplayed()
        }
    }

    @Test
    fun shouldClearValuesByClearButton() {
        performFilter {
            clickColorOptionWithScroll(FIRST_OPTION[0])
            clickAcceptButton()
            waitBottomSheetBeClosed()
            checkFilter { isFilterScreen() }
            clickFieldWithOverScroll(FIELD_NAME, COLOR_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(COLOR_NAME, "")
            countWatcher.checkNotRequestBodyParameter(COLOR_PARAM)
        }
    }

    @Test
    fun shouldSelectMultiValues() {
        performFilter {
            clickColorOptionWithScroll(FIRST_OPTION[0])
            clickColorOptionWithScroll(SECOND_OPTION[0])
            clickAcceptButton()
        }.checkResult {
            isContainer(COLOR_NAME, "${FIRST_OPTION[0]}, ${SECOND_OPTION[0]}")
            countWatcher.checkRequestBodyArrayParameter(COLOR_PARAM, setOf(FIRST_OPTION[1], SECOND_OPTION[1]))
        }
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            clickColorOptionWithScroll(FIRST_OPTION[0])
            closeListBottomsheetWithSwipe()
        }.checkResult {
            isContainer(COLOR_NAME, FIRST_OPTION[0])
            countWatcher.checkRequestBodyArrayParameter(COLOR_PARAM, setOf(FIRST_OPTION[1]))
        }
    }

    @Test
    fun shouldNotApplyValueWhenClosedByCross() {
        performFilter {
            clickColorOptionWithScroll(FIRST_OPTION[0])
            clickCloseIcon()
        }.checkResult {
            isContainer(COLOR_NAME, "")
            countWatcher.checkNotRequestBodyParameter(COLOR_PARAM)
        }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)
}
