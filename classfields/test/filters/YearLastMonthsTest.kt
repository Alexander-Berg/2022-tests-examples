package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.util.Clock

@RunWith(AndroidJUnit4::class)
class YearLastMonthsTest {
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val timeRule = SetupTimeRule()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule(),
        timeRule,
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
    }

    @After
    fun doAfter(){
        Clock.impl = Clock.System
    }

    @Test
    fun shouldAllowSelectNextYearIfNovemberOrDecember() {
        timeRule.setTime("01.11.2021", "22:00")
        performMain { openFilters() }
        performFilter {
            clickField(getResourceString(R.string.field_year_label))
            waitBottomSheet()
            setYearFrom(2020)
            setYearTo(2022)
        }.checkResult {
            isNumberPickerValueDisplayed("2020")
            isNumberPickerValueDisplayed("2022")
        }
    }

    @Test
    fun shouldNotAllowSelectNextYearIfNotNovemberOrDecember() {
        timeRule.setTime("05.10.2021", "22:00")
        performMain { openFilters() }
        performFilter {
            clickField(getResourceString(R.string.field_year_label))
            waitBottomSheet()
            interactions.onNumberPickerValue("2022")
                .waitUntilIsNotExist()
            setYearFrom(2020)
            setYearTo(2021)
        }.checkResult {
            isNumberPickerValueDisplayed("2020")
            isNumberPickerValueDisplayed("2021")
        }
    }
}
