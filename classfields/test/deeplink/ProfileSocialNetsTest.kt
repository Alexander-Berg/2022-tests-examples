package ru.auto.ara.test.deeplink

import androidx.annotation.DrawableRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.user.UserAssets.WITH_GOSUSLUGI
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.othertab.profile.checkBindSocialNets
import ru.auto.ara.core.robot.othertab.profile.checkProfile
import ru.auto.ara.core.robot.othertab.profile.performBindSocialNets
import ru.auto.ara.core.robot.othertab.profile.performProfile
import ru.auto.ara.core.routing.assetResponse
import ru.auto.ara.core.routing.delete
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.SocialNet

@RunWith(AndroidJUnit4::class)
class ProfileSocialNetsTest {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
        delete(
            description = "login-or-add-social or social-profiles",
            requestDefinition = {
                path(
                    anyOf(
                        containsString("auth/login-or-add-social"),
                        containsString("user/social-profiles/")
                    )
                )
            },
            response = assetResponse("status_success.json")
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetPreferencesRule(),
        activityTestRule,
        SetupAuthRule(),
    )

    @Before
    fun setup() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/my/profile")
    }

    @Test
    fun shouldHaveSocialNetCount() {
        performProfile { scrollToBottom() }
        checkProfile {
            checkHasBindedSocialNets(
                DEFAULT_SOCIAL_PROFILES[0].icon,
                DEFAULT_SOCIAL_PROFILES[1].icon,
                DEFAULT_SOCIAL_PROFILES[2].icon,
            )
        }
    }

    @Test
    fun shouldHaveSocialNetsBindedInBottomsheet() {
        performProfile {
            scrollToBottom()
            openSocialNetsDialog()
        }
        checkBindSocialNets {
            DEFAULT_SOCIAL_PROFILES.forEach { (socialNet, icon) -> checkSocialNetIsBinded(socialNet.logName, icon) }
            checkOtherAccountsTitleIsDisplayed()
            (SocialNet.getSupportedSocialNets() - SocialNet.TWITTER - DEFAULT_SOCIAL_PROFILES.map(SocialProfile::socialNet))
                .forEach { checkSocialNetIsUnbinded(it.logName) }
        }
    }

    @Test
    fun shouldSwitchToSocialNetsCounterWhenThreeOrLess() {
        performProfile {
            scrollToBottom()
        }
        for (i in DEFAULT_SOCIAL_PROFILES.indices) {
            performProfile {
                openSocialNetsDialog()
            }
            performBindSocialNets {
                unbindAccount(DEFAULT_SOCIAL_PROFILES[i].socialNet.logName)
            }
            checkProfile {
                val bindedProfiles = DEFAULT_SOCIAL_PROFILES
                    .subList(i + 1, DEFAULT_SOCIAL_PROFILES.size)
                    .map { it.icon }
                checkHasBindedSocialNets(
                    bindedProfiles.getOrNull(0),
                    bindedProfiles.getOrNull(1),
                    bindedProfiles.getOrNull(2)
                )
            }
        }
    }

    @Test
    fun shouldShowGosuslugiAsSocialNet() {
        webServerRule.routing {
            userSetup(
                user = WITH_GOSUSLUGI
            )
        }
        performProfile {
            scrollToBottom()
        }.checkResult {
            checkHasBindedSocialNets(
                firstIcon = GOSULSLUGI_PROFILE.icon
            )
        }
    }

    companion object {
        private val DEFAULT_SOCIAL_PROFILES = listOf(
            SocialProfile(SocialNet.YANDEX, R.drawable.ic_yandex),
            SocialProfile(SocialNet.GOOGLE, R.drawable.ic_google),
            SocialProfile(SocialNet.VK, R.drawable.ic_vk),
        )

        private val GOSULSLUGI_PROFILE =
            SocialProfile(SocialNet.GOSUSLUGI, R.drawable.ic_gos_logo)
    }
}

private data class SocialProfile(
    val socialNet: SocialNet,
    @DrawableRes val icon: Int
)
