package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FuelRateTest {
    private val WAIT_DURATION = 400L
    private val FIELD_NAME = "Расположение руля"
    private val FUEL_RATE_FIELD_NAME = "Расход до, л"
    private val FUEL_RATE_TITLE = "Расход топлива"
    private val FUEL_RATE_TO_PARAM = "fuel_rate_to"
    private val webServerRule = WebServerRule {
        getOfferCount(100, "cars")
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, FUEL_RATE_FIELD_NAME) }
    }

    @Test
    fun shouldSeeFuelRatePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(FUEL_RATE_TITLE)
            isHintAboveEditTextDisplayed()
            isSingleEditTextFieldDisplayed("")
        }
    }

    @Test
    fun shouldSeeClearButtonAfterValueChanged() {
        performFilter {
            setValueToEditText("10")
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyFuelRateValue() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameter(FUEL_RATE_TO_PARAM, "10")
            }
        }
        performFilter {
            setValueToEditText("10")
            checkFilter {
                isSingleEditTextFieldDisplayed("10")
                isAcceptButtonDisplayed()
            }
            waitButton()
            clickAcceptButton()
        }

        checkFilter {
            isFilterScreen()
            isContainer(FUEL_RATE_FIELD_NAME, "до 10")
            isDoSearchButtonWithText("Показать 50,826 предложений")
        }
    }

    @Test
    fun shouldApplyFuelRateValueWhenCloseBySwipe() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameter(FUEL_RATE_TO_PARAM, "10")
            }
        }
        performFilter {
            setValueToEditText("10")
            checkFilter {
                isSingleEditTextFieldDisplayed("10")
                isAcceptButtonDisplayed()
            }
            waitButton()
            closeDesignBottomSheetBySwipe()
        }

        checkFilter {
            isFilterScreen()
            isContainer(FUEL_RATE_FIELD_NAME, "до 10")
            isDoSearchButtonWithText("Показать 50,826 предложений")
        }
    }

    @Test
    fun shouldClearValueByClearButton() {
        performFilter {
            setValueToEditText("10")
            checkFilter { isClearButtonDisplayed() }
            waitButton()
            clickClearButton()
        }
        checkFilter { isSingleEditTextFieldDisplayed("") }
    }

    @Test
    fun shouldClearAppliedValuesByClearButton() {
        webServerRule.routing{
            getOfferCount().watch {
                checkNotRequestBodyParameter(FUEL_RATE_TO_PARAM)
            }
        }
        performFilter {
            setValueToEditText("10")
            waitButton()
            clickAcceptButton()
            checkFilter { isFilterScreen() }
            waitButton()
            clickField(FUEL_RATE_FIELD_NAME)
            waitButton()
            timeRule.setTime(time = "00:01")
            clickClearButton()
            waitButton()
            clickAcceptButton()
        }
        checkFilter {
            isFilterScreen()
            isContainer(FUEL_RATE_FIELD_NAME, "")
            isDoSearchButtonWithText("Показать 50,826 предложений")
        }
    }

    @Test
    fun shouldNotClearFuelRateByCloseIcon() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameter(FUEL_RATE_TO_PARAM, "10")
            }
        }
        performFilter {
            setValueToEditText("10")
            waitButton()
            clickAcceptButton()
            checkFilter { isFilterScreen() }
            waitButton()
            clickField(FUEL_RATE_FIELD_NAME)
            waitButton()
            clickCloseIcon()
        }
        checkFilter {
            isFilterScreen()
            isContainer(FUEL_RATE_FIELD_NAME, "до 10")
            isDoSearchButtonWithText("Показать 50,826 предложений")
        }
    }

    private fun waitButton() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

}
