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
import ru.auto.data.model.filter.StateGroup
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PriceForNewCarsTest {
    private val WAIT_DURATION = 400L
    private val PRICE_FIELD_NAME = "Цена, \u20BD"
    private val PRICE_FROM_PARAM = "price_from"
    private val PRICE_TO_PARAM = "price_to"
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
        performFilter {
            selectState(StateGroup.NEW)
            clickField(PRICE_FIELD_NAME)
        }
    }

    @Test
    fun shouldSeePricePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBottomSheetTitleDisplayed(PRICE_FIELD_NAME)
            isPriceFromTitleDisplayed()
            isPriceToTitleDisplayed()
            isPriceFromInputWithHintDisplayed("10 000")
            isPriceToInputWithHintDisplayed("300 000 000")
            isPriceSeekBarDisplayed()
        }
        watcher.checkNotRequestBodyParameters(listOf(PRICE_FROM_PARAM, PRICE_TO_PARAM))
    }

    @Test
    fun shouldSeeClearButtonWhenPriceFromChanged() {
        performFilter {
            setPriceFrom("30000")
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldSeeClearButtonWhenPriceToChanged() {
        performFilter {
            setPriceTo("30000")
        }.checkResult {
            isClearButtonDisplayed()
        }
    }

    @Test
    fun shouldApplyPriceFromValue() {
        performFilter { setPriceFrom("30000") }
            .checkResult { isPriceFromInputDisplayed("30 000") }
        waitButton()
        performFilter { clickAcceptButton() }.checkResult { isContainer(PRICE_FIELD_NAME, "от 30,000 ") }
        watcher.checkRequestBodyParameter(PRICE_FROM_PARAM, "30000")
        watcher.checkNotRequestBodyParameter(PRICE_TO_PARAM)
    }

    @Test
    fun shouldApplyPriceToValue() {
        performFilter { setPriceTo("300000") }
            .checkResult { isPriceToInputDisplayed("300 000") }
        waitButton()
        performFilter { clickAcceptButton() }.checkResult { isContainer(PRICE_FIELD_NAME, "до 300,000") }
        watcher.checkRequestBodyParameter(PRICE_TO_PARAM, "300000")
        watcher.checkNotRequestBodyParameter(PRICE_FROM_PARAM)
    }

    @Test
    fun shouldApplyPriceFromToValues() {
        performFilter {
            setPriceFrom("30000")
            setPriceTo("300000")
        }.checkResult {
            isPriceFromInputDisplayed("30 000")
            isPriceToInputDisplayed("300 000")
        }
        waitButton()
        performFilter { clickAcceptButton() }.checkResult { isContainer(PRICE_FIELD_NAME, "от 30,000 до 300,000") }
        watcher.checkRequestBodyParameters(PRICE_FROM_PARAM to "30000", PRICE_TO_PARAM to "300000")
    }

    @Test
    fun shouldApplyPriceFromToWithUpsideDownValues() {
        performFilter {
            setPriceFrom("300000")
            setPriceTo("30000")
        }.checkResult {
            isPriceFromInputDisplayed("300 000")
            isPriceToInputDisplayed("30 000")
        }
        waitButton()
        performFilter { clickAcceptButton() }.checkResult { isContainer(PRICE_FIELD_NAME, "от 30,000 до 300,000") }
        watcher.checkRequestBodyParameters(PRICE_FROM_PARAM to "30000", PRICE_TO_PARAM to "300000")
    }

    @Test
    fun shouldClearFromToValuesByClearButton() {
        performFilter {
            setPriceFrom("30000")
            setPriceTo("300000")
            waitButton()
            clickClearButton()
        }.checkResult {
            isPriceFromInputWithHintDisplayed("10 000")
            isPriceToInputWithHintDisplayed("300 000 000")
        }
    }

    @Test
    fun shouldClearAppliedFromToValuesByClearButtonAfterCountCacheExpired() {
        performFilter {
            setPriceFrom("30000")
            setPriceTo("300000")
            clickAcceptButton()
            waitButton()
            clickField(PRICE_FIELD_NAME)
            waitButton()
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(PRICE_FIELD_NAME, "")
        }
        watcher.checkNotRequestBodyParameters(listOf(PRICE_FROM_PARAM, PRICE_TO_PARAM))
    }

    @Test
    fun shouldNotClearPriceByCloseIcon() {
        performFilter {
            setPriceFrom("30000")
            setPriceTo("300000")
            clickAcceptButton()
            waitButton()
            clickField(PRICE_FIELD_NAME)
            waitButton()
            clickCloseIcon()
        }.checkResult { isContainer(PRICE_FIELD_NAME, "от 30,000 до 300,000") }
        watcher.checkRequestBodyParameters(PRICE_FROM_PARAM to "30000", PRICE_TO_PARAM to "300000")
    }

    @Test
    fun shouldNotApplyLowerMinValue() {
        performFilter {
            setPriceFrom("1000")
            clickAcceptButton()
        }.checkResult {
            isPriceFromInputDisplayed("1 000")
        }
    }

    @Test
    fun shouldNotApplyHigherMaxValue() {
        performFilter {
            setPriceFrom("400000000")
            clickAcceptButton()
        }.checkResult {
            isPriceFromInputDisplayed("400 000 000")
        }
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            setPriceFrom("30000")
            setPriceTo("300000")
            closeDesignBottomSheetBySwipe()
        }.checkResult { isContainer(PRICE_FIELD_NAME, "от 30,000 до 300,000") }
        watcher.checkRequestBodyParameters(PRICE_FROM_PARAM to "30000", PRICE_TO_PARAM to "300000")
    }

    private fun waitButton() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

}
