package ru.auto.ara.test.chat.room

import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.RoomSpamMessage
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.getRoomSpamMessages
import ru.auto.ara.core.dispatchers.chat.postChatMessage
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.closeSoftKeyboard
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TechSupportBotTest {
    private val TECH_SUPPORT_CHAT_DEEPLINK = "autoru://app/chat/room/6198b8c87ae28da9a9389fbc606e8a7b"
    private val LONG_WAIT_DURATION = 3000L

    private val webServerRule = WebServerRule {
        userSetup()
        getRoomMessagesFirstPage(RoomMessages.SUPPORT_CHAT_BOT)
        getRoomSpamMessages(RoomSpamMessage.EMPTY)
        getChatRoom("autoru_and_vibiralshik")
    }

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupAuthRule()
    )

    @Test
    fun shouldSeePresetMessages() {
        activityTestRule.launchDeepLinkActivity(TECH_SUPPORT_CHAT_DEEPLINK)

        longWaitAnimation()
        closeSoftKeyboard()
        checkChatRoom {
            interactions.onChatOptionButton("Позови человека").waitUntilIsCompletelyDisplayed()
            interactions.onChatOptionButton("Вопрос про телефон").waitUntilIsCompletelyDisplayed()
        }
    }

    @Test
    fun shouldSendFeedback() {
        webServerRule.routing {
            postChatMessage("call_human").watch {
                checkRequestBodyParameters("payload.value" to "Позови человека")
            }
        }

        activityTestRule.launchDeepLinkActivity(TECH_SUPPORT_CHAT_DEEPLINK)

        performChatRoom {
            longWaitAnimation()
            closeSoftKeyboard()
            longWaitAnimation()
            interactions.onTextInput().waitUntil(isCompletelyDisplayed(), withHint(R.string.write_text_hint))
            interactions.onChatOptionButton("Позови человека").waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            longWaitAnimation()
            isChatMessageDisplayed("Позови человека")
            interactions.onChatOptionList().checkNotExists()
        }
    }

    @Test
    fun shouldHideFeedbackPresetsAfterSendCommonMessage() {
        webServerRule.routing {
            postChatMessage("common_message").watch {
                checkRequestBodyParameter("payload.value", "message without preset")
            }
        }

        activityTestRule.launchDeepLinkActivity(TECH_SUPPORT_CHAT_DEEPLINK)

        performChatRoom {
            longWaitAnimation()
            closeSoftKeyboard()
            sendMessage("message without preset")
        }.checkResult {
            longWaitAnimation()
            isChatMessageDisplayed("message without preset")
            interactions.onPresetMessagesList().checkNotVisible()
        }
    }

    private fun longWaitAnimation() = waitSomething(LONG_WAIT_DURATION, TimeUnit.MILLISECONDS)
}
