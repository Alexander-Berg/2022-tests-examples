package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ChatsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnChatsScreen
import com.yandex.mobile.realty.core.robot.performOnCommunicationScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author matek3022 on 2020-02-03.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ChatsTest {

    private val activityTestRule = ChatsActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowUnauthorizedChats() {
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            tapOn(lookup.matchesMessagesTabTitle())

            performOnChatsScreen {
                waitUntil { isUnauthorizedScreenShown() }
            }
        }
    }

    @Test
    fun shouldShowErrorScreenChats() {
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            tapOn(lookup.matchesMessagesTabTitle())

            performOnChatsScreen {
                waitUntil { isErrorScreenShown() }
            }
        }
    }

    @Test
    fun shouldShowTechSupportChatWithLastMessage() {
        configureWebServer {
            registerTechSupportChatWithLastMessage()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet("a94698b61afd3f72ae88bc52b5d1d322") }

                performOnChatSnippet("a94698b61afd3f72ae88bc52b5d1d322") {
                    isViewStateMatches("ChatsTest/shouldShowTechSupportChatWithLastMessage")
                }
            }
        }
    }

    @Test
    fun shouldShowTechSupportChatWithoutLastMessage() {
        configureWebServer {
            registerTechSupportChatWithoutLastMessage()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet("a94698b61afd3f72ae88bc52b5d1d322") }

                performOnChatSnippet("a94698b61afd3f72ae88bc52b5d1d322") {
                    isViewStateMatches("ChatsTest/shouldShowTechSupportChatWithoutLastMessage")
                }
            }
        }
    }

    private fun DispatcherRegistry.registerTechSupportChatWithLastMessage() {
        register(
            request {
                path("2.0/chat/rooms/list/all")
                queryParam("includeTechSupport", "true")
            },
            response {
                assetBody("techSupportChatWithLastMessage.json")
            }
        )
    }

    private fun DispatcherRegistry.registerTechSupportChatWithoutLastMessage() {
        register(
            request {
                path("2.0/chat/rooms/list/all")
                queryParam("includeTechSupport", "true")
            },
            response {
                assetBody("techSupportChatWithoutLastMessage.json")
            }
        )
    }
}
