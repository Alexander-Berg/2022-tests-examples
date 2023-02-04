package ru.auto.ara.test.dealer.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.dealer.GetDealerCampaignsDispatcher
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersMarkModelsDispatcher
import ru.auto.ara.core.robot.dealeroffers.performDealerOffers
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.withItemsCount

@RunWith(AndroidJUnit4::class)
class MarkModelTest {
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val markModelsWatcher = RequestWatcher()
    private val markModelsHolder = DispatcherHolder()
    private val MARK_FIELD_NAME = "Марка"
    private val SELECTED_MARK = "Ford"
    private val MODEL_FIELD_NAME = "Модель"
    private val SELECTED_MODEL = "Ranger"
    private val MARK_MODEL_PARAM = "mark_model"

    private val webServerRule = WebServerRule {
        userSetupDealer()
        delegateDispatchers(
            GetDealerCampaignsDispatcher("all"),
            GetUserOffersDispatcher.dealerOffers(),
            markModelsHolder
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
        markModelsHolder.innerDispatcher = GetUserOffersMarkModelsDispatcher.unselectedMark()
        performMain { openLowTab(R.string.offers) }
        performDealerOffers { interactions.onParametersFab().waitUntilIsCompletelyDisplayed().performClick() }
    }

    @Test
    fun shouldApplyCorrectMark() {
        performFilter { interactions.onContainerWithHint(MARK_FIELD_NAME).performClick() }
        markModelsHolder.innerDispatcher = GetUserOffersMarkModelsDispatcher.unselectedModels(markModelsWatcher)
        performMark {
            interactions.onList().waitUntil(withItemsCount(2))
            interactions.onItemWithName(SELECTED_MARK).waitUntilIsCompletelyDisplayed().performClick()
        }
        checkFilter {
            isInputContainer(MARK_FIELD_NAME, SELECTED_MARK)
            markModelsWatcher.checkQueryParameter(MARK_MODEL_PARAM, SELECTED_MARK.toUpperCase())
        }
    }

    @Test
    fun shouldApplyCorrectModel() {
        performFilter { interactions.onContainerWithHint(MARK_FIELD_NAME).performClick() }
        markModelsHolder.innerDispatcher = GetUserOffersMarkModelsDispatcher.unselectedModels(markModelsWatcher)
        performMark { interactions.onItemWithName(SELECTED_MARK).waitUntilIsCompletelyDisplayed().performClick() }
        performFilter {
            waitBottomSheet()
            interactions.onContainerWithHint(MODEL_FIELD_NAME).performClick()
        }
        markModelsHolder.innerDispatcher = GetUserOffersMarkModelsDispatcher.selectedModels(markModelsWatcher)
        performModel {
            interactions.onModelList().waitUntil(withItemsCount(2))
            interactions.onItemWithName(SELECTED_MODEL).waitUntilIsCompletelyDisplayed().performClick()
        }
        checkFilter {
            checkFilter { isInputContainer(MODEL_FIELD_NAME, SELECTED_MODEL) }
            markModelsWatcher.checkQueryParameter(
                MARK_MODEL_PARAM,
                "${SELECTED_MARK.toUpperCase()}#${SELECTED_MODEL.toUpperCase()}"
            )
        }
    }
}
