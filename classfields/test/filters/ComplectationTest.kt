package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.actions.step
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.GetEquipmentFiltersDispatcher
import ru.auto.ara.core.robot.searchfeed.checkComplectation
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performComplectation
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ComplectationTest {
    private val WAIT_DURATION = 400L
    private val INPUT_COMPLECTATION_FIELD_NAME = R.string.input_complectation
    private val CHANGE_COMPLECTATION_FIELD_NAME = R.string.change_complectation
    private val HEADLIGHTS_FIELD_NAME = "Фары"
    private val XENON_HEADLIGHTS_FIELD_NAME = "Ксеноновые/Биксеноновые"
    private val LASER_HEADLIGHTS_FIELD_NAME = "Лазерные"
    private val XENON_AND_LASER_FIELD_PARAM = "xenon,laser-lights"
    private val LED_HEADLIGHTS_FIELD_NAME = "Светодиодные"
    private val ANTIFOG_HEADLIGHTS_FIELD_NAME = "Противотуманные фары"
    private val ANTIFOG_HEADLIGHTS_FIELD_PARAM = "ptf"
    private val EQUIPMENT_FIELD_PARAM = "catalog_equipment"
    private val countWatcher = RequestWatcher()
    private val equipmentWatcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", countWatcher),
        GetEquipmentFiltersDispatcher(equipmentWatcher)
    )
    private val activityRule = activityScenarioRule<MainActivity>()
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
        performMain { openFilters() }
        performFilter { openComplectation(INPUT_COMPLECTATION_FIELD_NAME) }
    }

    @Test
    fun shouldSeeComplectationPickerControls() {
        checkComplectation {
            interactions.onComplectaionTitle().waitUntilIsCompletelyDisplayed()
            interactions.onAcceptButton().waitUntilIsCompletelyDisplayed()
            interactions.onClearButton().checkNotVisible()
            interactions.onCloseIcon().waitUntilIsCompletelyDisplayed()
            interactions.onOffersCount().waitUntilIsCompletelyDisplayed()
                .checkWithClearText("50,826 предложений")
            interactions.onExpandArrow(HEADLIGHTS_FIELD_NAME).waitUntilIsCompletelyDisplayed()
            interactions.onField(HEADLIGHTS_FIELD_NAME).waitUntilIsCompletelyDisplayed()
            interactions.onGroupTitle("Обзор").waitUntilIsCompletelyDisplayed()
            checkEquipmentRequestWasCalled()
        }
    }

    @Test
    fun shouldSelect2ComplectationsFromOneGroup() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(XENON_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(LASER_HEADLIGHTS_FIELD_NAME)
        }.checkResult {
            countWatcher.checkRequestBodyArrayParameter(EQUIPMENT_FIELD_PARAM, setOf(XENON_AND_LASER_FIELD_PARAM))
            isChecked(XENON_HEADLIGHTS_FIELD_NAME, true)
            isChecked(LASER_HEADLIGHTS_FIELD_NAME, true)
        }
    }

    @Test
    fun shouldSeeCounterOfSelectedFieldFromOneGroup() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(XENON_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(LASER_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(HEADLIGHTS_FIELD_NAME)
        }.checkResult {
            interactions.onGroupCounter(HEADLIGHTS_FIELD_NAME).waitUntilIsCompletelyDisplayed().checkWithClearText("2")
        }
    }

    @Test
    fun shouldCheckGroupParentAfterCheckingAllChildren() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(XENON_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(LASER_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(LED_HEADLIGHTS_FIELD_NAME)
        }.checkResult {
            isChecked(HEADLIGHTS_FIELD_NAME, true)
        }
    }

    @Test
    fun shouldSelectComplectationNotFromGroup() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(ANTIFOG_HEADLIGHTS_FIELD_NAME)
        }.checkResult {
            countWatcher.checkRequestBodyArrayParameter(EQUIPMENT_FIELD_PARAM, setOf(ANTIFOG_HEADLIGHTS_FIELD_PARAM))
            isChecked(ANTIFOG_HEADLIGHTS_FIELD_NAME, true)
        }
    }

    @Test
    fun shouldSeeCorrectStateAtFilterScreen() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(XENON_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(LASER_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(ANTIFOG_HEADLIGHTS_FIELD_NAME)
            interactions.onAcceptButton().performClick()
        }.checkResult {
            countWatcher.checkRequestBodyArrayParameter(
                keyPath = EQUIPMENT_FIELD_PARAM,
                values = setOf(ANTIFOG_HEADLIGHTS_FIELD_PARAM, XENON_AND_LASER_FIELD_PARAM)
            )
        }
        checkFilter { isExtrasValues("Выбрано 2 параметра") }
    }

    @Test
    fun shouldSeeCorrectStateAtComplectationScreenAfterApply() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(XENON_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(LASER_HEADLIGHTS_FIELD_NAME)
            clickFieldWithScroll(ANTIFOG_HEADLIGHTS_FIELD_NAME)
            interactions.onAcceptButton().performClick()
            waitButton()
        }
        performFilter { openComplectation(CHANGE_COMPLECTATION_FIELD_NAME) }
        checkComplectation {
            waitButton()
            isChecked(XENON_HEADLIGHTS_FIELD_NAME, true)
            isChecked(LASER_HEADLIGHTS_FIELD_NAME, true)
            isChecked(ANTIFOG_HEADLIGHTS_FIELD_NAME, true)
        }
    }

    @Test
    fun shouldApplyValueBySwipe() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(ANTIFOG_HEADLIGHTS_FIELD_NAME)
            closeBottomSheetBySwipe()
            waitButton()
        }
        checkFilter {
            isExtrasValues("Выбран 1 параметр")
            countWatcher.checkRequestBodyArrayParameter(EQUIPMENT_FIELD_PARAM, setOf(ANTIFOG_HEADLIGHTS_FIELD_PARAM))
        }
    }

    @Test
    fun shouldNotApplyValueByClickCloseIcon() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(ANTIFOG_HEADLIGHTS_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            interactions.onCloseIcon().performClick()
            waitButton()
        }
        checkFilter { interactions.onExtrasValues().checkWithText("") }
        performFilter { openComplectation(INPUT_COMPLECTATION_FIELD_NAME) }
        countWatcher.checkNotRequestBodyParameter(EQUIPMENT_FIELD_PARAM)
    }

    @Test
    fun shouldClearValuesByClearButton() {
        performComplectation {
            waitButton()
            clickFieldWithScroll(ANTIFOG_HEADLIGHTS_FIELD_NAME)
            interactions.onClearButton().waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult { isWarningMessage() }
        performComplectation { interactions.onYes().performClick() }.checkResult {
            isChecked(ANTIFOG_HEADLIGHTS_FIELD_NAME, false)
        }
    }

    private fun checkEquipmentRequestWasCalled() = step("check equipment request was called") {
        MatcherAssert.assertThat(equipmentWatcher.isRequestCalled(), CoreMatchers.`is`(true))
    }

    private fun waitButton() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)

}
