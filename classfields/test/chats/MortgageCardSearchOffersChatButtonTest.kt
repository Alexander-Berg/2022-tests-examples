package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createSecondaryFlatProgram
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 02.08.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageCardSearchOffersChatButtonTest : ChatButtonTest() {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createSecondaryFlatProgram(),
        launchActivity = false
    )
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldStartChatWhenOfferChatButtonPressed() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult()
            registerOffers()
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<MortgageProgramCardScreen> {
            offerSnippet(SNIPPET_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetOfferChatViewState()
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("ChatButtonTest/offerWithSiteSearch.json")
            }
        )
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

    companion object {

        private const val PROGRAM_ID = "1"
    }
}
