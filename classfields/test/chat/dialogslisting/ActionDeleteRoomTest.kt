package ru.auto.ara.test.chat.dialogslisting

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.chat.DeleteChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.performMessages
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ActionDeleteRoomTest {
    private val watcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            DeleteChatRoomDispatcher("739100c955d4c2a05f9f5033b7a1473e", watcher),
            GetChatRoomDispatcher("actions_room")
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


    @Before
    fun setUp() {
        activityTestRule.launchActivity()
        performMain { openLowTab(R.string.messages) }
    }

    @Test
    fun shouldDeleteRoom() {
        performMessages {
            swipeDialogSnippet(1)
            waitSomething(200, TimeUnit.MILLISECONDS)
            interactions.onDeleteIcon(1).performClick()
        }.checkResult {
            isAlertDialogDisplayed()
        }
        checkDeleteRequestWasNotCalled()
        performMessages { interactions.onDeleteButton().performClick() }
            .checkResult { isSnippetNotDisplayed(subject = "Audi A6 allroad III (C7), 2014") }
        checkDeleteRequestWasCalled()
    }

    @Test
    fun shouldNotDeleteRoom() {
        performMessages {
            swipeDialogSnippet(1)
            waitSomething(200, TimeUnit.MILLISECONDS)
            interactions.onDeleteIcon(1).performClick()
        }.checkResult {
            isAlertDialogDisplayed()
        }
        performMessages { interactions.onDismissButton().performClick() }
            .checkResult {
                isBaseSnippetInfoDisplayed(
                    snippetIndex = 1,
                    subject = "Audi A6 allroad III (C7), 2014",
                    subjectCost = "2 200 000 \u20BD",
                    lastMessage = "Здравствуйте, какая причина продажи?",
                )
            }
        checkDeleteRequestWasNotCalled()
    }

    private fun checkDeleteRequestWasCalled() = assertThat(
        "Запрос DeleteRoom не был выполнен",
        watcher.isRequestCalled(), `is`(true)
    )

    private fun checkDeleteRequestWasNotCalled() = assertThat(
        "Запрос DeleteRoom был выполнен",
        watcher.isRequestCalled(), `is`(false)
    )
}
