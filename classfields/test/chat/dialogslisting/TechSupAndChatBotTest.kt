package ru.auto.ara.test.chat.dialogslisting

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.checkMessages
import ru.auto.ara.core.robot.chat.performMessages
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class TechSupAndChatBotTest {
    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetChatRoomDispatcher("autoru_and_vibiralshik")
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupTimeRule("31.12.2019"),
        SetupAuthRule()
    )


    @Before
    fun openDialogsListing() {
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.messages)
        }
    }

    @Test
    fun shouldSeeTechSupportSnippet() {
        checkMessages {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 1,
                title = "Авто.ру",
                subject = "Чат с поддержкой",
                lastMessage = "Надеемся, мы вам помогли! Будет здорово, если вы поставите нам оценку",
            )
            isLastMessageDateDisplayed(1, "15 мая")
            isSnippetImageDisplayed(1)
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun shouldSeeChatBotSnippet() {
        checkMessages {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 2,
                title = "Помощник осмотра",
                subject = "Покажу всё, что скрыто",
                lastMessage = "Ой, похоже, вы отправили мне что-то неожиданное. Пожалуйста, перечитайте моё предыдущее сообщение.",
            )
            isLastMessageDateDisplayed(2, "23 апреля")
            isSnippetImageDisplayed(2)
        }
    }

    @Test
    fun shouldSeeTechSupportSnippetControls() {
        performMessages { swipeDialogSnippet(1) }
            .checkResult {
                isMuteButtonDisplayed(1)
                isUnmuteButtonNotExist(1)
                isBlockButtonNotExist(1)
                isUnblockButtonNotExist(1)
                isDeleteButtonNotExist(1)
            }
    }

    @Test
    fun shouldSeeChatBotSnippetControls() {
        performMessages { swipeDialogSnippet(2) }
            .checkResult {
                isMuteButtonDisplayed(2)
                isUnmuteButtonNotExist(2)
                isBlockButtonNotExist(2)
                isUnblockButtonNotExist(2)
                isDeleteButtonNotExist(2)
            }
    }
}
