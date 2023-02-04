package ru.auto.ara.test.listing

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.COMMON_OPTIONS
import ru.auto.ara.core.testdata.DEFAULT_COMMON_PARAMS
import ru.auto.ara.core.testdata.NEW_CARS_OPTIONS
import ru.auto.ara.core.testdata.USED_CARS_OPTIONS
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.ara.ui.helpers.form.util.VehicleSearchToFormStateConverter
import ru.auto.ara.viewmodel.search.SearchFeedContext
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.MotoCategory
import ru.auto.data.model.filter.MotoParams
import ru.auto.data.model.filter.MotoSearch
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.model.filter.TruckCategory
import ru.auto.data.model.filter.TruckParams
import ru.auto.data.model.filter.TruckSearch
import ru.auto.data.model.filter.VehicleSearch
import ru.auto.data.model.search.SearchContext

@RunWith(Parameterized::class)
class FeedSortCheckingTest(private val testParams: TestParameter) {


    private val watcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSearchOffersDispatcher.getGenericFeed(watcher)
    )
    var activityTestRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityTestRule
    )

    @Before
    fun openListingWithGivenCategory() {
        activityTestRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragment.createArgs(
                SearchFeedContext(
                    context = SearchContext.DEFAULT,
                    formState = VehicleSearchToFormStateConverter.convert(testParams.search)
                )
            )
        )
    }

    @Test
    fun shouldSelectSort() {
        performSearchFeed {
            openSort()
        }
        performFeedSort {
            scrollToSortItem(testParams.sortOption)
            selectSort(testParams.sortOption)
        }
        performSearchFeed {
            waitSearchFeed()
        }
        watcher.checkQueryParameter("sort", testParams.sortParameter)
        performSearchFeed {
            openSort()
        }
        performFeedSort {
            scrollToSortItem(testParams.sortOption)
        }.checkResult {
            checkSortSelected(testParams.sortOption)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = (NEW_CARS + ALL_CARS + TRUCKS + MOTO).map { arrayOf(it) }

        private val COMMON_SORT_PARAMS = listOf(
            "cr_date-desc",
            "price-asc",
            "price-desc",
            "year-desc",
            "year-asc",
            "km_age-asc",
            "alphabet-asc"
        )

        private val NEW_CARS = NEW_CARS_OPTIONS.zip(
            listOf(
                "fresh_relevance_1-desc",
                "price-asc",
                "price-desc",
                "alphabet-asc"
            )
        ).map { (option, parameter) ->
            TestParameter(
                search = CarSearch(
                    carParams = CarParams(),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.NEW)
                ),
                sortOption = option,
                sortParameter = parameter
            )
        }

        private val ALL_CARS = USED_CARS_OPTIONS.zip(
            listOf(
                "fresh_relevance_1-desc",
                "cr_date-desc",
                "price-asc",
                "price-desc",
                "year-desc",
                "year-asc",
                "km_age-asc",
                "alphabet-asc",
                "autoru_exclusive-desc",
                "price_profitability-desc",
                "proven_owner-desc"
            )
        ).map { (option, param) ->
            TestParameter(
                search = CarSearch(
                    carParams = CarParams(),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.ALL)
                ),
                sortOption = option,
                sortParameter = param
            )
        }

        private val TRUCKS = COMMON_OPTIONS.zip(COMMON_SORT_PARAMS).map { (option, param) ->
            TestParameter(
                search = TruckSearch(
                    truckParams = TruckParams(trucksCategory = TruckCategory.BUS),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.ALL)
                ),
                sortOption = option,
                sortParameter = param
            )
        }

        private val MOTO = COMMON_OPTIONS.zip(COMMON_SORT_PARAMS).map { (option, param) ->
            TestParameter(
                search = MotoSearch(
                    motoParams = MotoParams(motoCategory = MotoCategory.MOTORCYCLE),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.ALL)
                ),
                sortOption = option,
                sortParameter = param
            )
        }
    }

    data class TestParameter(
        val search: VehicleSearch,
        val sortOption: String,
        val sortParameter: String
    ) {
        override fun toString(): String = "${search.category} ${search.commonParams.stateGroup} sort by $sortParameter"
    }
}
