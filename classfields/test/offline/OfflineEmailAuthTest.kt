package ru.auto.ara.test.offline

import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.interaction.auth.LoginInteractions.onLoginBtn
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.rules.OfflineRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.utils.activityScenarioWithFragmentRule
import ru.auto.ara.core.utils.getRandomEmail
import ru.auto.ara.ui.activity.PhoneAuthActivity
import ru.auto.ara.ui.fragment.auth.EmailAuthFragment

@RunWith(AndroidJUnit4::class)
class OfflineEmailAuthTest {

    private val activityTestRule = activityScenarioWithFragmentRule<PhoneAuthActivity, EmailAuthFragment>()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        OfflineRule(),
        activityTestRule,
    )

    @Test
    fun shouldSeeConnectionError() {
        performLogin {
            input(getRandomEmail())
            clickOnLoginBtn()
        }.checkResult {
            isConnectionError()
            onLoginBtn().check(matches(isDisplayed()))
        }
    }
}
