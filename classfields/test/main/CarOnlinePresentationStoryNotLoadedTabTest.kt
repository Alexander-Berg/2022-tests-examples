package ru.auto.ara.test.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.checkOffers
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CarOnlinePresentationStoryNotLoadedTabTest {

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        WebServerRule {
            userSetup()
        },
        activityScenarioRule<MainActivity>(),
        SetPreferencesRule(),
        SetupAuthRule()
    )

    @Test
    fun shouldNotSeeDotOnAddTabWhenNoStory() {
        checkMain {
            isLowTabTheSame(ADD_TAB, ADD_TAB_WITHOUT_DOT_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldNotOpenStoryWhenNotLoaded() {
        performMain { openLowTab(R.string.add_offer) }
        checkOffers {
            waitSomething(3, TimeUnit.SECONDS)
            isLoginButtonDisplayed()
        }
    }

    companion object {
        private const val ADD_TAB = R.string.add_offer
    }
}
