package ru.auto.ara.test.dealer.filters

import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
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

@RunWith(AndroidJUnit4::class)
class SegmentDependenciesOfCampaignTest {
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val campaignsHolder = DispatcherHolder()

    private val webServerRule = WebServerRule {
        userSetupDealer()
        delegateDispatchers(
            campaignsHolder,
            GetUserOffersMarkModelsDispatcher.empty(),
            GetUserOffersDispatcher.dealerOffers()
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule()
    )

    @Test
    fun shouldSeeCategorySegmentWhenAllCategoriesAvailable() {
        campaignsHolder.innerDispatcher = GetDealerCampaignsDispatcher("all")
        openFilters()
        checkFilter {
            isToggleButtonChecked("Все")
            isToggleButtonNotChecked("Легковые")
            isToggleButtonNotChecked("Мото")
            isToggleButtonNotChecked("Комтранс")
        }
    }

    @Test
    fun shouldNotSeeCategorySegmentWhenOnlyOneCategoryAvailable() {
        campaignsHolder.innerDispatcher = GetDealerCampaignsDispatcher("cars")
        openFilters()
        checkFilter {
            interactions.onToolbarTitle().waitUntilIsCompletelyDisplayed()
            interactions.onToggleButton("Легковые").checkNotExists()
            interactions.onToggleButton("Мото").checkNotExists()
            interactions.onToggleButton("Комтранс").checkNotExists()
        }
    }

    @Test
    fun shouldSeeStateSegmentWhenBothStatesAvailable() {
        campaignsHolder.innerDispatcher = GetDealerCampaignsDispatcher("cars")
        openFilters()
        checkFilter {
            interactions.onToggleButtonWithSibling("Все", "Новые").waitUntil(isCompletelyDisplayed(), isChecked())
            isToggleButtonNotChecked("Новые")
            isToggleButtonNotChecked("С пробегом")
        }
    }

    @Test
    fun shouldNotSeeStateSegmentWhenOneStateAvailable() {
        campaignsHolder.innerDispatcher = GetDealerCampaignsDispatcher("all_without_new_cars")
        openFilters()
        performFilter { interactions.onToggleButton("Легковые").waitUntilIsCompletelyDisplayed().performClick() }
            .checkResult {
                isToggleButtonChecked("Легковые")
                interactions.onToggleButtonWithSibling("Все", "Новые").checkNotExists()
                interactions.onToggleButton("Новые").checkNotExists()
                interactions.onToggleButton("С пробегом").checkNotExists()
            }
    }

    private fun openFilters() {
        activityRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }
        performDealerOffers {
            interactions.onParametersFab().waitUntilIsCompletelyDisplayed().performClick()
        }
    }
}
