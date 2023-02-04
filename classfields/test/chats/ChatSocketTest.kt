package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ChatMessagesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createImageAndGetUriString
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.socket.sendMessageToWebSocket
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author matek3022 on 2020-02-13.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ChatSocketTest {

    private val activityTestRule = ChatMessagesActivityTestRule(
        chatId = CHAT_ID,
        launchActivity = false
    )

    private val authorizationRule = AuthorizationRule()

    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowAllTypeMessagesFromSocket() {
        configureWebServer {
            registerTechSupportChat()
            registerEmptyListMessages()
            registerWebSocketUrl()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        performOnChatMessagesScreen {
            waitUntil { isFullscreenEmptyViewShown() }

            sendMessageToWebSocket(getTextMessage("test message", "1", "0"))

            waitUntil { containsTextMessage("test message") }

            sendMessageToWebSocket(getTextMessage("test message 2", "2", "1"))

            waitUntil { containsTextMessage("test message 2") }
            containsTextMessage("test message")

            sendMessageToWebSocket(getPollMessage("3", "2"))

            waitUntil { containsPollMessage() }
            isPollShown()
            containsTextMessage("test message 2")

            sendMessageToWebSocket(getPresetMessage("4", "3"))

            waitUntil { containsTextMessage("Preset message") }
            isPresetWithTextShown("All good")
            isPresetWithTextShown("Too long")
            containsPollMessage()
            isPollShown()

            sendMessageToWebSocket(getPhotoMessage("5", "4"))

            waitUntil { containsImageMessage() }
            isImageMessageImageShown()
            containsTextMessage("Preset message")
            isPresetWithTextShown("All good")
            isPresetWithTextShown("Too long")
        }
    }

    private fun DispatcherRegistry.registerTechSupportChat() {
        register(
            request {
                path("2.0/chat/room/$CHAT_ID")
            },
            response {
                setBody(
                    """
                        {
                          "response": {
                            "id": "$CHAT_ID",
                            "created": "2019-12-27T11:54:26.912Z",
                            "updated": "2020-12-16T18:24:08.842Z",
                            "userIds": [
                              "$TEST_UID",
                              "techSupport"
                            ],
                            "properties": {
                              "type": "techsupport"
                            },
                            "creator": "$TEST_UID",
                            "users": [
                              {
                                "id": "$TEST_UID",
                                "activeRoom": true,
                                "averageReplyDelayMinutes": 0,
                                "roomLastRead": "2020-12-29T08:07:30.081Z",
                                "profile": {
                                  "alias": "erik.burygin",
                                  "clientId": "$TEST_UID"
                                }
                              },
                              {
                                "id": "techSupport",
                                "activeRoom": true,
                                "averageReplyDelayMinutes": 0,
                                "roomLastRead": "2020-12-10T15:11:41.764Z",
                                "profile": {
                                  "alias": "Я.Недвижимость"
                                }
                              }
                            ],
                            "me": "$TEST_UID",
                            "subject": {
                              "title": "Чат с техподдержкой"
                            }
                          }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private fun getPresetMessage(messageId: String, previousMessageId: String): String {
        return """{
                "operation": "chat_new_message",
                "message": "{
                                    \"message\":{
                                        \"id\": \"$messageId\",
                                        \"roomId\": \"$CHAT_ID\",
                                        \"author\": \"techSupport\",
                                        \"created\": \"${getValidStringDate()}\",
                                        \"payload\": {
                                            \"contentType\": \"TEXT_PLAIN\",
                                            \"value\": \"Preset message\"
                                        },
                                        \"isSilent\": true,
                                        \"properties\": {
                                            \"techSupportFeedback\": {
                                                \"hash\": \"1\",
                                                \"presets\": [
                                                    {
                                                        \"id\": \"all_good\",
                                                        \"value\": \"All good\"
                                                    },
                                                    {
                                                        \"id\": \"too_long\",
                                                        \"value\": \"Too long\"
                                                    }
                                                ],
                                                \"ttl\": \"604800\"
                                            },
                                            \"type\": \"TECH_SUPPORT_FEEDBACK_REQUEST\"
                                        }
                                    },
                                    \"previousMessageId\":\"$previousMessageId\"
                                }"
            }"""
    }

    private fun getPollMessage(messageId: String, previousMessageId: String): String {
        return """{
                "operation": "chat_new_message",
                "message": "{
                                \"message\":{
                                    \"id\": \"$messageId\",
                                    \"roomId\": \"$CHAT_ID\",
                                    \"author\": \"techSupport\",
                                    \"created\": \"${getValidStringDate()}\",
                                    \"payload\": {
                                        \"contentType\": \"TEXT_PLAIN\",
                                        \"value\": \"This is poll\"
                                    },
                                    \"properties\": {
                                        \"techSupportPoll\": {
                                            \"hash\": \"1\",
                                            \"ttl\": \"604800\"
                                        },
                                        \"type\": \"TECH_SUPPORT_POLL\"
                                    }
                                },
                                \"previousMessageId\":\"$previousMessageId\"
                            }"
            }"""
    }

    private fun getTextMessage(text: String, messageId: String, previousMessageId: String): String {
        return """{
                "operation": "chat_new_message",
                "message": "{
                    \"message\":{
                        \"roomId\":\"$CHAT_ID\",
                        \"author\":\"techSupport\",
                        \"payload\":{\"contentType\":\"TEXT_HTML\",\"value\":\"$text\"},
                        \"id\":\"$messageId\",
                        \"properties\":{},
                        \"created\":\"${getValidStringDate()}\"
                    },
                    \"previousMessageId\":\"$previousMessageId\"
                }"
            }"""
    }

    private fun getPhotoMessage(messageId: String, previousMessageId: String): String {
        return """{
                "operation": "chat_new_message",
                "message": "{
                    \"message\":{
                        \"roomId\":\"$CHAT_ID\",
                        \"author\":\"techSupport\",
                        \"payload\":{\"contentType\":\"TEXT_HTML\"},
                        \"id\":\"$messageId\",
                        \"properties\":{},
                        \"attachments\": [
                            {
                                \"image\": {
                                    \"sizes\": {
                                        \"1200x1200\": \"${createImageAndGetUriString()}\"
                                    }
                                }
                            }
                        ],
                        \"created\":\"${getValidStringDate()}\"
                    },
                    \"previousMessageId\":\"$previousMessageId\"
                }"
            }"""
    }

    /**
     * пустой список региструруем 2 раза, т.к. может быть ситуация с 2 запросами подряд, когда
     * вызывается обновление из - за screenHolder и из - за getMessages при старте экрана
     */
    private fun DispatcherRegistry.registerEmptyListMessages() {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
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

    private fun getValidStringDate(): String {
        return formatter.format(Date(System.currentTimeMillis() - 100 * 1000L))
    }

    companion object {
        private const val CHAT_ID = "test_chat_id"
        private const val TEST_UID = 1
    }
}
