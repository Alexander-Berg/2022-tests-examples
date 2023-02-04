package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createAlfaProgram
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 8/11/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramCardAlfaIntegrationTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createAlfaProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldOpenAlfaForm() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            floatingAlfaFormalizeButton.waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("floatingButtonState"))
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_URL) }
            pressBack()
        }
        onScreen<MortgageProgramCardScreen> {
            listView.scrollTo(rateConditionsTitleItem)
            priceInputView.replaceText(PRICE_CHANGED)
            periodInputView.replaceText(PERIOD_CHANGED)
            initialPaymentInputView.replaceText(INITIAL_PAYMENT_CHANGED)

            listView.scrollTo(formalizeAlfaButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .isViewStateMatches(getTestRelatedFilePath("listButtonState"))
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_CHANGED_URL) }
        }
    }

    @Test
    fun shouldOpenAlfaFormWithoutCalculator() {
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            floatingAlfaFormalizeButton.waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("floatingButtonState"))
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_URL) }
            pressBack()
        }
        onScreen<MortgageProgramCardScreen> {
            listView.scrollTo(formalizeAlfaButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .isViewStateMatches(getTestRelatedFilePath("listButtonState"))
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_URL) }
        }
    }

    @Test
    fun shouldOpenAlfaFormInBankProgram() {
        configureWebServer {
            registerMortgageProgramBankSearch()
            registerCalculatorConfig()
            registerCalculatorResult()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(ID_ALFA_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .formalizeButton
                .isViewStateMatches(getTestRelatedFilePath("snippetButtonState"))
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_URL) }
            pressBack()
        }
        onScreen<MortgageProgramCardScreen> {
            listView.scrollTo(calculatorTitleItem)
            priceInputView.replaceText(PRICE_CHANGED)
            periodInputView.replaceText(PERIOD_CHANGED)
            initialPaymentInputView.replaceText(INITIAL_PAYMENT_CHANGED)

            mortgageProgramSnippet(ID_ALFA_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .formalizeButton
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_CHANGED_URL) }
        }
    }

    @Test
    fun shouldOpenAlfaFormInBankProgramWithoutCalculator() {
        configureWebServer {
            registerMortgageProgramBankSearch()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(ID_ALFA_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .formalizeButton
                .isViewStateMatches(getTestRelatedFilePath("snippetButtonState"))
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_URL) }
        }
    }

    @Test
    fun shouldShowAlfaMetaInfoAndTheme() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult()
        }
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            specialConditionsItem.waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("specialConditionsState"))

            listView.contains(mortgageDisclaimerItem)
                .isViewStateMatches(getTestRelatedFilePath("disclaimerState"))

            listView.isItemsStateMatches(
                getTestRelatedFilePath("calculatorState"),
                calculatorTitleItem,
                rateConditionsTitleItem,
                inclusive = false
            )
        }
    }

    private fun DispatcherRegistry.registerCalculatorConfig() {
        register(
            request {
                path("2.0/mortgage/program/$PROGRAM_ID/calculator")
            },
            response {
                assetBody("mortgage/calculatorConfig.json")
            }
        )
    }

    private fun DispatcherRegistry.registerCalculatorResult() {
        register(
            request {
                path("2.0/mortgage/program/$PROGRAM_ID/calculator")
            },
            response {
                assetBody("mortgage/calculatorResultDefault.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramBankSearch() {
        register(
            request {
                path("2.0/mortgage/program/search")
            },
            response {
                assetBody("mortgage/programSearch.json")
            }
        )
    }

    companion object {

        private const val PROGRAM_ID = "1"
        private const val ID_ALFA_INTEGRATION = "10"
        private const val PERIOD_CHANGED = "20"
        private const val PRICE_CHANGED = "10000000"
        private const val INITIAL_PAYMENT_CHANGED = "5000000"

        private const val MORTGAGE_PROPOSITION_URL = "https://alfabank.ru/get-money/mortgage/" +
            "iform/?maternalCapital=false&payrollClient=false&partnerProperty=true" +
            "&utm_source=yandex_realty&utm_medium=month&filterCity=msc&" +
            "type=construction&creditTerm=15&estateCost=3000000&initialFee=900000&" +
            "stateSubsidy=false&utm_campaign=marketing_feb-dec21_mortgage_omd&" +
            "utm_content=mortgage-section_iframe_application-form__always-on&" +
            "utm_term=yarealty-uid_1&" +
            "platformId=yandex-realty_month_marketing_feb-dec21_mortgage_omd_mortgage-" +
            "section_iframe_application-form__always-on_yarealty-" +
            "uid_1&only-content=true"

        private const val MORTGAGE_PROPOSITION_CHANGED_URL =
            "https://alfabank.ru/get-money/mortgage/" +
                "iform/?maternalCapital=false&payrollClient=false&partnerProperty=true" +
                "&utm_source=yandex_realty&utm_medium=month&filterCity=msc&" +
                "type=construction&creditTerm=$PERIOD_CHANGED&estateCost=$PRICE_CHANGED&" +
                "initialFee=$INITIAL_PAYMENT_CHANGED&stateSubsidy=false&" +
                "utm_campaign=marketing_feb-dec21_mortgage_omd&" +
                "utm_content=mortgage-section_iframe_application-form__always-on&" +
                "utm_term=yarealty-uid_1&" +
                "platformId=yandex-realty_month_marketing_feb-dec21_mortgage_" +
                "omd_mortgage-section_iframe_application-form__always-on_yarealty-" +
                "uid_1&only-content=true"
    }
}
