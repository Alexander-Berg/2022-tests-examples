package ru.auto.ara.test.chat.room

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.CHAT_ROOM_ANSWER_TIME_DATA
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class AnswerTimeWithoutCallButtonTest(private val testParams: TestParameter) {
    private val DEFAULT_CHAT_DEEPLINK = "autoru://app/chat/room/f5d5b794395b82aff25e325d21c987c3"
    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetRoomMessagesDispatcher.getEmptyResponse(),
            GetRoomSpamMessagesDispatcher.getEmptyResponse(),
            testParams.chatListingDispatcher
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

    @Test
    fun shouldSeeCorrectStateForDescription() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        checkChatRoom {
            interactions.onReplyTimeText().waitUntilIsCompletelyDisplayed().checkWithClearText(testParams.replyTimeText)
            isReplyTimeImageDisplayed()
            interactions.onReplyTimeCallButton().checkIsGone()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = CHAT_ROOM_ANSWER_TIME.map { arrayOf(it) }

        private val CHAT_ROOM_ANSWER_TIME = CHAT_ROOM_ANSWER_TIME_DATA.map { chatRoomAnswerTimeData ->
            TestParameter(
                chatListingDispatcher = chatRoomAnswerTimeData.dispatcher,
                replyTimeText = chatRoomAnswerTimeData.replyTimeText,
                description = chatRoomAnswerTimeData.description
            )
        }

        data class TestParameter(
            val chatListingDispatcher: GetChatRoomDispatcher,
            val replyTimeText: Int,
            val description: String
        ) {
            override fun toString() = description
        }
    }
}
