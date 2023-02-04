package ru.auto.ara.test.offercard.loan

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.carfax.report.getNoCarfaxReportDispatcher
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.preliminary.updatePreliminary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.mapping.cheapOffer
import ru.auto.ara.core.robot.bankloan.checkCreditPreliminaryPicker
import ru.auto.ara.core.robot.bankloan.performCreditPreliminary
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.ara.html.AutoLinks
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import ru.auto.core_ui.util.removeHtml
import ru.auto.data.model.credit.Bank
import ru.auto.experiments.Experiments
import ru.auto.experiments.creditBankCard
import ru.auto.experiments.loanBrokerEnabled

@RunWith(Parameterized::class)
class LoanPreliminaryDisclaimerTest(private val testParameter: TestParameter) {
    private val webServerRule = WebServerRule {
        userSetup()
        getNoCarfaxReportDispatcher()
        getTinkoffClaims("empty")
        getOffer("1082957054-8d55bf9a") { copy(offer = offer?.cheapOffer()) }
        updatePreliminary(testParameter.urlBank)
        getPreliminary(testParameter.urlBank)
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        SetupAuthRule(),
        SetPreferencesRule(),
        activityTestRule,
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
                Experiments::creditBankCard then testParameter.bank
            }
        )
    )

    @Before
    fun openCardAndScrollToLoan() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")

        checkOfferCard { isOfferCard() }
        performOfferCard {
            collapseAppBar()
            scrollToLoanBottomDivider()
        }
    }

    @Test
    fun shouldSeeCorrectDisclaimer() {
        performOfferCard {
            clickPersonalAgreement(testParameter.disclaimerClickableText)
        }
        testParameter.checkDisclaimer()
        testParameter.checkLink()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<TestParameter> = (listOf(
            TestParameter(
                bank = "sravni",
                disclaimerClickableText = "условия обработки данных",
                checkDisclaimer = {
                    checkCreditPreliminaryPicker {
                        isDisclaimerWithCorrectContent(
                            title = getResourceString(R.string.legal_disclaimer_title),
                            text = getResourceString(R.string.legal_disclaimer_sravni)
                        )
                    }
                },
                urlBank = Bank.SRAVNI.value
            ),
            TestParameter(
                bank = "gazprom",
                disclaimerClickableText = "условия обработки данных",
                checkDisclaimer = {
                    checkCreditPreliminaryPicker {
                        isDisclaimerWithCorrectContent(
                            title = getResourceString(R.string.legal_disclaimer_title),
                            text = getResourceString(R.string.legal_disclaimer_gazprom)
                        )
                    }
                },
                urlBank = Bank.GAZPROM.value
            ),
            TestParameter(
                bank = "alfa",
                checkDisclaimer = {
                    checkCreditPreliminaryPicker {
                        isDisclaimerWithCorrectContent(
                            title = getResourceString(R.string.legal_disclaimer_title),
                            text = getResourceString(R.string.legal_disclaimer_alfa).removeHtml()
                        )
                    }
                },
                urlBank = Bank.ALFA.value,
                checkLink = {
                    watchWebView {
                        performCreditPreliminary {
                            clickTermsInAgreement()
                        }
                    }.checkResult {
                        checkUrlMatches(AutoLinks.TERMS_OF_SERVICE)
                    }
                }
            ),
            TestParameter(
                bank = "tinkoff",
                checkDisclaimer = {
                    checkCreditPreliminaryPicker {
                        isDisclaimerWithCorrectContent(
                            title = getResourceString(R.string.legal_disclaimer_title),
                            text = getResourceString(R.string.legal_disclaimer_tinkoff)
                        )
                    }
                },
                urlBank = Bank.TINKOFF.value
            )
        ))
    }

    data class TestParameter(
        val bank: String,
        val checkDisclaimer: () -> Unit,
        val disclaimerClickableText: String = "условия обработки персональных данных",
        val checkLink: () -> Unit = {},
        val urlBank: String,
    ) {
        override fun toString() = "bank $bank"
    }
}
