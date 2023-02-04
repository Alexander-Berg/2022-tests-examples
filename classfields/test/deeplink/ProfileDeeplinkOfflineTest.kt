package ru.auto.ara.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.othertab.profile.checkProfile
import ru.auto.ara.core.rules.OfflineRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class ProfileDeeplinkOfflineTest {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        OfflineRule(),
        SetPreferencesRule(),
        activityTestRule
    )


    @Before
    fun setup() {
        webServerRule.routing { clear() }
        activityTestRule.launchDeepLinkActivity("autoru://app/my/profile/")
    }

    @Test
    fun shouldOpenProfileFromDeeplink() {
        checkProfile { isProfileErrorStateDisplayed() }
    }

}
