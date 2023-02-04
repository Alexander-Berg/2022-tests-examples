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

@RunWith(AndroidJUnit4::class)
class DisplacementTest {
    private val FIELD_NAME = "Пробег, км"
    private val DISPLACEMENT_FIELD_NAME = "Объём двигателя, л"
    private val DISPLACEMENT_FROM_PARAM = "displacement_from"
    private val DISPLACEMENT_TO_PARAM = "displacement_to"
    private val watcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("cars", watcher)
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, DISPLACEMENT_FIELD_NAME) }
    }

    @Test
    fun shouldSeeDisplacementPickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(DISPLACEMENT_FIELD_NAME)
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldSeeClearButtonAfterFromValueChanged() {
        performFilter {
            setDisplacementFrom(0.4f)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldSeeClearButtonAfterToValueChanged() {
        performFilter {
            setDisplacementTo(0.4f)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyDisplacementFromValue() {
        performFilter {
            setDisplacementFrom(1.4f)
        }.checkResult {
            isNumberPickerValueDisplayed("1.4")
            isNumberPickerValueDisplayed("до")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(DISPLACEMENT_FIELD_NAME, "от 1.4 ") }
        watcher.checkRequestBodyParameter(DISPLACEMENT_FROM_PARAM, "1400")
            .checkNotRequestBodyParameter(DISPLACEMENT_TO_PARAM)
    }

    @Test
    fun shouldApplyDisplacementToValue() {
        performFilter {
            setDisplacementTo(1.8f)
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("1.8")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(DISPLACEMENT_FIELD_NAME, "до 1.8") }
        watcher.checkRequestBodyParameter(DISPLACEMENT_TO_PARAM, "1800")
            .checkNotRequestBodyParameter(DISPLACEMENT_FROM_PARAM)
    }

    @Test
    fun shouldApplyDisplacementFromToValue() {
        performFilter {
            setDisplacementFrom(1.4f)
            setDisplacementTo(1.8f)
        }.checkResult {
            isNumberPickerValueDisplayed("1.4")
            isNumberPickerValueDisplayed("1.8")
            isClearButtonDisplayed()
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(DISPLACEMENT_FIELD_NAME, "от 1.4 до 1.8") }
        watcher.checkRequestBodyParameters(DISPLACEMENT_FROM_PARAM to "1400", DISPLACEMENT_TO_PARAM to "1800")
    }

    @Test
    fun shouldNotApplyDisplacementWithUpsideDownValues() {
        performFilter {
            setDisplacementFrom(1.8f)
            setDisplacementTo(1.4f)
        }.checkResult {
            isNumberPickerValueDisplayed("1.4")
            isNumberPickerValueDisplayed("1.8")
            isClearButtonDisplayed()
        }
        performFilter {
            clickAcceptButton()
        }.checkResult {
            isNumberPickerValueDisplayed("1.4")
            isNumberPickerValueDisplayed("1.8")
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldClearFromToValuesByClearButton() {
        performFilter {
            setDisplacementFrom(1.4f)
            setDisplacementTo(1.8f)
            clickClearButton()
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldClearAppliedFromToValuesByClearButtonAfterCacheExpired() {
        performFilter {
            setDisplacementFrom(1.4f)
            setDisplacementTo(1.8f)
            clickAcceptButton()
            clickField(DISPLACEMENT_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(DISPLACEMENT_FIELD_NAME, "")
        }
        watcher.checkNotRequestBodyParameters(listOf(DISPLACEMENT_FROM_PARAM, DISPLACEMENT_TO_PARAM))
    }

    @Test
    fun shouldNotClearDisplacementByCloseIcon() {
        performFilter {
            setDisplacementFrom(1.4f)
            setDisplacementTo(1.8f)
            clickAcceptButton()
            clickField(DISPLACEMENT_FIELD_NAME)
            clickCloseIcon()
        }.checkResult {
            isContainer(DISPLACEMENT_FIELD_NAME, "от 1.4 до 1.8")
        }
        watcher.checkRequestBodyParameters(DISPLACEMENT_FROM_PARAM to "1400", DISPLACEMENT_TO_PARAM to "1800")
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            setDisplacementFrom(1.4f)
            setDisplacementTo(1.8f)
            closeDesignBottomSheetBySwipe()
        }.checkResult { isContainer(DISPLACEMENT_FIELD_NAME, "от 1.4 до 1.8") }
        watcher.checkRequestBodyParameters(DISPLACEMENT_FROM_PARAM to "1400", DISPLACEMENT_TO_PARAM to "1800")
    }
}
