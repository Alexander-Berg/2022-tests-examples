package ru.auto.ara.test.dealer.offersfeed

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.robot.dealeroffers.performDealerOffers
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableDealerPromoBannerRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.SORTING_PARAMS

@RunWith(Parameterized::class)
class SortTest(private val testParams: TestParameter) {
    private val markModelsWatcher = RequestWatcher()
    private val SORTING_BOTTOMESHEET_NAME = "Сортировать по:"
    private val SORTING_PARAM = "sort"

    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    private val webServerRule = WebServerRule {
        userSetupDealer()
        delegateDispatchers(GetUserOffersDispatcher.empty("all", markModelsWatcher))
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupAuthRule(),
        DisableDealerPromoBannerRule(),
    )

    @Before
    fun setup() {
        activityTestRule.launchActivity()
        performMain { openLowTab(R.string.offers) }
        performDealerOffers { interactions.onToolbarSortIcon().waitUntilIsCompletelyDisplayed().performClick() }
        performFilter {
            waitBottomSheet()
            clickOption(testParams.name)
        }
    }

    @Test
    fun shouldSeeCorrectStateAndRequest() {
        performDealerOffers {
            interactions.onToolbarSortIcon().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkFilter {
            isBottomsheetListHasExpectedChildsCount(SORTING_PARAMS.size)
            isBottomSheetTitleDisplayed(SORTING_BOTTOMESHEET_NAME)
            markModelsWatcher.checkQueryParameter(SORTING_PARAM, testParams.param)
            isCheckedOptionDisplayed(testParams.name)
            isNotCheckedOptionsDisplayed(testParams.uncheckedOptions)
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = SORTINGS.map { arrayOf(it) }

        private val SORTINGS =
            SORTING_PARAMS.map { (name, param) ->
                TestParameter(
                    name = name,
                    param = param,
                    uncheckedOptions = SORTING_PARAMS.filter { (sortname) -> sortname != name }.map { it.first() }
                )
            }

        data class TestParameter(val name: String, val param: String, val uncheckedOptions: List<String>) {
            override fun toString(): String = param
        }
    }
}
