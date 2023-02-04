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
import ru.auto.ara.core.testdata.OWNING_TIME_PARAMS
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(AndroidJUnit4::class)
class OwningTimeTest {
    private val FIELD_NAME = "Таможня"
    private val OWNING_TIME_FIELD_NAME = "Срок владения"
    private val OWNING_TIME_DEFAULT_VALUE = "Неважно"
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
        performFilter { clickFieldWithHintWithOverScroll(FIELD_NAME, OWNING_TIME_FIELD_NAME) }
    }

    @Test
    fun shouldSeeOwningTimePickerControls() {
        checkFilter {
            isAcceptButtonNotDisplayed()
            isClearButtonNotExists()
            isCloseIconDisplayed()
            isSingleSelectOptionWithDefaultDisplayed(OWNING_TIME_PARAMS, OWNING_TIME_DEFAULT_VALUE)
            isBottomSheetTitleDisplayed(OWNING_TIME_FIELD_NAME)
        }
    }
}
