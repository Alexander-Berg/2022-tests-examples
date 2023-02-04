package ru.auto.ara.test.dealer.filters

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.dealer.GetDealerCampaignsDispatcher
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersMarkModelsDispatcher
import ru.auto.ara.core.robot.dealeroffers.performDealerOffers
import ru.auto.ara.core.robot.searchfeed.FilterRobotChecker
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.STATUS_PARAMS

@RunWith(Parameterized::class)
class StatusTest(private val testParams: TestParameter) {
    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    private val webServerRule = WebServerRule {
        userSetupDealer()
        delegateDispatchers(
            GetUserOffersDispatcher.dealerOffers(),
            GetDealerCampaignsDispatcher("all"),
            testParams.dispatcher
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
            interactions.onInputContainer(STATUS_FIELD_NAME).performClick()
            clickOption(testParams.name)
        }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestsAtFilterScreen() {
        checkFilter { testParams.checkAtFilterScreenAndWatcher(this) }
    }

    @Test
    fun shouldSeeCorrectStateAtBottomSheet() {
        performFilter {
            waitBottomSheet()
            interactions.onInputContainer(STATUS_FIELD_NAME).performClick()
        }
        checkFilter { testParams.checkPicker(this) }
    }

    companion object {
        val watcher = RequestWatcher()
        const val STATUS_FIELD_NAME = "Статус"
        private const val STATUS_PARAM = "status"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = STATUSES.map { arrayOf(it) }

        private val STATUSES = STATUS_PARAMS.map { (name, param) ->
            TestParameter(
                name = name,
                param = param,
                dispatcher = GetUserOffersMarkModelsDispatcher.empty(requestWatcher = watcher),
                checkAtFilterScreenAndWatcher = {
                    isInputContainer(STATUS_FIELD_NAME, name)
                    watcher.checkQueryParameter(STATUS_PARAM, param)
                },
                checkPicker = {
                    STATUS_PARAMS.map { (optionName) ->
                        if (optionName == name) {
                            isCheckedOptionDisplayed(optionName)
                        } else {
                            isNotCheckedOptionDisplayed(optionName)
                        }
                    }
                }
            )
        }

        data class TestParameter(
            val name: String,
            val param: String,
            val dispatcher: DelegateDispatcher,
            val checkAtFilterScreenAndWatcher: FilterRobotChecker.() -> Unit,
            val checkPicker: FilterRobotChecker.() -> Unit
        ) {
            override fun toString() = param
        }
    }

}
