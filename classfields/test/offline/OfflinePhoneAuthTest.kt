package ru.auto.ara.test.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.interaction.auth.LoginInteractions.onInput
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.OfflineRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.utils.activityScenarioWithFragmentRule
import ru.auto.ara.core.utils.getRandomPhone
import ru.auto.ara.ui.activity.PhoneAuthActivity
import ru.auto.ara.ui.fragment.auth.PhoneAuthFragment

@RunWith(AndroidJUnit4::class)
class OfflinePhoneAuthTest {

    val activityTestRule = activityScenarioWithFragmentRule<PhoneAuthActivity, PhoneAuthFragment>()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        OfflineRule(),
        GrantPermissionsRule(),
        activityTestRule,
    )

    @Test
    fun shouldSeeConnectionError() {
        performLogin {
            input(getRandomPhone())
            onInput().performPressEnter()
        }.checkResult {
            isLoginShown()
        }
    }
}
