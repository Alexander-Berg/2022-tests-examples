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
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ClearanceTest {
    private val WAIT_DURATION = 400L
    private val FIELD_NAME = "Расположение руля"
    private val CLEARANCE_FIELD_NAME = "Клиренс от, мм"
    private val CLEARANCE_TITLE = "Клиренс"
    private val CLEARANCE_FROM_PARAM = "clearance_from"
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, CLEARANCE_FIELD_NAME) }
    }

    @Test
    fun shouldSeeClearancePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(CLEARANCE_TITLE)
            isHintAboveEditTextDisplayed()
            isSingleEditTextFieldDisplayed("")
        }
    }

    @Test
    fun shouldSeeClearButtonAfterValueChanged() {
        performFilter {
            setValueToEditText("100")
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyClearanceValue() {
        performFilter {
            setValueToEditText("100")
            checkFilter {
                isSingleEditTextFieldDisplayed("100")
                isAcceptButtonDisplayed()
            }
            waitButton()
            clickAcceptButton()
        }

        checkFilter {
            isFilterScreen()
            isContainer(CLEARANCE_FIELD_NAME, "от 100")
        }
        watcher.checkRequestBodyParameter(CLEARANCE_FROM_PARAM, "100")
    }

    @Test
    fun shouldApplyClearanceValueWhenCloseBySwipe() {
        performFilter {
            setValueToEditText("100")
            checkFilter {
                isSingleEditTextFieldDisplayed("100")
                isAcceptButtonDisplayed()
            }
            waitButton()
            closeDesignBottomSheetBySwipe()
        }.checkResult {
            isFilterScreen()
            isContainer(CLEARANCE_FIELD_NAME, "от 100")
        }
        watcher.checkRequestBodyParameter(CLEARANCE_FROM_PARAM, "100")
    }

    @Test
    fun shouldClearValueByClearButton() {
        performFilter {
            setValueToEditText("100")
            checkFilter { isClearButtonDisplayed() }
            waitButton()
            clickClearButton()
        }
        checkFilter { isSingleEditTextFieldDisplayed("") }
    }

    @Test
    fun shouldClearAppliedValuesByClearButton() {
        performFilter {
            setValueToEditText("100")
            waitButton()
            clickAcceptButton()
            checkFilter {
                isFilterScreen()
                isContainerDisplayed(CLEARANCE_FIELD_NAME)
            }
            waitButton()
            clickField(CLEARANCE_FIELD_NAME)
            waitButton()
            timeRule.setTime(time = "00:01")
            clickClearButton()
            waitButton()
            clickAcceptButton()
        }
        checkFilter {
            isFilterScreen()
            isContainer(CLEARANCE_FIELD_NAME, "")
        }
        watcher.checkNotRequestBodyParameter(CLEARANCE_FROM_PARAM)
    }

    @Test
    fun shouldNotClearClearanceByCloseIcon() {
        performFilter {
            setValueToEditText("100")
            waitButton()
            clickAcceptButton()
            checkFilter {
                isFilterScreen()
                isContainerDisplayed(CLEARANCE_FIELD_NAME)
            }
            waitButton()
            clickField(CLEARANCE_FIELD_NAME)
            waitButton()
            clickCloseIcon()
        }
        checkFilter {
            isFilterScreen()
            isContainer(CLEARANCE_FIELD_NAME, "от 100")
        }
        watcher.checkRequestBodyParameter(CLEARANCE_FROM_PARAM, "100")
    }

    private fun waitButton() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)
}
