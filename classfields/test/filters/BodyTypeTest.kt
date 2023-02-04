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
import ru.auto.ara.core.testdata.BODY_TYPE_PARAMS
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BodyTypeTest {
    private val WAIT_DURATION = 400L
    private val FIELD_NAME = "Привод"
    private val BODY_TYPE_FIELD_NAME = "Кузов"
    private val BODY_TYPE_PARAM = "cars_params.body_type_group"
    private val BODY_TYPE_PARENT_PARAM = "cars_params"
    private val FIRST_OPTION = BODY_TYPE_PARAMS[0]
    private val SECOND_OPTION = BODY_TYPE_PARAMS[1]
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
        performFilter { clickFieldWithOverScroll(FIELD_NAME, BODY_TYPE_FIELD_NAME) }
    }

    @Test
    fun shouldSeeBodyTypePickerControls() {
        checkFilter {
            isAcceptButtonDisplayed()
            isClearButtonNotDisplayed()
            isCloseIconDisplayed()
            isBodyTypeNotCheckedOptionsDisplayed(BODY_TYPE_PARAMS)
            isBottomSheetTitleDisplayed(BODY_TYPE_FIELD_NAME)
        }
    }

    @Test
    fun shouldSeeClearButtonWhenValueSelected() {
        performFilter { clickMultiSelectOptionWithScroll(FIRST_OPTION.name) }.checkResult {
            isAcceptButtonDisplayed()
            isClearButtonDisplayed()
            isCloseIconDisplayed()
        }
    }

    @Test
    fun shouldClearValuesByClearButtonAfterCacheExpired() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION.name)
            clickAcceptButton()
            waitBottomSheetBeClosed()
            checkFilter { isFilterScreen() }
            clickFieldWithOverScroll(FIELD_NAME, BODY_TYPE_FIELD_NAME)
            timeRule.setTime(time = "00:01")
            clickClearButton()
            clickAcceptButton()
        }.checkResult {
            isContainer(BODY_TYPE_FIELD_NAME, "")
            watcher.checkNotRequestBodyParameter(BODY_TYPE_PARENT_PARAM)
        }
    }

    @Test
    fun shouldSelectMultiValues() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION.name)
            clickMultiSelectOptionWithScroll(SECOND_OPTION.name)
            clickAcceptButton()
        }.checkResult {
            isContainer(BODY_TYPE_FIELD_NAME, "${FIRST_OPTION.name}, ${SECOND_OPTION.name}")
            watcher.checkRequestBodyArrayParameter(BODY_TYPE_PARAM, setOf(FIRST_OPTION.param, SECOND_OPTION.param))
        }
    }

    @Test
    fun shouldApplyParamsWhenClosedBySwipe() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION.name)
            closeListBottomsheetWithSwipe()
        }.checkResult {
            isContainer(BODY_TYPE_FIELD_NAME, FIRST_OPTION.name)
            watcher.checkRequestBodyArrayParameter(BODY_TYPE_PARAM, setOf(FIRST_OPTION.param))
        }
    }

    @Test
    fun shouldNotApplyValueWhenClosedByCross() {
        performFilter {
            clickMultiSelectOptionWithScroll(FIRST_OPTION.name)
            clickCloseIcon()
        }.checkResult {
            isContainer(BODY_TYPE_FIELD_NAME, "")
            watcher.checkNotRequestBodyParameter(BODY_TYPE_PARENT_PARAM)
        }
    }

    private fun waitBottomSheetBeClosed() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)
}
