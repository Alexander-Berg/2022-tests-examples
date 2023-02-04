package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SimilarOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SimilarOffersScreen
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
 * @author misha-kozlov on 12.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class P2PChatSimilarOffersTest : ChatButtonTest() {

    private val activityTestRule = SimilarOffersActivityTestRule(
        offerId = OFFER_ID,
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
    fun shouldStartChatWhenOfferChatButtonPressed() {
        configureWebServer {
            registerOffer()
            registerSimilarOffers()
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SimilarOffersScreen> {
            offerSnippet(SNIPPET_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { chatButton.click() }

            checkSnippetOfferChatViewState()
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody("ChatButtonTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                assetBody("P2PChatOfferCardTest/similarOffers.json")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "0"
    }
}
