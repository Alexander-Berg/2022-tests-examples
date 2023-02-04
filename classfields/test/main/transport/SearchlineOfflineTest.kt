package ru.auto.ara.test.main.transport

import android.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.searchline.checkSearchline
import ru.auto.ara.core.robot.searchline.performSearchline
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.OfflineRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.closeSoftKeyboard
import ru.auto.ara.core.utils.getContextUnderTest
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class SearchlineOfflineTest {

    private val webRule = WebServerRule {
        userSetup()
        delegateDispatcher(CountDispatcher("cars"))
        delegateDispatcher(PostSearchOffersDispatcher.getGenericFeed())
    }

    private val activityRule = activityScenarioRule<MainActivity>()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(getContextUnderTest())

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webRule,
        activityRule,
        SetupAuthRule(),
        SetPreferencesRule(),
        OfflineRule()
    )

    @Test
    fun shouldSeeErrorOnInputWhenOffline() {
        performMain { openSearchline() }
        performSearchline { waitScreenLoaded() }
        checkSearchline {
            isDefaultSuggestsTitleDisplayed(0)
            areRandomDefaultsVisible()
        }
        performSearchline { replaceText("Ford Focus") }
        performCommon { closeSoftKeyboard() }
        checkSearchline { isOfflineErrorVisible() }
    }

    @Test
    fun shouldRetryRequestOnErrorAction() {
        performMain { openSearchline() }
        performSearchline { waitScreenLoaded() }
        performSearchline {
            replaceText("Ford Focus")
            performCommon { closeSoftKeyboard() }
            clickOnErrorAction()
        }
        checkSearchline { isOfflineErrorVisible() }
    }

    @Test
    fun shouldSeeDefaultsAfterClearTextFromClearButton() {
        performMain { openSearchline() }
        performSearchline { waitScreenLoaded() }
        performSearchline {
            replaceText("Ford Focus")
            waitSomething(1, TimeUnit.SECONDS)
            clickOnClear()
        }
        performCommon { closeSoftKeyboard() }
        checkSearchline {
            isDefaultSuggestsTitleDisplayed(0)
            areRandomDefaultsVisible()
        }
    }

}
