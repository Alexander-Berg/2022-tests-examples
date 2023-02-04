package ru.auto.ara.test.dealer.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.dealer.GetDealerCampaignsDispatcher
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersMarkModelsDispatcher
import ru.auto.ara.core.robot.dealeroffers.performDealerOffers
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class PriceTest {
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val markModelsWatcher = RequestWatcher()
    private val PRICE_FIELD_NAME = "Цена, \u20BD"
    private val PRICE_FROM_PARAM = "price_from"
    private val PRICE_TO_PARAM = "price_to"

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
        performMain {
            openLowTab(R.string.offers)
        }
        performDealerOffers {
            interactions.onParametersFab().waitUntilIsCompletelyDisplayed().performClick()
        }
        performFilter {
            performFilter { interactions.onContainer(PRICE_FIELD_NAME).performClick() }
            waitBottomSheet()
        }
    }

    @Test
    fun shouldApplyPriceFromToValues() {
        performFilter {
            setPriceFrom("30000")
            setPriceTo("300000")
        }.checkResult {
            isPriceFromInputDisplayed("30 000")
            isPriceToInputDisplayed("300 000")
        }
        performFilter {
            waitBottomSheet()
            clickAcceptButton()
        }.checkResult {
            interactions.onContainer(PRICE_FIELD_NAME).checkIsCompletelyDisplayed()
                .checkWithText("от 30,000 до 300,000")
            markModelsWatcher.checkQueryParameters(listOf(PRICE_FROM_PARAM to "30000", PRICE_TO_PARAM to "300000"))
        }
    }

}
