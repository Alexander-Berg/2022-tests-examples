package ru.auto.ara.test.other.tinkoff

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.offer_card.TinkoffDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.burger.performBurger
import ru.auto.ara.core.robot.searchfeed.SearchFeedRobotChecker
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.tinkoff.LoanLKRobot
import ru.auto.ara.core.robot.tinkoff.performLoanLK
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.data.model.filter.StateGroup
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled
import ru.auto.feature.burger.ui.BurgerMenuItem

@RunWith(Parameterized::class)
class TinkoffSendAutoTest(private val testParameter: TestParameter) {
    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", testParameter.offerId),
            TinkoffDispatcher(testParameter.loanStatus)
        )
        postEmptyGarageListing()
    }
    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupAuthRule(),
        SetupTimeRule("28.02.2019", "08:00"),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
            }
        )
    )

    @Before
    fun setUp() {
        activityTestRule.launchActivity()
        performMain { openBurgerMenu() }
        performBurger { scrollAndClickOnCardItem(BurgerMenuItem.LoanCabinet) }
        performLoanLK { testParameter.performClick(this) }
        performSearchFeed { waitSearchFeed() }
    }

    @Test
    fun shouldSeeCorrectLoanState() {
        checkSearchFeed { testParameter.check(this) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = (listOf(
            TestParameter(
                loanStatus = "wait_car_without_offer",
                offerId = "",
                uri = "",
                performClick = {
                    interactions.onCarSelectButton().performClick()
                },
                check = {
                    isEmptyMMNGFilterDisplayed()
                    isStateSelectorChecked(StateGroup.ALL)
                }
            ),
            TestParameter(
                loanStatus = "second_agreement",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                performClick = {
                    interactions.onChooseOtherButton().performClick()
                },
                check = {
                    isEmptyMMNGFilterDisplayed()
                    isStateSelectorChecked(StateGroup.ALL)
                }
            )
        )).map { arrayOf(it) }
    }

    data class TestParameter(
        val loanStatus: String,
        val offerId: String,
        val uri: String,
        val performClick: LoanLKRobot.() -> Unit,
        val check: SearchFeedRobotChecker.() -> Unit,
    ) {
        override fun toString() = "loanStatus=$loanStatus"
    }
}
