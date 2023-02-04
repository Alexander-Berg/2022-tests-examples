package ru.auto.ara.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.checkLogin
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.othertab.profile.checkProfile
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class ProfileDeeplinkAnonTest {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
        postLoginOrRegisterSuccess()
        postEmptyGarageListing()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetPreferencesRule(),
        activityTestRule
    )

    @Before
    fun setup() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/my/profile")
    }

    @Test
    fun shouldOpenProfileFromDeeplinkAuthState() {
        checkLogin { isPhoneAuth() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }

        checkProfile {
            isProfileHeaderDisplayed()
        }
    }

    companion object {
        private const val PHONE = "+7 (000) 000-00-00"
        private const val CODE = "0000"
    }
}
