package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
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
import ru.auto.ara.core.utils.getResourceString

@RunWith(AndroidJUnit4::class)
class YearFromToTest {
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
        timeRule.setTime(CURRENT_DATE)
        performMain { openFilters() }
        performFilter { clickField(getResourceString(R.string.field_year_label)) }
    }

    @Test
    fun shouldSeeDatePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(R.string.field_year_label)
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldSeeClearButtonAfterFromValueChanged() {
        performFilter {
            setYearFrom(1940)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldSeeClearButtonAfterToValueChanged() {
        performFilter {
            setYearTo(2000)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyDateFromValue() {
        performFilter {
            setYearFrom(1940)
        }.checkResult {
            isNumberPickerValueDisplayed("1940")
            isNumberPickerValueDisplayed("до")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(R.string.field_year_label, "от 1940 ") }
        watcher.checkRequestBodyParameter("year_from", "1940")
        watcher.checkNotRequestBodyParameter("year_to")
    }

    @Test
    fun shouldApplyDateToValue() {
        performFilter {
            setYearTo(2000)
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("2000")
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(R.string.field_year_label, "до 2000") }
        watcher.checkRequestBodyParameter("year_to", "2000")
        watcher.checkNotRequestBodyParameter("year_from")
    }

    @Test
    fun shouldApplyDateFromToValue() {
        performFilter {
            setYearFrom(1940)
            setYearTo(2000)
        }.checkResult {
            isNumberPickerValueDisplayed("1940")
            isNumberPickerValueDisplayed("2000")
            isClearButtonDisplayed()
        }
        performFilter { clickAcceptButton() }
            .checkResult { isContainer(R.string.field_year_label, "от 1940 до 2000") }
        watcher.checkRequestBodyParameters("year_from" to "1940", "year_to" to "2000")
    }

    @Test
    fun shouldNotApplyDateWithUpsideDownValues() {
        performFilter {
            setYearFrom(2000)
            setYearTo(1940)
        }.checkResult {
            isNumberPickerValueDisplayed("1940")
            isNumberPickerValueDisplayed("2000")
            isClearButtonDisplayed()
        }
        performFilter {
            clickAcceptButton()
        }.checkResult {
            isNumberPickerValueDisplayed("1940")
            isNumberPickerValueDisplayed("2000")
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldClearFromToValuesByClearButton() {
        performFilter {
            setYearFrom(1940)
            setYearTo(2000)
            clickClearButton()
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldClearAppliedFromToValuesByClearButtonAfterCountCacheExpired() {
        performFilter {
            setYearFrom(1940)
            setYearTo(2000)
            clickAcceptButton()
            clickField(getResourceString(R.string.field_year_label))
            timeRule.setTime(CURRENT_DATE,time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(getResourceString(R.string.field_year_label), "")
        }
        watcher.checkNotRequestBodyParameters(listOf("year_from", "year_to"))
    }

    @Test
    fun shouldNotCallCountersAPIIfItIsCached() {
        performFilter {
            setYearFrom(1940)
            setYearTo(2000)
            clickAcceptButton()
            clickField(getResourceString(R.string.field_year_label))
            watcher.clearRequestWatcher()
            clickClearButton()
            clickAcceptButton()
        }
        watcher.checkRequestWasNotCalled()
    }

    @Test
    fun shouldNotClearYearByCloseIcon() {
        performFilter {
            setYearFrom(1940)
            setYearTo(2000)
            clickAcceptButton()
            clickField(getResourceString(R.string.field_year_label))
            clickCloseIcon()
        }.checkResult {
            isContainer(R.string.field_year_label, "от 1940 до 2000")
        }
        watcher.checkRequestBodyParameters("year_from" to "1940", "year_to" to "2000")
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            setYearFrom(1940)
            setYearTo(2000)
            closeDesignBottomSheetBySwipe()
        }.checkResult { isContainer(R.string.field_year_label, "от 1940 до 2000") }
        watcher.checkRequestBodyParameters("year_from" to "1940", "year_to" to "2000")
    }

    companion object {
        private const val CURRENT_DATE = "10.10.2020"
    }
}
