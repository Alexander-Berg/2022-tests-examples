package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
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
 * @author rogovalex on 01/02/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class P2PChatOfferCardTest : ChatButtonTest() {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldStartChatWhenButtonPressed() {
        configureWebServer {
            registerOffer()
            registerEmptyChat()
            registerEmptyListMessages(CHAT_ID)
            registerEmptyListMessages(CHAT_ID)
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }

            floatingCommButtons.isViewStateMatches(
                "P2PChatOfferCardTest/shouldStartChatWhenButtonPressed/floatingButtons"
            )

            floatingChatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(
                "P2PChatOfferCardTest/shouldStartChatWhenButtonPressed/chatViewState"
            )

            pressBack()
        }

        onScreen<OfferCardScreen> {
            appBar.collapse()
            listView.scrollTo(commButtonsItem)
            listView.scrollByFloatingButtonHeight()

            commButtonsItem.view.isViewStateMatches(
                "P2PChatOfferCardTest/shouldStartChatWhenButtonPressed/contentButtons"
            )

            chatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(
                "P2PChatOfferCardTest/shouldStartChatWhenButtonPressed/chatViewState"
            )
        }
    }

    @Test
    fun shouldStartChatWhenSimilarOfferChatButtonPressed() {
        configureWebServer {
            registerOffer()
            registerSimilarOffers()
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<OfferCardScreen> {
            waitUntil { floatingChatButton.isCompletelyDisplayed() }

            appBar.collapse()

            similarOfferSnippet(SNIPPET_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetOfferChatViewState()
    }

    @Test
    fun shouldNotShowChatButton() {
        configureWebServer {
            registerOfferNoChat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCallButton.isCompletelyDisplayed() }

            floatingChatButton.doesNotExist()

            appBar.collapse()
            listView.scrollTo(commButtonsItem)

            chatButton.doesNotExist()
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("P2PChatOfferCardTest/cardWithViews.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferNoChat() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("P2PChatOfferCardTest/cardWithViewsNoChat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyChat() {
        register(
            request {
                path("2.0/chat/room/offer/$OFFER_ID")
            },
            response {
                assetBody("P2PChatOfferCardTest/emptyOfferChat.json")
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

    private companion object {

        private const val OFFER_ID = "731271620849149185"
        private const val CHAT_ID = "f866e7379e649f07c760fca0d96af259"
    }
}
