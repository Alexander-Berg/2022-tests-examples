package ru.auto.ara.test.chat.room

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity


@RunWith(AndroidJUnit4::class)
class AnswerTimeWithCallButtonTest {
    private val DEFAULT_CHAT_DEEPLINK = "autoru://app/chat/room/f5d5b794395b82aff25e325d21c987c3"
    private val PHONE_NUMBER = "+7 985 440-66-27"
    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetRoomSpamMessagesDispatcher.getEmptyResponse(),
            GetRoomMessagesDispatcher.getEmptyResponse(),
            GetPhonesDispatcher.onePhone(offerId = "1086893549-303bb635"),
            GetChatRoomDispatcher(expectedResponse = "rarely_reply_time")
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupAuthRule()
    )


    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
    }

    @Test
    fun shouldSeeCorrectStateWithRarelyReplyTime() {
        checkChatRoom {
            interactions.onReplyTimeText().waitUntilIsCompletelyDisplayed().checkWithClearText(R.string.reply_rarely)
            isReplyTimeImageDisplayed()
        }
        performChatRoom {
            Intents.init()
            interactions.onReplyTimeCallButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon { isActionDialIntentCalled(PHONE_NUMBER) }
    }
}
