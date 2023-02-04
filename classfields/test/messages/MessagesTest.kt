package ru.auto.ara.test.messages

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.chat.performMessages
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(AndroidJUnit4::class)
class MessagesTest {

    @JvmField
    @Rule
    var activityTestRule = baseRuleChain(
        WebServerRule(),
        activityScenarioRule<MainActivity>()
    )

    @Before
    fun openMessages() {
        performMain {
            openLowTab(R.string.messages)
        }
    }

    @Test
    fun shouldOpenLogin() {
        performMessages {
            interactions.onLogin().checkIsCompletelyDisplayed().performClick()
        }
        performLogin {}.checkResult { isPhoneAuth() }
    }
}
