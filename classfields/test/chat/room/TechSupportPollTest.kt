package ru.auto.ara.test.chat.room

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.PutPollVoteDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.poll.checkPoll
import ru.auto.ara.core.robot.poll.performPoll
import ru.auto.ara.core.routing.delegateDispatchers
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
class TechSupportPollTest {
    private val TECH_SUPPORT_CHAT_DEEPLINK = "autoru://app/chat/room/6198b8c87ae28da9a9389fbc606e8a7b"
    private val WAIT_DURATION = 400L
    private val RATING_PARAM = "rating"
    private val POLL_ID = "1b58af93bd19dd9bfe69da8ab9ad243d"
    private val voteWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetRoomMessagesDispatcher.getPollResponse(),
            GetRoomSpamMessagesDispatcher.getEmptyResponse(),
            GetChatRoomDispatcher("autoru_and_vibiralshik"),
            PutPollVoteDispatcher(POLL_ID, voteWatcher)
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
        activityTestRule.launchDeepLinkActivity(TECH_SUPPORT_CHAT_DEEPLINK)
    }

    @Test
    fun shouldSeePollWithoutVote() {
        checkPoll {
            isPollDisplayed(R.string.title_ask_for_vote)
            isPollWithoutVote()
        }
    }

    @Test
    fun shouldSetBadVoteForPoll() {
        waitVoteAnimation()
        closeSoftKeyboard()
        waitVoteAnimation()
        performPoll { clickBadVote() }
        waitVoteAnimation()
        voteWatcher.checkQueryParameter(RATING_PARAM, "1")
        checkPoll { isBadVoteSelected() }
    }

    @Test
    fun shouldSetMediumVoteForPoll() {
        waitVoteAnimation()
        closeSoftKeyboard()
        waitVoteAnimation()
        performPoll { clickMediumVote() }
        waitVoteAnimation()
        voteWatcher.checkQueryParameter(RATING_PARAM, "2")
        checkPoll { isMediumVoteSelected() }
    }

    @Test
    fun shouldSetGoodVoteForPoll() {
        waitVoteAnimation()
        closeSoftKeyboard()
        waitVoteAnimation()
        performPoll { clickGoodVote() }
        waitVoteAnimation()
        voteWatcher.checkQueryParameter(RATING_PARAM, "3")
        checkPoll { isGoodVoteSelected() }
    }

    private fun waitVoteAnimation() = waitSomething(WAIT_DURATION, TimeUnit.MILLISECONDS)
}
