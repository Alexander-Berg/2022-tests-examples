package ru.auto.ara.test.dealer.filters

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.dealer.GetDealerCampaignsDispatcher
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersMarkModelsDispatcher
import ru.auto.ara.core.robot.dealeroffers.performDealerOffers
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.DEALER_TRUCK_SUBCATEGORIES_PARAMS

@RunWith(Parameterized::class)
class TruckSubcategoriesTest(private val testParams: TestParameter) {
    private val TRUCK_CATEGORY_FIELD_NAME = "Раздел"
    private val TRUCK_CATEGORY_PARAM = "truck_category"
    private val TRUCK_TOGGLE = "Комтранс"
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val markModelsWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        userSetupDealer()
        delegateDispatchers(
            GetDealerCampaignsDispatcher("all"),
            GetUserOffersDispatcher.dealerOffers(),
            GetUserOffersMarkModelsDispatcher.empty(markModelsWatcher)
        )
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule()
    )

    @Before
    fun setup() {
        activityRule.launchActivity()
        performMain { openDealerOffersTab() }
        performDealerOffers { interactions.onParametersFab().waitUntilIsCompletelyDisplayed().performClick() }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestForTruckSubcategory() {
        performFilter {
            interactions.onToggleButton(TRUCK_TOGGLE).waitUntilIsCompletelyDisplayed().performClick()
            interactions.onContainerWithHint(TRUCK_CATEGORY_FIELD_NAME).performClick()
            waitBottomSheet()
            clickSelectOptionWithScroll(testParams.name)
        }
        checkFilter {
            isInputContainer(TRUCK_CATEGORY_FIELD_NAME, testParams.name)
            markModelsWatcher.checkQueryParameter(TRUCK_CATEGORY_PARAM, testParams.param)
        }
        performFilter { interactions.onContainerWithHint(TRUCK_CATEGORY_FIELD_NAME).performClick() }.checkResult {
            isBottomsheetListHasExpectedChildsCount(DEALER_TRUCK_SUBCATEGORIES_PARAMS.size)
            isBottomSheetTitleDisplayed(TRUCK_CATEGORY_FIELD_NAME)
            isCheckedSelectOptionDisplayed(testParams.name)
            isNotCheckedSelectOptionsDisplayed(testParams.uncheckedOptions)
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = DEALER_TRUCK_SUBCATEGORIES_PARAMS.map { (name, param) ->
            TestParameter(
                name = name,
                param = param,
                uncheckedOptions = DEALER_TRUCK_SUBCATEGORIES_PARAMS.filter { (sortname) -> sortname != name }
                    .map { it.first() }
            )
        }.map { arrayOf(it) }

        data class TestParameter(
            val name: String,
            val param: String,
            val uncheckedOptions: List<String>
        ) {
            override fun toString() = param
        }
    }
}
