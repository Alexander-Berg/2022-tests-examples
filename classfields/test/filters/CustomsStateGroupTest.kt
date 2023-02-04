package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.CUSTOMS_STATE_GROUP_PARAMS

@RunWith(AndroidJUnit4::class)
class CustomsStateGroupTest {
    private val FIELD_NAME = "Только с фото"
    private val CUSTOMS_STATE_GROUP_FIELD_NAME = "Таможня"
    private val CUSTOMS_STATE_GROUP_DEFAULT_VALUE = "Растаможен"
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("cars")
        )
    }
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        performMain { openFilters() }
        performFilter { clickFieldWithHintWithOverScroll(FIELD_NAME, CUSTOMS_STATE_GROUP_FIELD_NAME) }
    }

    @Test
    fun shouldSeeCustomsStatePickerControls() {
        checkFilter {
            isAcceptButtonNotDisplayed()
            isClearButtonNotExists()
            isCloseIconDisplayed()
            isSingleSelectOptionWithDefaultDisplayed(CUSTOMS_STATE_GROUP_PARAMS, CUSTOMS_STATE_GROUP_DEFAULT_VALUE)
            isBottomSheetTitleDisplayed(CUSTOMS_STATE_GROUP_FIELD_NAME)
        }
    }
}
