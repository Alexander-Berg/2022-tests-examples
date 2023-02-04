package ru.auto.ara.test.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.favorites.performFavorites
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(AndroidJUnit4::class)
class FavoritesUnauthorizedTest {

    private val activityTestRule = activityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule(),
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun openFavorites() {
        performMain {
            openLowTab(R.string.favorites)
        }
    }

    @Test
    fun shouldOpenLogin() {
        performFavorites {
            interactions.onLoginButton().performClick()
        }
        performLogin {}.checkResult { isPhoneAuth() }
    }
}
