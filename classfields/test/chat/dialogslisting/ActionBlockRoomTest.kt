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
import ru.auto.ara.core.dispatchers.chat.PutChatBlockRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.PutChatUnblockRoomDispatcher
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
class ActionBlockRoomTest {
    private val blockWatcher = RequestWatcher()
    private val unblockWatcher = RequestWatcher()

    private val commonRoomDispatcher = GetChatRoomDispatcher("actions_room")
    private val blockedRoomDispatcher = GetChatRoomDispatcher("blocked")

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            PutChatBlockRoomDispatcher("739100c955d4c2a05f9f5033b7a1473e", blockWatcher),
            PutChatUnblockRoomDispatcher("784b9f562bad9229a724b3e6693a70dd", unblockWatcher)
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
    fun shouldBlockRoom() {
        webServerRule.routing { delegateDispatcher(commonRoomDispatcher) }
        activityTestRule.launchActivity()
        performMain { openLowTab(R.string.messages) }
        performMessages {
            swipeDialogSnippet(1)
            waitSomething(200, TimeUnit.MILLISECONDS)
            interactions.onBlockIcon(1).performClick()
        }.checkResult {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 1,
                subject = "Audi A6 allroad III (C7), 2014",
                subjectCost = "2 200 000 \u20BD",
                lastMessage = "Заблокирован",
            )
            isBlockedIconDisplayed(1)
        }
        checkBlockRequestWasCalled()
        performMessages { swipeDialogSnippet(1) }
            .checkResult {
                isDeleteButtonDisplayed(1)
                isUnblockButtonDisplayed(1)
                isBlockButtonNotExist(1)
                isMuteButtonNotExist(1)
                isUnmuteButtonNotExist(1)
            }
    }

    @Test
    fun shouldUnblockRoom() {
        webServerRule.routing { delegateDispatcher(blockedRoomDispatcher) }
        activityTestRule.launchActivity()
        performMain { openLowTab(R.string.messages) }
        performMessages {
            swipeDialogSnippet(1)
            waitSomething(200, TimeUnit.MILLISECONDS)
            interactions.onUnblockIcon(1).performClick()
        }.checkResult {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 1,
                subject ="BMW X6 I (E71), 2010",
                subjectCost = "1 300 000 \u20BD",
                lastMessage = "Обмен интересует?",
            )
            isBlockedIconNotDisplayed(1)
        }
        checkUnblockRequestWasCalled()
        performMessages { swipeDialogSnippet(1) }
            .checkResult {
                isDeleteButtonDisplayed(1)
                isBlockButtonDisplayed(1)
                isUnblockButtonNotExist(1)
                isMuteButtonDisplayed(1)
                isUnmuteButtonNotExist(1)
            }
    }

    private fun checkBlockRequestWasCalled() = assertThat(
        "Запрос PutBlockRoom не был выполнен",
        blockWatcher.isRequestCalled(), `is`(true)
    )

    private fun checkUnblockRequestWasCalled() = assertThat(
        "Запрос PutUnblockRoom не был выполнен",
        unblockWatcher.isRequestCalled(), `is`(true)
    )
}
