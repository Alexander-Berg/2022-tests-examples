package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.filters.checkMark
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class FiltersTest {

    private val countWatcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", countWatcher)
    )
    private val activityRule = ActivityTestRule(MainActivity::class.java, false, true)

    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityRule
    )

    @Before
    fun setUp() {
        performMain { openFilters() }
    }

    @Test
    fun shouldSeeWarningMessage() {
        performFilter {
            selectState(StateGroup.NEW)
            clearAll()
        }.checkResult {
            isWarningMessage()
        }
    }

    @Test
    fun shouldSendNewState() {
        performFilter {
            selectState(StateGroup.NEW)
        }
        countWatcher
            .checkRequestBodyParameter("state_group", "NEW")
            .checkQueryParameter("context", "listing")
    }

    @Test
    fun shouldNotSendExchangeGroupInNewFilter() {
        countWatcher
            .checkNotRequestBodyParameter("exchange_group")
            .checkNotQueryParameter("count")
    }

    @Test
    fun shouldOpenMark() {
        performFilter {
            openMarkFilters()
        }
        checkMark { isToolbarTitleWithText("Марки") }
    }

    @Test
    fun shouldDoSearch() {
        performFilter {
            doSearch()
        }
        checkSearchFeed {
            isEmptyMMNGFilterDisplayed()
        }
    }
}
