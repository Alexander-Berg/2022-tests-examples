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
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class AccelerationTest {
    private val FIELD_NAME = "Объем багажника от, л"
    private val ACCELERATION_FIELD_NAME = "Разгон до 100 км/ч, с"
    private val ACCELERATION_FROM_PARAM = "acceleration_from"
    private val ACCELERATION_TO_PARAM = "acceleration_to"
    private val watcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", watcher)
    )
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
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
        activityRule.launchActivity()
        performMain { openFilters() }
        performFilter { clickFieldWithOverScroll(FIELD_NAME, ACCELERATION_FIELD_NAME) }
    }

    @Test
    fun shouldSeeAccelerationPickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(ACCELERATION_FIELD_NAME)
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldSeeClearButtonAfterFromValueChanged() {
        performFilter {
            setAccelerationFrom(5)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldSeeClearButtonAfterToValueChanged() {
        performFilter {
            setAccelerationTo(15)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyAccelerationFromValue() {
        performFilter {
            setAccelerationFrom(5)
        }.checkResult {
            isNumberPickerValueDisplayed("5")
            isNumberPickerValueDisplayed("до")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(ACCELERATION_FIELD_NAME, "от 5 ") }
        watcher.checkRequestBodyParameter(ACCELERATION_FROM_PARAM, "5")
        watcher.checkNotRequestBodyParameter(ACCELERATION_TO_PARAM)
    }

    @Test
    fun shouldApplyAccelerationToValue() {
        performFilter {
            setAccelerationTo(15)
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("15")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(ACCELERATION_FIELD_NAME, "до 15") }
        watcher.checkRequestBodyParameter(ACCELERATION_TO_PARAM, "15")
        watcher.checkNotRequestBodyParameter(ACCELERATION_FROM_PARAM)
    }

    @Test
    fun shouldApplyAccelerationFromToValue() {
        performFilter {
            setAccelerationFrom(5)
            setAccelerationTo(15)
        }.checkResult {
            isNumberPickerValueDisplayed("5")
            isNumberPickerValueDisplayed("15")
            isClearButtonDisplayed()
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(ACCELERATION_FIELD_NAME, "от 5 до 15") }
        watcher.checkRequestBodyParameters(ACCELERATION_FROM_PARAM to "5", ACCELERATION_TO_PARAM to "15")
    }

    @Test
    fun shouldNotApplyAccelerationWithUpsideDownValues() {
        performFilter {
            setAccelerationFrom(15)
            setAccelerationTo(5)
        }.checkResult {
            isNumberPickerValueDisplayed("5")
            isNumberPickerValueDisplayed("15")
            isClearButtonDisplayed()
        }
        performFilter {
            clickAcceptButton()
        }.checkResult {
            isNumberPickerValueDisplayed("5")
            isNumberPickerValueDisplayed("15")
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldClearFromToValuesByClearButton() {
        performFilter {
            setAccelerationFrom(5)
            setAccelerationTo(15)
            clickClearButton()
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldClearAppliedFromToValuesByClearButtonAfterCacheExpired() {
        performFilter {
            setAccelerationFrom(5)
            setAccelerationTo(15)
            clickAcceptButton()
            clickField(ACCELERATION_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(ACCELERATION_FIELD_NAME, "")
        }
        watcher.checkNotRequestBodyParameters(listOf(ACCELERATION_FROM_PARAM, ACCELERATION_TO_PARAM))
    }

    @Test
    fun shouldNotClearYearByCloseIcon() {
        performFilter {
            setAccelerationFrom(5)
            setAccelerationTo(15)
            clickAcceptButton()
            clickField(ACCELERATION_FIELD_NAME)
            clickCloseIcon()
        }.checkResult {
            isContainer(ACCELERATION_FIELD_NAME, "от 5 до 15")
        }
        watcher.checkRequestBodyParameters(ACCELERATION_FROM_PARAM to "5", ACCELERATION_TO_PARAM to "15")
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            setAccelerationFrom(5)
            setAccelerationTo(15)
            closeDesignBottomSheetBySwipe()
        }.checkResult { isContainer(ACCELERATION_FIELD_NAME, "от 5 до 15") }
        watcher.checkRequestBodyParameters(ACCELERATION_FROM_PARAM to "5", ACCELERATION_TO_PARAM to "15")
    }
}
