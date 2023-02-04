package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.tags.GetSearchTagsDispatcher
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup


@RunWith(AndroidJUnit4::class)
class WithNdsFieldTest {

    private val searchHistoryDispatcherHolder = DispatcherHolder()

    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars"),
        PostSearchOffersDispatcher.getGenericFeed(),
        searchHistoryDispatcherHolder
    )
        .plus(StateGroup.values().map { GetSearchTagsDispatcher(it) })

    private val activityRule = activityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        performMain {
            openFilters()
        }
    }

    @Test
    fun shouldShowWithNdsOnAllStateGroups() {
        performFilter {
            scrollToFilterField(FIELD_TO_SCROLL)
        }.checkResult {
            isCheckboxDisplayed(R.string.field_nds_label)
        }
    }

    @Test
    fun shouldShowWithNdsOnNewStateGroups() {
        performFilter {
            scrollToState()
            selectState(StateGroup.NEW)
            scrollToFilterField(FIELD_TO_SCROLL)
        }.checkResult {
            isCheckboxDisplayed(R.string.field_nds_label)
        }
    }

    @Test
    fun shouldShowWithNdsOnUsedStateGroups() {
        performFilter {
            scrollToState()
            selectState(StateGroup.USED)
            scrollToFilterField(FIELD_TO_SCROLL)
        }.checkResult {
            isCheckboxDisplayed(R.string.field_nds_label)
        }
    }

    @Test
    fun shouldShowWithNdsOnMoto() {
        performFilter {
            selectCategory(VehicleCategory.MOTO)
            scrollToFilterField(FIELD_TO_SCROLL)
        }.checkResult {
            isCheckboxDisplayed(R.string.field_nds_label)
        }
    }

    @Test
    fun shouldShowWithNdsForComm() {
        performFilter {
            selectCategory(VehicleCategory.TRUCKS)
            scrollToFilterField(FIELD_TO_SCROLL)
        }.checkResult {
            isCheckboxDisplayed(R.string.field_nds_label)
        }
    }

    companion object {
        private const val FIELD_TO_SCROLL = "Срок размещения"
    }
}
