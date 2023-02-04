package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.STEERING_WHEEL_PARAMS
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(AndroidJUnit4::class)
class SteeringWheelTest {
    private val FIELD_NAME = "Срок владения"
    private val STEERING_WHEEL_FIELD_NAME = "Расположение руля"
    private val STEERING_WHEEL_DEFAULT_VALUE = "Любой руль"
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars")
    )
    private val activityRule = activityScenarioRule<MainActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        performMain { openFilters() }
        performFilter {
            clickFieldWithHintWithOverScroll(
                FIELD_NAME,
                STEERING_WHEEL_FIELD_NAME
            )
        }
    }

    @Test
    fun shouldSeeSteeringWheelPickerControls() {
        checkFilter {
            isAcceptButtonNotDisplayed()
            isClearButtonNotExists()
            isCloseIconDisplayed()
            isSingleSelectOptionWithDefaultDisplayed(STEERING_WHEEL_PARAMS, STEERING_WHEEL_DEFAULT_VALUE)
            isBottomSheetTitleDisplayed(STEERING_WHEEL_FIELD_NAME)
        }
    }

}
