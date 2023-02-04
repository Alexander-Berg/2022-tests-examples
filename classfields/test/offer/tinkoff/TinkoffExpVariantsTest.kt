package ru.auto.ara.test.offer.tinkoff

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.tinkoff.checkLoan
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.experiments.Experiments
import ru.auto.experiments.creditBankCard
import ru.auto.experiments.loanBrokerEnabled

@RunWith(Parameterized::class)
class TinkoffExpVariantsTest(private val testParameter: TestParameter) {
    private val webServerRule = WebServerRule {
        userSetup()
        getTinkoffClaims(testParameter.loanStatus)
        getOffer(testParameter.offerId)
        getPreliminary("tinkoff")
        getPreliminary("sravniru")
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        SetupAuthRule(),
        SetupTimeRule("28.02.2019", localTime = "08:00"),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
                Experiments::creditBankCard then "sravni"
            }
        )
    )

    @Before
    fun openCardAndScrollToLoan() {
        activityTestRule.launchDeepLinkActivity(testParameter.uri)

        checkOfferCard { isOfferCard() }
        performOfferCard { scrollToLoanBlock() }
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
                offerId = "1082957054-8d55bf9z", // this offer has price less than 2M
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9z",
                check = {
                    checkLoan {
                        isSravniHeaderDisplayed()
                        isSravniCalcDisplayed(
                            loanAmount = "180 000",
                            monthlyPaymentValue = "от 3 600 ₽/мес.",
                            firstPaymentValue = "40 000 ₽"
                        )
                    }
                }
            ),
            TestParameter(
                description = "calculator",
                loanStatus = "cancel",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isSravniHeaderDisplayed()
                        isSravniCalcDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "continue your loanDraft",
                loanStatus = "draft",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isSravniHeaderDisplayed()
                        isSravniCalcDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "new",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isHoldLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "hold",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isSravniHeaderDisplayed()
                        isSravniCalcDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "client_verification",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isHoldLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "first_agreement",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isHoldLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "hold",
                loanStatus = "meeting",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isHoldLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "button send this car",
                loanStatus = "wait_car",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isApprovedLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "this car is waiting approval",
                loanStatus = "auto_verification",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isThisCarWaitApprovalLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "this car is rejected",
                loanStatus = "auto_verification",
                offerId = "1084782777-a4467715",
                uri = "https://auto.ru/cars/used/sale/1084782777-a4467715",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isRejectedCarLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "another car is waiting approval",
                loanStatus = "auto_verification",
                offerId = "1084044743-07c46fb3",
                uri = "https://auto.ru/cars/used/sale/1084044743-07c46fb3",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isAnotherCarWaitApprovalLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "this car is waiting approval",
                loanStatus = "second_agreement",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isThisCarWaitApprovalLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "this car is rejected",
                loanStatus = "second_agreement",
                offerId = "1084782777-a4467715",
                uri = "https://auto.ru/cars/used/sale/1084782777-a4467715",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isRejectedCarLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "button send this car",
                loanStatus = "second_agreement",
                offerId = "1084044743-07c46fb3",
                uri = "https://auto.ru/cars/used/sale/1084044743-07c46fb3",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isApprovedLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "issue loan",
                loanStatus = "issue",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isHeadLoanBlockDisplayed()
                        isIssueLoanDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "reject loan on not rejected car",
                loanStatus = "reject",
                offerId = "1082957054-8d55bf9a",
                uri = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a",
                check = {
                    checkLoan {
                        isSravniHeaderDisplayed()
                        isSravniCalcDisplayed()
                    }
                }
            ),
            TestParameter(
                description = "reject loan on rejected car",
                loanStatus = "reject",
                offerId = "1084782777-a4467715",
                uri = "https://auto.ru/cars/used/sale/1084782777-a4467715",
                check = {
                    checkLoan {
                        isSravniHeaderDisplayed()
                        isCalcLoanDisplayed(
                            loanAmount = "1 000 000",
                            firstPaymentValue = "250 000 ₽",
                            monthlyPaymentValue = "от 19 950 ₽/мес.",
                            loanAmountHint = getResourceString(R.string.loan_amount_hint, "5 млн"),
                            loanRate = "7"
                        )
                    }
                }
            )
        )).map { arrayOf(it) }
    }

    data class TestParameter(
        val description: String,
        val loanStatus: String,
        val offerId: String,
        val uri: String,
        val check: () -> Unit
    ) {
        override fun toString() = "$description loanStatus=$loanStatus"
    }
}
