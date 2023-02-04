package ru.auto.ara.test.main.transport

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.last_searches.SearchHistory
import ru.auto.ara.core.dispatchers.last_searches.getSearchHistory
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.burger.performBurger
import ru.auto.ara.core.robot.transporttab.checkTransport
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class TransportLastSearchesForUnauthorizeTest {

    private val webServerRule = WebServerRule {
        postEmptyGarageListing()
    }
    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        DisableAdsRule(),
    )

    @Test
    fun shouldSeeLastSearchesAfterLogin() {
        activityRule.launchActivity()
        performMain { openBurgerMenu() }
        performBurger {
            scrollAndClickOnUserItemForAnon()
        }
        webServerRule.routing {
            getSearchHistory(SearchHistory.ONE_ITEM)
            userSetup()
            postLoginOrRegisterSuccess()
        }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        performBurger { clickOnClose() }
        performMain { openLowTab(R.string.search) }
        checkTransport { isLastSearchVisible() }
    }

    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"
}
