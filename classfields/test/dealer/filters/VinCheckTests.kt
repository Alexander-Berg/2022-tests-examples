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
import ru.auto.ara.core.testdata.VIN_CHECK_PARAMS

@RunWith(Parameterized::class)
class VinCheckTests(private val testParams: TestParameter) {
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
            performFilter { interactions.onContainerWithHint(VIN_CHECKS_FIELD_NAME).performClick() }
            waitBottomSheet()
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
            interactions.onContainerWithHint(VIN_CHECKS_FIELD_NAME).performClick()
        }
        checkFilter { testParams.checkPicker(this) }
    }

    companion object {
        val watcher = RequestWatcher()
        private const val VIN_CHECKS_FIELD_NAME = "Проверки по VIN"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = VIN_CHECKS.map { arrayOf(it) }

        private val VIN_CHECKS = VIN_CHECK_PARAMS.map { vinCheckParam ->
            TestParameter(
                name = vinCheckParam.name,
                paramName = vinCheckParam.paramName,
                paramValues = vinCheckParam.values,
                description = vinCheckParam.description,
                dispatcher = GetUserOffersMarkModelsDispatcher.empty(requestWatcher = watcher),
                checkAtFilterScreenAndWatcher = {
                    interactions.onFilterValueWithText(vinCheckParam.name).checkIsCompletelyDisplayed()
                    watcher.checkQueryParameterMultipleValues(vinCheckParam.paramName, vinCheckParam.values)
                },
                checkPicker = {
                    VIN_CHECK_PARAMS.map { params ->
                        if (params.name == vinCheckParam.name) {
                            isCheckedOptionDisplayed(params.name)
                        } else {
                            isNotCheckedOptionDisplayed(params.name)
                        }
                    }
                }
            )
        }

        data class TestParameter(
            val name: String,
            val paramName: String,
            val paramValues: Set<String>,
            val description: String,
            val dispatcher: DelegateDispatcher,
            val checkAtFilterScreenAndWatcher: FilterRobotChecker.() -> Unit,
            val checkPicker: FilterRobotChecker.() -> Unit
        ) {
            override fun toString() = "$paramName=$description"
        }
    }
}
