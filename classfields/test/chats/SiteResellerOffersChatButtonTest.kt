package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SiteResellerOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteResellerOffersScreen
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
 * @author rogovalex on 15/03/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteResellerOffersChatButtonTest : ChatButtonTest() {

    private val activityTestRule = SiteResellerOffersTestRule(
        siteId = "1",
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
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteResellerOffersScreen> {
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
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteResellerOffersScreen> {
            offerSnippet(SNIPPET_SITE_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetSiteOfferChatViewState()
    }

    private fun DispatcherRegistry.registerOffers(fileName: String) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody(fileName)
            }
        )
    }
}
