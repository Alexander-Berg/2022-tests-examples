package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FavoriteListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FavoriteOffersScreen
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
 * @author rogovalex on 15/03/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoriteListChatButtonTest : ChatButtonTest() {

    private val activityTestRule = FavoriteListActivityTestRule(launchActivity = false)
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
            registerOffers()
            registerFavoriteIds(SNIPPET_OFFER_ID)
            registerFavoriteIds(SNIPPET_OFFER_ID)
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<FavoriteOffersScreen> {
            offerSnippet(SNIPPET_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .chatButton
                .click()
        }

        checkSnippetOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenSiteOfferChatButtonPressed() {
        configureWebServer {
            registerSiteOffers()
            registerFavoriteIds(SNIPPET_SITE_OFFER_ID)
            registerFavoriteIds(SNIPPET_SITE_OFFER_ID)
            registerSnippetSiteOfferChat()
            registerSnippetSiteOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<FavoriteOffersScreen> {
            offerSnippet(SNIPPET_SITE_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .chatButton
                .click()
        }

        checkSnippetSiteOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenSiteChatButtonPressed() {
        configureWebServer {
            registerSites()
            registerFavoriteIds("site_$SNIPPET_SITE_ID")
            registerFavoriteIds("site_$SNIPPET_SITE_ID")
            registerFavoriteIds("site_$SNIPPET_SITE_ID")
            registerSnippetSiteChat()
            registerSnippetSiteChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<FavoriteOffersScreen> {
            selectorListView.waitUntil { listView.contains(this) }
                .scrollTo(siteSelectorItem)
                .click()

            siteSnippet(SNIPPET_SITE_ID)
                .waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .chatButton
                .click()
        }

        checkSnippetSiteChatViewState()
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", SNIPPET_OFFER_ID)
            },
            response {
                assetBody("ChatButtonTest/offerWithSiteSearch.json")
            }
        )
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

    private fun DispatcherRegistry.registerSites() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SNIPPET_SITE_ID)
            },
            response {
                assetBody("ChatButtonTest/siteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerFavoriteIds(id: String) {
        register(
            request {
                path("1.0/favorites.json")
            },
            response {
                setBody(
                    """
                                {
                                   "response":{
                                      "actual":[
                                         "$id"
                                      ],
                                      "outdated":[],
                                      "relevant":[
                                         "$id"
                                      ]
                                   }
                                }
                    """.trimIndent()
                )
            }
        )
    }
}
