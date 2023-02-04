package ru.auto.ara.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.othertab.profile.checkProfile
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class ProfileDeeplinkAuthTest {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule {
            userSetup()
            postEmptyGarageListing()
        },
        SetupAuthRule(),
        SetPreferencesRule(),
        activityTestRule
    )


    @Before
    fun setup() {
        activityTestRule.launchDeepLinkActivity("autoru://app/my/profile/")
    }

    @Test
    fun shouldOpenProfileFromDeeplinkAuthState() {
        checkProfile {
            isProfileHeaderDisplayed()
        }
    }
}
