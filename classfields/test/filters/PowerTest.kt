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

@RunWith(AndroidJUnit4::class)
class PowerTest {
    private val FIELD_NAME = "Расход до, л"
    private val POWER_FIELD_NAME = "Мощность, л.с."
    private val POWER_FROM_PARAM = "power_from"
    private val POWER_TO_PARAM = "power_to"
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val timeRule = SetupTimeRule()
    private val webServerRule = WebServerRule { getOfferCount(100, "cars") }
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
        performMain {
            openTransportTab()
            openFilters()
        }
        performFilter { clickFieldWithOverScroll(FIELD_NAME, POWER_FIELD_NAME) }
    }

    @Test
    fun shouldSeePowerPickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(POWER_FIELD_NAME)
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldSeeClearButtonAfterFromValueChanged() {
        performFilter {
            setPowerFrom(100)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldSeeClearButtonAfterToValueChanged() {
        performFilter {
            setPowerTo(200)
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyPowerFromValue() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameter(POWER_FROM_PARAM, "100")
                checkNotRequestBodyParameter(POWER_TO_PARAM)
            }
        }
        performFilter {
            setPowerFrom(100)
        }.checkResult {
            isNumberPickerValueDisplayed("100")
            isNumberPickerValueDisplayed("до")
        }
        performFilter { clickAcceptButton() }
            .checkResult {
                isContainer(POWER_FIELD_NAME, "от 100 ")
                isDoSearchButtonWithText("Показать 50,826 предложений")
            }
    }

    @Test
    fun shouldApplyPowerToValue() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameter(POWER_TO_PARAM, "250")
                checkNotRequestBodyParameter(POWER_FROM_PARAM)
            }
        }
        performFilter {
            setPowerTo(250)
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("250")
        }
        performFilter { clickAcceptButton() }
            .checkResult {
                isContainer(POWER_FIELD_NAME, "до 250")
                isDoSearchButtonWithText("Показать 50,826 предложений")
            }
    }

    @Test
    fun shouldApplyPowerFromToValue() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameters(POWER_FROM_PARAM to "100", POWER_TO_PARAM to "250")
            }
        }
        performFilter {
            setPowerFrom(100)
            setPowerTo(250)
        }.checkResult {
            isNumberPickerValueDisplayed("100")
            isNumberPickerValueDisplayed("250")
            isClearButtonDisplayed()
        }
        performFilter { clickAcceptButton() }
            .checkResult {
                isContainer(POWER_FIELD_NAME, "от 100 до 250")
                isDoSearchButtonWithText("Показать 50,826 предложений")
            }
    }

    @Test
    fun shouldNotApplyPowerWithUpsideDownValues() {
        performFilter {
            setPowerFrom(250)
            setPowerTo(100)
        }.checkResult {
            isNumberPickerValueDisplayed("100")
            isNumberPickerValueDisplayed("250")
            isClearButtonDisplayed()
        }
        performFilter {
            clickAcceptButton()
        }.checkResult {
            isNumberPickerValueDisplayed("100")
            isNumberPickerValueDisplayed("250")
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldClearFromToValuesByClearButton() {
        performFilter {
            setPowerFrom(100)
            setPowerTo(250)
            clickClearButton()
        }.checkResult {
            isNumberPickerValueDisplayed("от")
            isNumberPickerValueDisplayed("до")
        }
    }

    @Test
    fun shouldClearAppliedFromToValuesByClearButtonAfterCacheExpired() {
        webServerRule.routing{
            getOfferCount().watch {
                checkNotRequestBodyParameters(listOf(POWER_FROM_PARAM, POWER_TO_PARAM))
            }
        }
        performFilter {
            setPowerFrom(100)
            setPowerTo(250)
            clickAcceptButton()
            clickField(POWER_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(POWER_FIELD_NAME, "")
            isDoSearchButtonWithText("Показать 50,826 предложений")
        }
    }

    @Test
    fun shouldNotClearYearByCloseIcon() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameters(POWER_FROM_PARAM to "100", POWER_TO_PARAM to "250")
            }
        }
        performFilter {
            setPowerFrom(100)
            setPowerTo(250)
            clickAcceptButton()
            clickField(POWER_FIELD_NAME)
            clickCloseIcon()
        }.checkResult {
            isContainer(POWER_FIELD_NAME, "от 100 до 250")
            isDoSearchButtonWithText("Показать 50,826 предложений")
        }
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        webServerRule.routing{
            getOfferCount().watch {
                checkRequestBodyParameters(POWER_FROM_PARAM to "100", POWER_TO_PARAM to "250")
            }
        }
        performFilter {
            setPowerFrom(100)
            setPowerTo(250)
            closeDesignBottomSheetBySwipe()
        }.checkResult {
            isContainer(POWER_FIELD_NAME, "от 100 до 250")
            isDoSearchButtonWithText("Показать 50,826 предложений")
        }
    }
}
