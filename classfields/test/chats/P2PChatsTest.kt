package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ChatsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.robot.performOnChatsScreen
import com.yandex.mobile.realty.core.robot.performOnCommunicationScreen
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.socket.sendMessageToWebSocket
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 28/01/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class P2PChatsTest {

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
    fun shouldRemoveChat() {
        val chatId = "c2c952f84858925a65bf6158e6b85248e5b"
        configureWebServer {
            registerChats()
            registerMarkChatSuccess(chatId, "inactive")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet(chatId) }

                tapOn(lookup.matchesSnippetView(chatId))
            }
        }

        performOnChatMessagesScreen {
            tapOn(lookup.matchesToolbarMenuButton(), true)
            tapOn(lookup.matchesToolbarMenuRemoveButton())

            performOnConfirmationDialog {
                isTitleEquals("Удаление чата")
                isMessageEquals("Вы действительно хотите удалить этот\u00A0чат?")
                isNegativeButtonTextEquals("нет")
                isPositiveButtonTextEquals("да")
                confirm()
            }
        }

        performOnCommunicationScreen {
            waitUntil { isToolbarTitleShown() }
            performOnChatsScreen {
                waitUntil { doesNotContainsChatSnippet(chatId) }
            }
        }
    }

    @Test
    fun shouldMuteAndUnmuteChat() {
        val chatId = "c2c952f84858925a65bf6158e6b85248e5b"
        configureWebServer {
            registerChats()
            registerMarkChatSuccess(chatId, "mute")
            registerMarkChatSuccess(chatId, "unmute")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet(chatId) }

                performOnChatSnippet(chatId) {
                    isViewStateMatches("P2PChatsTest/shouldMuteAndUnmuteChat/unmuted")
                }

                tapOn(lookup.matchesSnippetView(chatId))
            }
        }

        performOnChatMessagesScreen {
            tapOn(lookup.matchesToolbarMenuButton(), true)
            tapOn(lookup.matchesToolbarMenuMuteButton())

            waitUntil { isToastShown("Уведомления отключены") }

            pressBack()
        }

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet(chatId) }

                performOnChatSnippet(chatId) {
                    isViewStateMatches("P2PChatsTest/shouldMuteAndUnmuteChat/muted")
                }

                tapOn(lookup.matchesSnippetView(chatId))
            }
        }

        performOnChatMessagesScreen {
            tapOn(lookup.matchesToolbarMenuButton(), true)
            tapOn(lookup.matchesToolbarMenuUnmuteButton())

            waitUntil { isToastShown("Уведомления включены") }

            pressBack()
        }

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet(chatId) }

                performOnChatSnippet(chatId) {
                    isViewStateMatches("P2PChatsTest/shouldMuteAndUnmuteChat/unmuted")
                }
            }
        }
    }

    @Test
    fun shouldReorderChats() {
        val chatId = "c2c0da9cd6e0c6afc0cb074a06b141f4010"
        val message = "message"
        configureWebServer {
            registerChats()
            registerEmptyListMessages(chatId)
            registerSendTextMessage(chatId, "1", message)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet(chatId) }

                isViewStateMatches("P2PChatsTest/shouldReorderChats/initial")

                tapOn(lookup.matchesSnippetView(chatId))
            }
        }

        performOnChatMessagesScreen {
            waitUntil { isInputFieldShown() }

            typeText(lookup.matchesInputField(), message)
            tapOn(lookup.matchesSendButton())

            waitUntil { containsTextMessage(message) }

            pressBack()
        }

        performOnCommunicationScreen {
            performOnChatsScreen {
                performOnChatSnippet(chatId) {
                    waitUntil { isMessageEquals(message) }
                }

                isViewStateMatches("P2PChatsTest/shouldReorderChats/reordered")
            }
        }
    }

    @Test
    fun shouldShowNewChatOnNewSocketMessage() {
        val chatId = "HasUnreadMessage"
        configureWebServer {
            registerChats()
            registerChat(chatId)
            registerWebSocketUrl()
            registerChatMessages(chatId)
            registerChatUnread(chatId)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnCommunicationScreen {
            performOnChatsScreen {
                waitUntil { containsChatSnippet("c2c952f84858925a65bf6158e6b85248e5b") }

                sendMessageToWebSocket(
                    """{
                            "operation": "chat_new_message",
                            "message": "{
                                \"message\":{
                                    \"roomId\":\"$chatId\",
                                    \"author\":\"4046750527\",
                                    \"payload\":{\"contentType\":\"TEXT_HTML\",\"value\":\"msg\"},
                                    \"id\":\"test_message_id\",
                                    \"properties\":{},
                                    \"created\":\"2020-01-22T08:30:39.504Z\"
                                },
                                \"previousMessageId\":null
                            }"
                        }"""
                )

                waitUntil { containsChatSnippet(chatId) }

                isViewStateMatches("P2PChatsTest/shouldShowNewChatOnNewSocketMessage/hasUnread")

                tapOn(lookup.matchesSnippetView(chatId))
            }
        }

        performOnChatMessagesScreen {
            waitUntil { containsTextMessage("msg") }

            pressBack()
        }

        performOnCommunicationScreen {
            performOnChatsScreen {
                performOnChatSnippet(chatId) {
                    isIndicatorHidden()
                }

                isViewStateMatches("P2PChatsTest/shouldShowNewChatOnNewSocketMessage/noUnread")
            }
        }
    }

    private fun DispatcherRegistry.registerChats() {
        register(
            request {
                path("2.0/chat/rooms/list/all")
                queryParam("includeTechSupport", "true")
            },
            response {
                assetBody("P2PChatsTest/chats.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMarkChatSuccess(id: String, action: String) {
        register(
            request {
                method("PUT")
                path("2.0/chat/room/$id/mark/$action")
            },
            success()
        )
    }

    /**
     * пустой список региструруем 2 раза, т.к. может быть ситуация с 2 запросами подряд, когда
     * вызывается обновление из - за screenHolder и из - за getMessages при старте экрана
     */
    private fun DispatcherRegistry.registerEmptyListMessages(chatId: String) {
        register(
            request {
                path("2.0/chat/messages/room/$chatId")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
        register(
            request {
                path("2.0/chat/messages/room/$chatId")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerChatMessages(chatId: String) {
        register(
            request {
                path("2.0/chat/messages/room/$chatId")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[
                                 {
                                    "roomId":"$chatId",
                                    "author":"4046750527",
                                    "payload":{"contentType":"TEXT_HTML","value":"msg"},
                                    "id":"test_message_id",
                                    "properties":{},
                                    "created":"2020-01-22T08:30:39.504Z"
                                  }
                           ]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerSendTextMessage(
        chatId: String,
        localId: String,
        text: String
    ) {
        register(
            request {
                method("POST")
                path("2.0/chat/messages")
            },
            response {
                setBody(
                    """{"response":{
                            "id": "a",
                            "roomId": "$chatId",
                            "author": "$TEST_UID",
                            "created": "2020-01-21T09:30:39.504Z",
                            "payload": {
                                  "contentType": "TEXT_HTML",
                                  "value": "$text"
                                },
                            "providedId": "$localId",
                            "properties": {}
                        }}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerChat(chatId: String) {
        register(
            request {
                path("2.0/chat/room/$chatId")
            },
            response {
                assetBody("P2PChatsTest/chat$chatId.json")
            }
        )
    }

    private fun DispatcherRegistry.registerChatUnread(chatId: String) {
        register(
            request {
                method("DELETE")
                path("2.0/chat/messages/room/$chatId/unread")
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerWebSocketUrl() {
        register(
            request {
                path("1.0/device/websocket")
            },
            response {
                setBody("""{"response": {"url": "wss://test.test/test"}}""")
            }
        )
    }

    companion object {
        private const val TEST_UID = 1
    }
}
