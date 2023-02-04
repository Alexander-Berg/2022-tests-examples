package ru.auto.ara.test.other.tinkoff

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.preliminary.updatePreliminary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user.userSetupNone
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.burger.performBurger
import ru.auto.ara.core.robot.tinkoff.checkLoanLK
import ru.auto.ara.core.robot.tinkoff.performLoanLK
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled
import ru.auto.feature.burger.ui.BurgerMenuItem

@RunWith(AndroidJUnit4::class)
class TinkoffLKLoginTest {
    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"
    private val FIO = "Тест Тестов Тестович"
    private val EMAIL = "test@test.ru"
    private val BANK = "tinkoff"

    private val webServerRule = WebServerRule {
        userSetupNone()
        getPreliminary(BANK)
        updatePreliminary(BANK)
    }

    private var activityTestRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetPreferencesRule(),
        SetupTimeRule(date = "28.02.2019", localTime = "08:00", timeZoneId = "Europe/Moscow"),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
            }
        )
    )

    @Test
    fun shouldOpenTinkoffWebviewAfterLoginWhenEmptyClaims() {
        webServerRule.routing {
            getTinkoffClaims("empty")
        }
        openLoanLK()
        performLoanLK {
            fillLoanForm(FIO, EMAIL, PHONE)
            clickApplyLoanButton()
        }
        watchWebView {
            webServerRule.routing {
                oneOff {
                    userSetup()
                    postLoginOrRegisterSuccess()
                }
            }
            performLogin { loginWithPhoneAndCode(PHONE, CODE) }
            checkWebView {
                isWebViewContentDisplayed()
            }
        }.checkResult {
            checkUrlMatches("https://auto.ru/promo/autoru-tcs/?&amount=500000&term=5&bank_name=tinkoff")
        }
        Espresso.pressBack()
        checkLoanLK { isCalcLoanDisplayed() }
    }

    @Ignore // fix in https://st.yandex-team.ru/AUTORUAPPS-13910
    @Test
    fun shouldOpenFallbackWebviewAfterLoginWhenCancelStatus() {
        webServerRule.routing {
            getTinkoffClaims("cancel")
        }
        openLoanLK()
        performLoanLK { clickApplyLoanButton() }
        watchWebView {
            webServerRule.routing {
                oneOff { userSetup() }
            }
            performLogin { loginWithPhoneAndCode(PHONE, CODE) }
            checkWebView {
                isToolBarDisplayed()
            }
        }.checkResult {
            checkUrlMatches("https://auto.ru/promo/autoru-tcs/?&amount=500000&term=5&bank_name=alfabank")
        }
        Espresso.pressBack()
        checkLoanLK {
            isCalcLoanDisplayed(
                getResourceString(R.string.alfa_legal_name),
                loanRate = "7.7",
                monthlyPaymentValue = "от 10 000 \u20BD/мес.",
                loanAmountHint = getResourceString(R.string.loan_amount_hint, "5 млн")
            )
        }
    }

    @Test
    fun shouldOpenTinkoffWebviewAfterLoginWhenDraftClaims() {
        webServerRule.routing {
            getTinkoffClaims("draft")
        }
        openLoanLK()
        performLoanLK {
            fillLoanForm(FIO, EMAIL, PHONE)
            clickApplyLoanButton()
        }
        watchWebView {
            webServerRule.routing {
                oneOff {
                    userSetup()
                    postLoginOrRegisterSuccess()
                }
            }
            performLogin { loginWithPhoneAndCode(PHONE, CODE) }
            checkWebView {
                isWebViewContentDisplayed()
            }
        }.checkResult {
            checkUrlMatches("https://auto.ru/promo/autoru-tcs/?&hid=a3e93e033aede067f2755ceedb9df5dd&bank_name=tinkoff")
        }
        Espresso.pressBack()
        checkLoanLK { isDraftLoanDisplayed(withDisclaimer = true) }
    }

    @Test
    fun shouldNotOpenUrlAfterLoginWhenOnHoldStatus() {
        webServerRule.routing {
            getTinkoffClaims("hold")
        }
        openLoanLK()
        performLoanLK {
            fillLoanForm(FIO, EMAIL, PHONE)
            clickApplyLoanButton()
        }
        webServerRule.routing {
            oneOff {
                postLoginOrRegisterSuccess()
                userSetup()
            }
        }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkLoanLK { isHoldLoanDisplayed(withDisclaimer = true) }
    }

    private fun openLoanLK() {
        activityTestRule.launchActivity()
        performMain { openBurgerMenu() }
        performBurger { scrollAndClickOnCardItem(BurgerMenuItem.LoanCabinet) }
    }
}
