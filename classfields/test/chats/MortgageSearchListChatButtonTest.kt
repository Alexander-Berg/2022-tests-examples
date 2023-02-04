package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 3/23/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageSearchListChatButtonTest : ChatButtonTest() {

    private val activityTestRule = MortgageProgramListActivityTestRule(
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
            registerOffers("ChatButtonTest/offerWithSiteSearch.json")
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
            registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
            registerMortgageProgramSearch(
                "mortgageProgramSearchDefault.json",
                rgid = RGID
            )
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<MortgageProgramListScreen> {
            offerSnippet(SNIPPET_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenSiteOfferChatButtonPressed() {
        configureWebServer {
            registerOffers("ChatButtonTest/siteOfferSearch.json")
            registerSnippetSiteOfferChat()
            registerSnippetSiteOfferChatMessages()
            registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
            registerMortgageProgramSearch(
                "mortgageProgramSearchDefault.json",
                rgid = RGID
            )
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<MortgageProgramListScreen> {
            offerSnippet(SNIPPET_SITE_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetSiteOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenSiteChatButtonPressed() {
        configureWebServer {
            registerOffers("ChatButtonTest/siteSearch.json")
            registerSnippetSiteChat()
            registerSnippetSiteChatMessages()
            registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
            registerMortgageProgramSearch(
                "mortgageProgramSearchDefault.json",
                rgid = RGID
            )
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<MortgageProgramListScreen> {
            siteSnippet(SNIPPET_SITE_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetSiteChatViewState()
    }

    private fun DispatcherRegistry.registerOffers(responseFileName: String) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        responseFileName: String,
        page: Int = 0,
        rgid: String?,
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                rgid?.let { queryParam("rgid", it) }
                queryParam("page", page.toString())
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    companion object {

        private const val RGID = "587795"
    }
}
