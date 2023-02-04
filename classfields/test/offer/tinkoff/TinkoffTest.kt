package ru.auto.ara.test.offer.tinkoff

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.mapping.cheapOffer
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getPromoCodeCell
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.offercard.performVinReport
import ru.auto.ara.core.robot.tinkoff.checkLoan
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class TinkoffTest(private val testParameter: TestParameter) {

    private val webServerRule = WebServerRule {
        makeXmlForReportByOfferId(
            offerId = testParameter.offerId,
            dirType = NOT_BOUGHT,
            mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
        )
        makeXmlForOffer(
            offerId = testParameter.offerId,
            dirType = NOT_BOUGHT,
        )
        getTinkoffClaims(testParameter.loanStatus)
        getOffer(offerId = testParameter.offerId, category = "cars") { copy(offer = offer?.cheapOffer()) }
        userSetup()
        getPreliminary("tinkoff")
    }

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetupAuthRule(),
        SetPreferencesRule(),
        SetupTimeRule(date = "28.02.2019", localTime = "08:00"),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
            }
        )
    )

    @Before
    fun openCardAndScrollToLoan() {
        activityTestRule.launchDeepLinkActivity(testParameter.uri)

        performOfferCard { collapseAppBar() }
        performOfferCardVin { clickShowFreeReport() }
        checkVinReport { isVinReport() }
        waitSomething(300, TimeUnit.MILLISECONDS)
        performVinReport { close() }
        checkOfferCard { isOfferCard() }
        performOfferCard { scrollToLoanBlock() }
        checkLoan { isHeadLoanBlockDisplayed() }
    }

    @Test
    fun shouldSeeCorrectLoanState() {
        testParameter.check()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = (listOf(
            TestParameter(
                description = "calculator",
                loanStatus = "empty",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isCalcLoanDisplayed(
                            loanAmount = "720 000",
                            firstPaymentValue = "180 000 ₽",
                            monthlyPaymentValue = "от 15 400 \u20BD/мес."
                        )
                    }
                }
            ),
            TestParameter(
                description = "continue your loanDraft",
                loanStatus = "draft",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isDraftLoanDisplayed() } }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "new",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isHoldLoanDisplayed() } }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "hold",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isHoldLoanDisplayed() } }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "client_verification",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isHoldLoanDisplayed() } }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "first_agreement",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isHoldLoanDisplayed() } }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "meeting",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isHoldLoanDisplayed() } }
            ),
            TestParameter(
                description = "button send this car",
                loanStatus = "wait_car",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isApprovedLoanDisplayed() } }
            ),
            TestParameter(
                description = "this car is waiting approval",
                loanStatus = "auto_verification",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isThisCarWaitApprovalLoanDisplayed() } }
            ),
            TestParameter(
                description = "this car is rejected",
                loanStatus = "auto_verification",
                offerId = "1084782777-a4467715",
                uri = "https://auto.ru/cars/used/sale/1084782777-a4467715",
                check = { checkLoan { isRejectedCarLoanDisplayed() } }
            ),
            TestParameter(
                description = "another car is waiting approval",
                loanStatus = "auto_verification",
                offerId = "1084044743-07c46fb3",
                uri = "https://auto.ru/cars/used/sale/1084044743-07c46fb3",
                check = { checkLoan { isAnotherCarWaitApprovalLoanDisplayed() } }
            ),
            TestParameter(
                description = "this car is waiting approval",
                loanStatus = "second_agreement",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isThisCarWaitApprovalLoanDisplayed() } }
            ),
            TestParameter(
                description = "this car is rejected",
                loanStatus = "second_agreement",
                offerId = "1084782777-a4467715",
                uri = "https://auto.ru/cars/used/sale/1084782777-a4467715",
                check = { checkLoan { isRejectedCarLoanDisplayed() } }
            ),
            TestParameter(
                description = "button send this car",
                loanStatus = "second_agreement",
                offerId = "1084044743-07c46fb3",
                uri = "https://auto.ru/cars/used/sale/1084044743-07c46fb3",
                check = { checkLoan { isApprovedLoanDisplayed() } }
            ),
            TestParameter(
                description = "issue loan",
                loanStatus = "issue",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = { checkLoan { isIssueLoanDisplayed() } }
            )
        )).map { arrayOf(it) }
    }

    data class TestParameter(
        val description: String,
        val loanStatus: String,
        val offerId: String,
        val uri: String,
        val check: () -> Unit,
    ) {
        override fun toString() = "$description loanStatus=$loanStatus"
    }
}
