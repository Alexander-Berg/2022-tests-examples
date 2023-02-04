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
class KmAgeTest {
    private val FIELD_NAME = "Расход до, л"
    private val KM_AGE_FIELD_NAME = "Пробег, км"
    private val KM_AGE_FROM_PARAM = "km_age_from"
    private val KM_AGE_TO_PARAM = "km_age_to"
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, KM_AGE_FIELD_NAME) }
    }

    @Test
    fun shouldSeeKmAgePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(KM_AGE_FIELD_NAME)
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldSeeClearButtonAfterFromValueChanged() {
        performFilter {
            setKmAgeFrom(10000)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldSeeClearButtonAfterToValueChanged() {
        performFilter {
            setKmAgeTo(110000)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyKmAgeFromValue() {
        performFilter {
            setKmAgeFrom(10000)
        }.checkResult {
            isNumberPickerValueDisplayed("10 000")
            isNumberPickerValueDisplayed("до")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(KM_AGE_FIELD_NAME, "от 10 000 ") }
        watcher.checkRequestBodyParameter(KM_AGE_FROM_PARAM, "10000")
        watcher.checkNotRequestBodyParameter(KM_AGE_TO_PARAM)
    }

    @Test
    fun shouldApplyKmAgeToValue() {
        performFilter {
            setKmAgeTo(110000)
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("110 000")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(KM_AGE_FIELD_NAME, "до 110 000") }
        watcher.checkRequestBodyParameter(KM_AGE_TO_PARAM, "110000")
        watcher.checkNotRequestBodyParameter(KM_AGE_FROM_PARAM)
    }

    @Test
    fun shouldApplyKmAgeFromToValue() {
        performFilter {
            setKmAgeFrom(10000)
            setKmAgeTo(110000)
        }.checkResult {
            isNumberPickerValueDisplayed("10 000")
            isNumberPickerValueDisplayed("110 000")
            isClearButtonDisplayed()
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(KM_AGE_FIELD_NAME, "от 10 000 до 110 000") }
        watcher.checkRequestBodyParameters(KM_AGE_FROM_PARAM to "10000", KM_AGE_TO_PARAM to "110000")
    }

    @Test
    fun shouldNotApplyKmAgeWithUpsideDownValues() {
        performFilter {
            setKmAgeFrom(110000)
            setKmAgeTo(10000)
        }.checkResult {
            isNumberPickerValueDisplayed("10 000")
            isNumberPickerValueDisplayed("110 000")
            isClearButtonDisplayed()
        }
        performFilter {
            clickAcceptButton()
        }.checkResult {
            isNumberPickerValueDisplayed("10 000")
            isNumberPickerValueDisplayed("110 000")
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldClearFromToValuesByClearButton() {
        performFilter {
            setKmAgeFrom(10000)
            setKmAgeTo(110000)
            clickClearButton()
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldClearAppliedFromToValuesByClearButtonAfterCacheExpired() {
        performFilter {
            setKmAgeFrom(10000)
            setKmAgeTo(110000)
            clickAcceptButton()
            clickField(KM_AGE_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(KM_AGE_FIELD_NAME, "")
        }
        watcher.checkNotRequestBodyParameters(listOf(KM_AGE_FROM_PARAM, KM_AGE_TO_PARAM))
    }

    @Test
    fun shouldNotClearYearByCloseIcon() {
        performFilter {
            setKmAgeFrom(10000)
            setKmAgeTo(110000)
            clickAcceptButton()
            clickField(KM_AGE_FIELD_NAME)
            clickCloseIcon()
        }.checkResult {
            isContainer(KM_AGE_FIELD_NAME, "от 10 000 до 110 000")
        }
        watcher.checkRequestBodyParameters(KM_AGE_FROM_PARAM to "10000", KM_AGE_TO_PARAM to "110000")
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            setKmAgeFrom(10000)
            setKmAgeTo(110000)
            closeDesignBottomSheetBySwipe()
        }.checkResult { isContainer(KM_AGE_FIELD_NAME, "от 10 000 до 110 000") }
        watcher.checkRequestBodyParameters(KM_AGE_FROM_PARAM to "10000", KM_AGE_TO_PARAM to "110000")
    }
}
