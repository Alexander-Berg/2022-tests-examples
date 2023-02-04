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
import ru.auto.ara.core.testdata.TRANSMISSION_PARAMS
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TransmissionTest {
    private val WAIT_DURATION = 400L
    private val FIELD_NAME = "Мощность, л.с."
    private val TRANSMISSION_FIELD_NAME = "Коробка"
    private val TRANSMISSION_PARAM = "cars_params.transmission"
    private val TRANSMISSION_PARENT_PARAM = "cars_params"
    private val PARENT_OPTION = TRANSMISSION_PARAMS[0]
    private val FIRST_CHILD_OPTION = TRANSMISSION_PARAMS[1]
    private val SECOND_CHILD_OPTION = TRANSMISSION_PARAMS[2]
    private val THIRD_CHILD_OPTION = TRANSMISSION_PARAMS[3]
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, TRANSMISSION_FIELD_NAME) }
    }

    @Test
    fun shouldSeeTransmissionPickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isMultiselectNotCheckedOptionsDisplayed(TRANSMISSION_PARAMS.map { it.nameInBottomSheet })
            isBottomSheetTitleDisplayed(TRANSMISSION_FIELD_NAME)
        }
    }

    @Test
    fun shouldSeeClearButtonWhenValueSelected() {
        performFilter { clickMultiSelectOptionWithScroll(FIRST_CHILD_OPTION.nameInBottomSheet) }.checkResult {
            isAcceptButtonDisplayed()
            isClearButtonDisplayed()
            isCloseIconDisplayed()
        }
    }

    @Test
    fun shouldSeeCheckedChildTransmissionsAfterCheckParent() {
        performFilter { clickMultiSelectOptionWithScroll(PARENT_OPTION.nameInBottomSheet) }.checkResult {
            isCheckedOptionInMultiselectWithScrollDisplayed(PARENT_OPTION.nameInBottomSheet)
            isCheckedOptionInMultiselectWithScrollDisplayed(FIRST_CHILD_OPTION.nameInBottomSheet)
            isCheckedOptionInMultiselectWithScrollDisplayed(SECOND_CHILD_OPTION.nameInBottomSheet)
            isCheckedOptionInMultiselectWithScrollDisplayed(THIRD_CHILD_OPTION.nameInBottomSheet)
        }
    }

    @Test
    fun shouldSeeNotCheckedParentTransmissionsAfterUncheckChild() {
        performFilter {
            clickMultiSelectOptionWithScroll(PARENT_OPTION.nameInBottomSheet)
            clickMultiSelectOptionWithScroll(FIRST_CHILD_OPTION.nameInBottomSheet)
        }.checkResult {
            isNotCheckedOptionInMultiselectWithScrollDisplayed(PARENT_OPTION.nameInBottomSheet)
            isNotCheckedOptionInMultiselectWithScrollDisplayed(FIRST_CHILD_OPTION.nameInBottomSheet)
            isCheckedOptionInMultiselectWithScrollDisplayed(SECOND_CHILD_OPTION.nameInBottomSheet)
            isCheckedOptionInMultiselectWithScrollDisplayed(THIRD_CHILD_OPTION.nameInBottomSheet)
        }
    }

    @Test
    fun shouldClearValuesByClearButton() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_CHILD_OPTION.nameInBottomSheet)
            clickAcceptButton()
            waitBottomSheetBeClosed()
            checkFilter { isFilterScreen() }
            clickFieldWithOverScroll(FIELD_NAME, TRANSMISSION_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(TRANSMISSION_FIELD_NAME, "")
            countWatcher.checkNotRequestBodyParameter(TRANSMISSION_PARENT_PARAM)
        }
    }

    @Test
    fun shouldSelectMultiValues() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_CHILD_OPTION.nameInBottomSheet)
            clickMultiSelectOptionWithScroll(SECOND_CHILD_OPTION.nameInBottomSheet)
            clickAcceptButton()
        }.checkResult {
            isContainer(TRANSMISSION_FIELD_NAME, "${FIRST_CHILD_OPTION.nameInBottomSheet}, ${SECOND_CHILD_OPTION.nameInBottomSheet}")
            countWatcher.checkRequestBodyArrayParameter(
                TRANSMISSION_PARAM, setOf(FIRST_CHILD_OPTION.param.first(), SECOND_CHILD_OPTION.param.first())
            )
        }
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_CHILD_OPTION.nameInBottomSheet)
            closeDesignBottomSheetBySwipe()
        }.checkResult {
            isContainer(TRANSMISSION_FIELD_NAME, FIRST_CHILD_OPTION.nameInBottomSheet)
            countWatcher.checkRequestBodyArrayParameter(TRANSMISSION_PARAM, FIRST_CHILD_OPTION.param)
        }
    }

    @Test
    fun shouldNotApplyValueWhenClosedByCross() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_CHILD_OPTION.nameInBottomSheet)
            clickCloseIcon()
        }.checkResult {
            isContainer(TRANSMISSION_FIELD_NAME, "")
            countWatcher.checkNotRequestBodyParameter(TRANSMISSION_PARENT_PARAM)
        }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)
}
