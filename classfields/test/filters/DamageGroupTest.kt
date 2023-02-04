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
import ru.auto.ara.core.testdata.DAMAGE_GROUP_PARAMS

@RunWith(AndroidJUnit4::class)
class DamageGroupTest {
    private val FIELD_NAME = "Только с фото"
    private val DAMAGE_GROUP_FIELD_NAME = "Состояние"
    private val DAMAGE_GROUP_DEFAULT_VALUE = "Кроме битых"
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
        performFilter { clickFieldWithHintWithOverScroll(FIELD_NAME, DAMAGE_GROUP_FIELD_NAME) }
    }

    @Test
    fun shouldSeeDamageGroupPickerControls() {
        checkFilter {
            isAcceptButtonNotDisplayed()
            isClearButtonNotExists()
            isCloseIconDisplayed()
            isSingleSelectOptionWithDefaultDisplayed(DAMAGE_GROUP_PARAMS, DAMAGE_GROUP_DEFAULT_VALUE)
            isBottomSheetTitleDisplayed(DAMAGE_GROUP_FIELD_NAME)
        }
    }
}
