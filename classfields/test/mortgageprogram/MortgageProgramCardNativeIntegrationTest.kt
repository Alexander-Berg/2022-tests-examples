package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageApplicationFormScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createNativeFormProgram
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 8/11/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramCardNativeIntegrationTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createNativeFormProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        activityTestRule
    )

    @Test
    fun shouldOpenNativeForm() {
        activityTestRule.launchActivity()

        val prefix = "MortgageProgramCardNativeIntegrationTest/shouldOpenNativeForm"
        onScreen<MortgageProgramCardScreen> {
            floatingNativeFormalizeButton.waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches("$prefix/floatingButtonState")
                .click()
        }
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { listView.contains(lastNameInputItem) }
            pressBack()
        }
        onScreen<MortgageProgramCardScreen> {
            listView.scrollTo(formalizeNativeButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .isViewStateMatches("$prefix/listButtonState")
                .click()
        }
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { listView.contains(lastNameInputItem) }
        }
    }

    @Test
    fun shouldOpenNativeFormInBankProgram() {
        configureWebServer {
            registerMortgageProgramBankSearch()
        }
        activityTestRule.launchActivity()

        val prefix = "MortgageProgramCardNativeIntegrationTest/shouldOpenNativeFormInBankProgram"
        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(ID_NATIVE_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .formalizeButton
                .isViewStateMatches("$prefix/snippetButtonState")
                .click()
        }
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { listView.contains(lastNameInputItem) }
        }
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

        private const val ID_NATIVE_INTEGRATION = "11"
    }
}
