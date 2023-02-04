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
import ru.auto.ara.core.robot.chat.performMessages
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class MutedRoomTest {
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetChatRoomDispatcher("muted")
        )
        userSetup()
    }
    var activityTestRule = lazyActivityScenarioRule<MainActivity>()

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
    fun shouldSeeMutedSnippetControls() {
        performMessages { swipeDialogSnippet(1) }
            .checkResult {
                isDeleteButtonDisplayed(1)
                isBlockButtonDisplayed(1)
                isUnblockButtonNotExist(1)
                isMuteButtonNotExist(1)
                isUnmuteButtonDisplayed(1)
            }
    }
}
