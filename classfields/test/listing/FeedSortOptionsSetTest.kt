package ru.auto.ara.test.listing

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.COMMON_OPTIONS
import ru.auto.ara.core.testdata.DEFAULT_COMMON_PARAMS
import ru.auto.ara.core.testdata.NEW_CARS_OPTIONS
import ru.auto.ara.core.testdata.TRUCKS_AVAILABLE_FROM_FILTERS
import ru.auto.ara.core.testdata.USED_CARS_OPTIONS
import ru.auto.ara.core.utils.getResourceString
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
import ru.auto.data.model.filter.TruckParams
import ru.auto.data.model.filter.TruckSearch
import ru.auto.data.model.filter.VehicleSearch
import ru.auto.data.model.search.SearchContext

/**
 * This test checks for showing available options for different category-state combinations
 */
@RunWith(Parameterized::class)
class FeedSortOptionsSetTest(private val testParameter: TestParameter) {

    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSearchOffersDispatcher.getGenericFeed()
    )
    private val activityTestRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @Rule
    @JvmField
    val ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun openListingWithGivenCategory() {
        activityTestRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragment.createArgs(
                SearchFeedContext(
                    context = SearchContext.DEFAULT,
                    formState = VehicleSearchToFormStateConverter.convert(testParameter.search)
                )
            )
        )
    }

    @Test
    fun checkSortOptionsAndDefaultSelected() {
        performSearchFeed {
            openSort()
        }

        performFeedSort().checkResult {
            checkSortOptionsAvailable(testParameter.sortOptions)
            checkSortSelected(testParameter.defaultOption)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = (listOf(
            TestParameter(
                search = CarSearch(
                    carParams = CarParams(),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.NEW)
                ),
                sortOptions = NEW_CARS_OPTIONS,
                defaultOption = getResourceString(R.string.sort_relevance),
                defaultOptionDescription = "fresh_relevance_1-desc"
            ),
            TestParameter(
                search = CarSearch(
                    carParams = CarParams(),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.USED)
                ),
                sortOptions = USED_CARS_OPTIONS,
                defaultOption = getResourceString(R.string.sort_relevance),
                defaultOptionDescription = "fresh_relevance_1-desc"
            ),
            TestParameter(
                search = CarSearch(
                    carParams = CarParams(),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.ALL)
                ),
                sortOptions = USED_CARS_OPTIONS,
                defaultOption = getResourceString(R.string.sort_relevance),
                defaultOptionDescription = "fresh_relevance_1-desc"
            )
        ) + TRUCKS_AVAILABLE_FROM_FILTERS.map { truckCategory ->
            TestParameter(
                search = TruckSearch(
                    truckParams = TruckParams(trucksCategory = truckCategory),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.ALL)
                ),
                sortOptions = COMMON_OPTIONS,
                defaultOption = getResourceString(R.string.sort_date),
                defaultOptionDescription = "cr_date-desc"

            )
        } + MotoCategory.values().map { motoCategory ->
            TestParameter(
                search = MotoSearch(
                    motoParams = MotoParams(motoCategory = motoCategory),
                    commonParams = DEFAULT_COMMON_PARAMS.copy(stateGroup = StateGroup.ALL)
                ),
                sortOptions = COMMON_OPTIONS,
                defaultOption = getResourceString(R.string.sort_date),
                defaultOptionDescription = "cr_date-desc"
            )
        }).map { arrayOf(it) }

    }

    data class TestParameter(
        val search: VehicleSearch,
        val sortOptions: List<String>,
        val defaultOption: String,
        val defaultOptionDescription: String
    ) {
        override fun toString(): String = "${search.category} ${search.commonParams.stateGroup} default $defaultOptionDescription"
    }
}
