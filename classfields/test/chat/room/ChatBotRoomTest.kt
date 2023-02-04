package ru.auto.ara.test.chat.room

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.RoomSpamMessage
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.getRoomSpamMessages
import ru.auto.ara.core.dispatchers.chat.postChatMessage
import ru.auto.ara.core.dispatchers.chat.putChatOpenRoom
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.matchers.ViewMatchers.withBoldText
import ru.auto.ara.core.matchers.ViewMatchers.withItalicText
import ru.auto.ara.core.matchers.ViewMatchers.withListText
import ru.auto.ara.core.robot.chat.ChatRoomRobotChecker.Companion.closeKeyboard
import ru.auto.ara.core.robot.chat.ChatRoomRobotChecker.Companion.longWaitAnimation
import ru.auto.ara.core.robot.chat.FINISH_CHECKUP
import ru.auto.ara.core.robot.chat.MUTE
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.clickToolbarMenu
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.feature.chats.model.MessageStatus

private const val DEEPLINK = "autoru://app/chat/room/ba170cd384b34bdd8b6249bb700ed744"
private const val PRESET_MESSAGE = "Еду смотреть"
private const val LONG_PAUSE = 3000L

@RunWith(AndroidJUnit4::class)
class ChatBotRoomTest {
    private val gson = Gson()
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
        getRoomSpamMessages(RoomSpamMessage.EMPTY)
        getChatRoom("vibiralshik_2")
        stub { getRoomMessagesFirstPage(RoomMessages.CHAT_BOT_HELPER_MESSAGE) }
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupAuthRule()
    )

    @Test
    fun shouldDisplayToolbarMenuWithButtonsFinishCheckupAndMuteNotifications() {
        webServerRule.routing {
            putChatOpenRoom("ba170cd384b34bdd8b6249bb700ed744").watch { checkRequestWasCalled() }
        }
        openBotChat()
        checkChatRoom {
            clickToolbarMenu()
            isAvailiableActionsDisplayed(listOf(MUTE, FINISH_CHECKUP))
        }
    }

    @Test
    fun showMessageFinishCheckupWhenClickOnMenuButton() {
        openBotChat()
        webServerRule.routing {
            postChatMessage("finish_checkup", gson).watch {
                checkRequestBodyParameters("payload.value" to FINISH_CHECKUP)
            }
        }
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(FINISH_CHECKUP).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            longWaitAnimation()
            isChatMessageDisplayed(FINISH_CHECKUP)
        }
    }

    @Test
    fun shouldShowMessageWhenClickOnPresetAndHidePreset() {
        openBotChat()
        webServerRule.routing {
            postChatMessage("go_to_checkup", gson).watch {
                checkRequestBodyParameters("payload.value" to PRESET_MESSAGE)
            }
        }
        performChatRoom {
            interactions.onChatOptionButton(PRESET_MESSAGE).waitUntilIsCompletelyDisplayed()
                .performClick()
        }.checkResult {
            longWaitAnimation()
            interactions.onChatOptionButton(PRESET_MESSAGE).checkNotExists()
            isChatMessageDisplayed(PRESET_MESSAGE)
        }
    }

    @Test
    fun shouldDisplayBoldText() {
        openBotChat()
        checkChatRoom {
            interactions.onMessageFormattingText()
                .waitUntilIsCompletelyDisplayed()
                .check(withBoldText("Привет!"))
        }
    }

    @Test
    fun shouldDisplayItalicText() {
        openBotChat()
        checkChatRoom {
            interactions.onMessageFormattingText()
                .waitUntilIsCompletelyDisplayed()
                .check(withItalicText("Помощник"))
        }
    }

    @Test
    fun shouldOpenLinkFromMessage() {
        openBotChat()
        webServerRule.routing { getOffer("1082957054-8d55bf9a") }

        performChatRoom {
            interactions.onMessageFormattingText()
                .waitUntilIsDisplayed()
                .performClickClickableSpan("https://auto.ru/cars/used/sale/mazda/cx_5/1082957054-8d55bf9a")
        }
        checkOfferCard { isOfferCardTitle("BMW 4 серия 420d xDrive F32/F33/F36, 2016") }
    }

    @Test
    fun shouldOpenHrefLinkFromMessage() {
        openBotChat()
        webServerRule.routing { getOffer("1082957054-8d55bf9a") }

        performChatRoom {
            interactions.onMessageFormattingText()
                .waitUntilIsCompletelyDisplayed()
                .performClickClickableSpan("я - ссылка")
        }
        checkOfferCard { isOfferCardTitle("BMW 4 серия 420d xDrive F32/F33/F36, 2016") }
    }

    @Test
    fun shouldDisplayListText() {
        openBotChat()
        checkChatRoom {
            interactions.onMessageFormattingText()
                .waitUntilIsCompletelyDisplayed()
                .check(withListText(listOf("Один", "Два", "Три")))
        }
    }

    @Test
    fun shouldHideReadStatus() {
        webServerRule.routing { getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_TWO_OUTCOMING_CHATBOT_MESSAGES) }
        openBotChat()

        checkChatRoom {
            // They both sould be same
            isMessageStatus("Это сообщение собеседник прочитал", MessageStatus.SENT)
            isMessageStatus("Это сообщение собеседник ещё не успел прочесть", MessageStatus.SENT)
        }
    }

    @Test
    fun shouldHideReadStatusOnImageMessages() {
        webServerRule.routing { getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_TWO_OUTCOMING_CHATBOT_IMAGE_MESSAGES) }
        openBotChat()

        checkChatRoom {
            // They both sould be same
           isMessageStatus(1, MessageStatus.SENT)
           isMessageStatus(2, MessageStatus.SENT)
        }
    }

    private fun openBotChat() {
        activityTestRule.launchDeepLinkActivity(DEEPLINK)
        closeKeyboard(LONG_PAUSE)
    }
}
