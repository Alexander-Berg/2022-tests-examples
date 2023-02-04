package ru.auto.ara.test.offer.tinkoff

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.carfax.report.getNoCarfaxReportDispatcher
import ru.auto.ara.core.dispatchers.journal.getEmptyJournals
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.preliminary.updatePreliminary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.mapping.cheapOffer
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.tinkoff.checkLoan
import ru.auto.ara.core.robot.tinkoff.performLoan
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.buildLoanUrl
import ru.auto.ara.core.utils.closeSoftKeyboard
import ru.auto.ara.core.utils.getDeepLinkIntent
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import ru.auto.data.model.credit.Bank
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class TinkoffLoginTest {
    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"
    private val offerId = "1082957054-8d55bf9a"
    private val webServerRule = WebServerRule {
        userSetup()
        postLoginOrRegisterSuccess()
        getOffer(offerId = offerId)
        getNoCarfaxReportDispatcher()
        getEmptyJournals()
    }
    private val activityTestRule = ActivityTestRule(DeeplinkActivity::class.java, false, false)

    private val fio = "Тестов Тест Тестович"
    private val email = "test@test.test"
    private val phone = "+70000000000"

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetPreferencesRule(),
        SetupTimeRule(date = "28.02.2019", localTime = "08:00", timeZoneId = "Europe/Moscow"),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled.then(false)
            }
        )
    )

    @Test
    fun shouldOpenTinkoffPreliminaryAfterLoginWhenEmptyClaims() {
        val urlBank = Bank.TINKOFF.value
        webServerRule.routing {
            getOffer(offerId = offerId) { copy(offer = offer?.cheapOffer()) }
            getTinkoffClaims("empty")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
        }
        applyForLoan()
        checkWebViewOpenedWithUrl(
            "https://auto.ru/promo/autoru-tcs/?" +
                "&amount=720000&term=5&fee=180000&only_frame=true&bank_name=tinkoff"
        )
        Espresso.pressBack()
        checkLoan {
            isCalcLoanDisplayed(
                loanAmount = "720 000",
                firstPaymentValue = "180 000 ₽",
                monthlyPaymentValue = "от 15 400 \u20BD/мес."
            )
        }
    }

    @Test
    fun shouldOpenAlphaPreliminaryAfterLoginWhenCancelStatus() {
        val urlBank = Bank.ALFA.value
        webServerRule.routing {
            getTinkoffClaims("cancel")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
        }
        val intent = getDeepLinkIntent("https://auto.ru/cars/used/sale/$offerId")
        activityTestRule.launchActivity(intent)
        performOfferCard { collapseAppBar() }
        checkOfferCard { isOfferCard() }
        performAndApplySetupLoan()
        checkWebViewOpenedWithBank(urlBank)
        Espresso.pressBack()
        checkLoan {
            isCalcLoanDisplayed(
                loanAmount = "1 760 000",
                loanRate = "6.5",
                monthlyPaymentValue = "от 34 650 ₽/мес.",
                loanAmountHint = getResourceString(R.string.loan_amount_hint, "5 млн")
            )
        }
    }

    @Test
    fun shouldOpenTinkoffWebviewDraftAfterLoginWhenDraftStatus() {
        val urlBank = Bank.TINKOFF.value
        webServerRule.routing {
            getTinkoffClaims("draft")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
            getOffer(offerId = offerId) { copy(offer = offer?.cheapOffer()) }
        }
        applyForLoan()
        webServerRule.routing {
            userSetup()
        }

        watchWebView {
            performLogin { loginWithPhoneAndCode(PHONE, CODE) }
            waitSomething(3, TimeUnit.SECONDS) // wait for webview to show up
            checkWebView {
                isToolBarDisplayed()
                isWebViewContentDisplayed()
            }
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/?" +
                    "&hid=a3e93e033aede067f2755ceedb9df5dd&only_frame=true&bank_name=tinkoff"
            )
            checkWithCookies(listOf("autoruuid"), "auto.ru") // "autoru_sid" cookie is flacky
        }

        Espresso.pressBack()
        checkLoan { isDraftLoanDisplayed() }
    }

    @Test
    fun shouldNotOpenDraftOrPreliminaryWhenOnHoldStatus() {
        val urlBank = Bank.TINKOFF.value
        webServerRule.routing {
            getTinkoffClaims("hold")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
            getOffer(offerId = offerId) { copy(offer = offer?.cheapOffer()) }
        }
        applyForLoan()
        webServerRule.routing { userSetup() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }

        checkOfferCard { isOfferCard() }
        checkLoan { isHoldLoanDisplayed() }
    }

    private fun checkWebViewOpenedWithBank(urlBank: String) {
        checkWebViewOpenedWithUrl(buildLoanUrl(urlBank))
    }

    private fun checkWebViewOpenedWithUrl(url: String) {
        webServerRule.routing {
            postLoginOrRegisterSuccess()
            userSetup()
        }
        watchWebView {
            performLogin {
                waitCode()
                interactions.onCodeInput().performReplaceText("0000")
            }
            waitSomething(3, TimeUnit.SECONDS) // wait for webview to show up
        }.checkResult {
            checkUrlMatches(url)
            checkWithCookies(listOf("autoruuid"), "auto.ru")
        }
    }

    private fun performAndApplySetupLoan() {
        performOfferCard { scrollToLoanBottomDivider() }
        performLoan { fillLoanForm(fio, email, phone) }
        closeSoftKeyboard()
        waitSomething(2, TimeUnit.SECONDS)
        performLoan { scrollAndClickApplyLoanButton() }
    }

    private fun applyForLoan() {
        val intent = getDeepLinkIntent("https://auto.ru/cars/used/sale/$offerId")
        activityTestRule.launchActivity(intent)
        checkOfferCard { isOfferCard() }
        performOfferCard { collapseAppBar() }
        performLoan { scrollAndClickApplyLoanButton() }
    }
}
