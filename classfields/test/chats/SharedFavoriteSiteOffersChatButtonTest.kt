package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SharedFavoritesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.ModelType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 02.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SharedFavoriteSiteOffersChatButtonTest : ChatButtonTest() {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.OFFER,
        objectIds = listOf(SNIPPET_SITE_OFFER_ID),
        launchActivity = false
    )
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldStartChatWhenSiteOfferChatButtonPressed() {
        configureWebServer {
            registerSiteOffers()
            registerSnippetSiteOfferChat()
            registerSnippetSiteOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SharedFavoritesScreen> {
            offerSnippet(SNIPPET_SITE_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    chatButton.click()
                }
        }

        checkSnippetSiteOfferChatViewState()
    }

    private fun DispatcherRegistry.registerSiteOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", SNIPPET_SITE_OFFER_ID)
            },
            response {
                assetBody("ChatButtonTest/siteOfferSearch.json")
            }
        )
    }
}
