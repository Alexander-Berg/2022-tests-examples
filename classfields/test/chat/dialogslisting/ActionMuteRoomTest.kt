package ru.auto.ara.test.chat.dialogslisting

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.PutChatMuteRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.PutChatUnmuteRoomDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.performMessages
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ActionMuteRoomTest {
    private val muteWatcher = RequestWatcher()
    private val unmuteWatcher = RequestWatcher()

    private val commonRoomDispatcher = GetChatRoomDispatcher("actions_room")
    private val mutedRoomDispatcher = GetChatRoomDispatcher("muted")

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            PutChatMuteRoomDispatcher("739100c955d4c2a05f9f5033b7a1473e", muteWatcher),
            PutChatUnmuteRoomDispatcher("739100c955d4c2a05f9f5033b7a1473e", unmuteWatcher)
        )
    }

    var activityTestRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupAuthRule()
    )

    @Test
    fun shouldMuteRoom() {
        webServerRule.routing { delegateDispatcher(commonRoomDispatcher) }
        activityTestRule.launchActivity()
        performMain { openLowTab(R.string.messages) }
        performMessages {
            swipeDialogSnippet(1)
            waitSomething(200, TimeUnit.MILLISECONDS)
            interactions.onMuteIcon(1).performClick()
        }.checkResult {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 1,
                subject = "Audi A6 allroad III (C7), 2014",
                subjectCost = "2 200 000 \u20BD",
                lastMessage = "Здравствуйте, какая причина продажи?",
            )
            isIconMutedDisplayed(1)
            isUnreadIndicatorOfMutedDialogDisplayed(1)
        }
        checkMuteRequestWasCalled()
        performMessages { swipeDialogSnippet(1) }
            .checkResult {
                isDeleteButtonDisplayed(1)
                isUnmuteButtonDisplayed(1)
                isMuteButtonNotExist(1)
                isBlockButtonDisplayed(1)
                isUnblockButtonNotExist(1)
            }
    }

    @Test
    fun shouldUnMuteRoom() {
        webServerRule.routing { delegateDispatcher(mutedRoomDispatcher) }
        activityTestRule.launchActivity()
        performMain { openLowTab(R.string.messages) }
        performMessages {
            swipeDialogSnippet(2)
            waitSomething(200, TimeUnit.MILLISECONDS)
            interactions.onUnmuteIcon(2).performClick()
        }.checkResult {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 2,
                subject = "Audi A6 allroad III (C7), 2014",
                subjectCost = "2 200 000 \u20BD",
                lastMessage = "Здравствуйте, какая причина продажи?",
            )
            isIconMutedNotDisplayed(2)
            isUnreadIndicatorOfDialogDisplayed(2)
        }
        checkUnmuteRequestWasCalled()
        performMessages { swipeDialogSnippet(2) }
            .checkResult {
                isDeleteButtonDisplayed(2)
                isMuteButtonDisplayed(2)
                isUnmuteButtonNotExist(2)
                isBlockButtonDisplayed(2)
                isUnblockButtonNotExist(2)
            }
    }

    private fun checkMuteRequestWasCalled() = assertThat(
        "Запрос PutMuteRoom не был выполнен",
        muteWatcher.isRequestCalled(), `is`(true)
    )

    private fun checkUnmuteRequestWasCalled() = assertThat(
        "Запрос PutUnmuteRoom не был выполнен",
        unmuteWatcher.isRequestCalled(), `is`(true)
    )
}
