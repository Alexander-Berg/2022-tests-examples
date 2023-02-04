package ru.auto.ara.test.log.frontlog

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.BuildConfig
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.BodyNode.Companion.checkEventContents
import ru.auto.ara.core.dispatchers.BodyNode.Companion.checkNoEvent
import ru.auto.ara.core.dispatchers.FRONTLOG_DELAY_MS
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.device.postHello
import ru.auto.ara.core.dispatchers.frontlog.FrontLogDispatcher
import ru.auto.ara.core.dispatchers.frontlog.checkFrontlogCommonParams
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.QueryMatcher
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles.searchFeedBundle
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.ara.viewmodel.feed.FilterPromoViewModel
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers.`is` as Is

@RunWith(AndroidJUnit4::class)
class SearchShowListingTest {

    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()
    private val frontlogWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        postHello()
        userSetup()
        delegateDispatcher(PostSearchOffersDispatcher.getGenericFeedWitLastPage())
        delegateDispatcher(FrontLogDispatcher(frontlogWatcher))
    }

    private val timeRule = SetupTimeRule(localTime = "12:00")

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        timeRule,
        DisableAdsRule(),
        activityRule
    )

    @Before
    fun setup() {
        activityRule.launchFragment<SearchFeedFragment>(searchFeedBundle(VehicleCategory.CARS, StateGroup.ALL))
    }

    @Test
    fun shouldChangeQueryIdAfterChangingFeedParameters() {
        performSearchFeed {
            waitFirstPageWithItem { item -> item is FilterPromoViewModel }
            timeRule.setZonedTime(localTime = "13:01")
            openParameters()
        }
        performFilter {
            clickField(getResourceString(R.string.field_year_label))
            setYearFrom(1940)
            clickAcceptButton()
            doSearch()
        }
        performSearchFeed { waitFirstPageWithItem { item -> item is FilterPromoViewModel } }
        checkEventLog(searchQueryId = "29f032b3608d896975864674736f076c169ca4d425260000")
    }

    @Test
    fun shouldLogSearchShowAfterOpenGeneralFeed() {
        performSearchFeed { waitFirstPageWithItem { item -> item is FilterPromoViewModel } }
        checkEventLog()
    }

    @Test
    fun shouldLogSearchShowAfterChangeSort() {
        performSearchFeed { waitFirstPageWithItem { item -> item is FilterPromoViewModel } }
        frontlogWatcher.checkRequestWasCalled(timeoutMs = FRONTLOG_DELAY_MS)
        frontlogWatcher.clearRequestWatcher()

        performSearchFeed {
            openSort()
        }
        performFeedSort {
            scrollToSortItem(getResourceString(R.string.sort_exclusive))
            selectSort(getResourceString(R.string.sort_exclusive))
        }
        performSearchFeed {
            waitFirstPageWithItem { item -> item is FilterPromoViewModel }
        }

        checkEventLog(
            query = listOf(
                "category_id" to "15",
                "section_id" to "1",
                "wheel" to "1",
                "seller" to "3",
                "state" to "2",
                "custom" to "1",
                "without_delivery_field" to "BOTH",
                "sort" to "autoru_exclusive-desc"
            )
        )
    }

    @Test
    fun shouldLogSearchShowAfterChangeState() {
        performSearchFeed { waitFirstPageWithItem { item -> item is FilterPromoViewModel } }
        frontlogWatcher.checkRequestWasCalled(timeoutMs = FRONTLOG_DELAY_MS)
        frontlogWatcher.clearRequestWatcher()

        performSearchFeed {
            selectState(StateGroup.NEW)
        }
        performSearchFeed {
            waitFirstPageWithItem { item -> item is FilterPromoViewModel }
        }

        checkEventLog(
            section = "NEW",
            query = listOf(
                "category_id" to "15",
                "section_id" to "2",
                "sort" to "fresh_relevance_1-desc"
            )
        )
    }

    @Test
    fun shouldNotLogSearchShowAfterPullToRefresh() {
        performSearchFeed { waitFirstPageWithItem { item -> item is FilterPromoViewModel } }
        checkEventLog()
        frontlogWatcher.clearRequestWatcher()

        performSearchFeed {
            refreshFeed()
        }
        performSearchFeed {
            waitFirstPageWithItem { item -> item is FilterPromoViewModel }
        }

        waitSomething(FRONTLOG_DELAY_MS, TimeUnit.MILLISECONDS)
        frontlogWatcher.checkNoEvent("search_show_event")
    }

    private fun checkEventLog(
        section: String = "ALL",
        query: List<Pair<String, String>> = listOf(
            "category_id" to "15",
            "section_id" to "1",
            "wheel" to "1",
            "seller" to "3",
            "state" to "2",
            "custom" to "1",
            "without_delivery_field" to "BOTH",
            "sort" to "fresh_relevance_1-desc"
        ),
        searchQueryId: String = "29f032b3608d896975864674736f076c169ca4d421600000"
    ) {
        val queryRegistry = query.map { (name, value) -> Is(name) to Is(value) }
        frontlogWatcher.checkRequestWasCalled(timeoutMs = FRONTLOG_DELAY_MS)
        frontlogWatcher.checkFrontlogCommonParams("search_show_event")
        frontlogWatcher.checkEventContents("search_show_event") {
            get("app_version").assertValue(BuildConfig.VERSION_NAME)
            get("category").assertValue(VehicleCategory.CARS.toString())
            get("context_block").assertValue("BLOCK_LISTING")
            get("context_page").assertValue("PAGE_LISTING")
            get("context_service").assertValue("SERVICE_AUTORU")
            get("query").assertValue(QueryMatcher(queryRegistry))
            get("region_id").assertValue(null)
            get("search_query_id").assertValue(searchQueryId)
            get("section").assertValue(section)
        }
    }
}
