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
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class CommonRoomTest {
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetChatRoomDispatcher("spam")
        )
        userSetup()
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
    fun openDialogsListing() {
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.messages)
        }
    }

    @Test
    fun shouldNotSeeRoomWithoutLastMessageSnippet() {
        checkMessages {
            isSnippetNotDisplayed(subject = "Audi A5 I Рестайлинг, 2012")
        }
    }

    @Test
    fun shouldSeeCommonSnippetControls() {
        performMessages { swipeDialogSnippet(1) }
            .checkResult {
                isDeleteButtonDisplayed(1)
                isBlockButtonDisplayed(1)
                isUnblockButtonNotExist(1)
                isMuteButtonDisplayed(1)
                isUnmuteButtonNotExist(1)
            }
    }

    @Test
    fun shouldSeeSnippetWithEmptyMessageFieldIfLastMessageIsSpam() {
        checkMessages {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 3,
                subject = "Уаз Патриот",
                subjectCost = "1 070 000 \u20BD",
                lastMessage = "",
            )
        }
    }
}
