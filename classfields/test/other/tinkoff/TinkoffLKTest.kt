package ru.auto.ara.test.other.tinkoff

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.burger.performBurger
import ru.auto.ara.core.robot.tinkoff.LoanLKRobotChecker
import ru.auto.ara.core.robot.tinkoff.checkLoanLK
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled
import ru.auto.feature.burger.ui.BurgerMenuItem

@RunWith(Parameterized::class)
class TinkoffLKTest(private val testParameter: TestParameter) {

    private val webServerRule = WebServerRule {
        testParameter.offerId?.let { getOffer(it) }
        getTinkoffClaims(testParameter.loanStatus)
        userSetup()
        postEmptyGarageListing()
    }

    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupAuthRule(),
        SetupTimeRule("28.02.2019", "21:00"),
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
        performBurger {
            scrollAndClickOnCardItem(BurgerMenuItem.LoanCabinet)
        }
    }

    @Test
    fun shouldSeeCorrectLoanState() {
        checkLoanLK { testParameter.check(this) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = (listOf(
            TestParameter(
                description = "calc displayed",
                loanStatus = "empty",
                offerId = null,
                uri = "",
                check = { isCalcLoanDisplayed() }
            ),
            TestParameter(
                description = "draft",
                loanStatus = "draft",
                offerId = null,
                uri = "",
                check = { isDraftLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "new",
                offerId = null,
                uri = "",
                check = { isHoldLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "hold",
                offerId = null,
                uri = "",
                check = { isHoldLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "client_verification",
                offerId = null,
                uri = "",
                check = { isHoldLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "first_agreement",
                offerId = null,
                uri = "",
                check = { isHoldLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "meeting",
                offerId = null,
                uri = "",
                check = { isHoldLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "wait_car_without_offer",
                loanStatus = "wait_car_without_offer",
                offerId = null,
                uri = "",
                check = { isCarWaitLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "wait_car_with_offer",
                loanStatus = "wait_car",
                offerId = "1080290554-5349dabf",
                uri = "https://auto.ru/cars/used/sale/1080290554-5349dabf",
                check = { isCarWaitWithRejectedCarLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "wait_car_with_inactive_offer",
                loanStatus = "wait_car_with_inactive_offer",
                offerId = "1077957027-c7abdb2f",
                uri = "https://auto.ru/cars/used/sale/1077957027-c7abdb2f",
                check = { isSoldCarWaitWithRejectedCarLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "auto_verification_with_offer",
                loanStatus = "auto_verification",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { isAutoVerificationLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "second_agreement_with_offer",
                loanStatus = "second_agreement",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { isSecondAgreementLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "second_agreement_without_offer",
                loanStatus = "second_agreement_without_offer",
                offerId = null,
                uri = "",
                check = { isSecondAgreementWithoutCarLoanDisplayed(withDisclaimer = true) }
            ),
            TestParameter(
                description = "issue",
                loanStatus = "issue",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { isIssueLoanDisplayed(withDisclaimer = true) }
            )
        )).map { arrayOf(it) }
    }

    data class TestParameter(
        val description: String,
        val loanStatus: String,
        val offerId: String?,
        val uri: String,
        val check: LoanLKRobotChecker.() -> Unit,
    ) {
        override fun toString() = "$description loanStatus=$loanStatus"
    }
}
