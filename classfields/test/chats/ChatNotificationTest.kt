package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FavoriteListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.PushBroadcastCommand
import com.yandex.mobile.realty.core.pressBack
import com.yandex.mobile.realty.core.robot.performOnBottomNavigationMenu
import com.yandex.mobile.realty.core.robot.performOnNotificationShade
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
 * @author matek3022 on 2020-03-04.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ChatNotificationTest {

    private val activityTestRule = FavoriteListActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
    )

    @Test
    fun shouldShowNotification() {
        authorizationRule.setUserAuthorized()
        val title = "title"
        val body = "body"
        PushBroadcastCommand.sendPush(
            action = "chat_new_message",
            params = """
                    {
                        "title": "$title",
                        "body": "$body",
                        "push_id": "CHAT_NEW_MESSAGE",
                        "chat_id": "1",
                        "message_id": "asd",
                        "recipient_id": "1"
                    }
            """.trimIndent(),
            name = "Новое сообщение в чате"
        )
        performOnNotificationShade {
            hasNotification(title, body)
        }
        pressBack()
    }

    @Test
    fun shouldNotShowNotification() {
        PushBroadcastCommand.sendPush(
            action = "chat_new_message",
            params = """
                    {
                        "title": "title",
                        "body": "body",
                        "push_id": "CHAT_NEW_MESSAGE",
                        "chat_id": "1",
                        "message_id": "asd",
                        "recipient_id": "1"
                    }
            """.trimIndent(),
            name = "Новое сообщение в чате"
        )
        performOnNotificationShade {
            hasNoNotifications()
        }
        pressBack()
    }

    @Test
    fun shouldShowNewMessagesIndicator() {
        val chatId = "HasUnreadMessage"
        configureWebServer {
            registerChat(chatId)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnBottomNavigationMenu {
            waitUntil { isBottomNavigationViewShown() }
        }

        val title = "title"
        val body = "body"
        PushBroadcastCommand.sendPush(
            action = "chat_new_message",
            params = """
                    {
                        "title": "$title",
                        "body": "$body",
                        "push_id": "CHAT_NEW_MESSAGE",
                        "chat_id": "$chatId",
                        "message_id": "asd",
                        "recipient_id": "1"
                    }
            """.trimIndent(),
            name = "Новое сообщение в чате"
        )

        performOnBottomNavigationMenu {
            waitUntil { isCommunicationIconIndicatorVisible() }

            isViewStateMatches("ChatNotificationTest/shouldShowNewMessagesIndicator/viewState")
        }
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
}
