package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.shark.getEmptyActiveApplicaiton
import ru.auto.ara.core.dispatchers.shark.getOneActiveApplicationNoProfile
import ru.auto.ara.core.dispatchers.shark.getOneCreditProduct
import ru.auto.ara.core.dispatchers.shark.getOneDraftApplicationNoProfile
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.loanbroker.checkLoanLK
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.search_filter.performPriceFilter
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanPopupInParameters
import java.util.concurrent.TimeUnit

private const val SHOWN_POPUP_PREFS_KEY = "shown_loan_popup"
private const val PRICE_FIELD_NAME = "Цена, \u20BD"

@RunWith(AndroidJUnit4::class)
class LoanBrokerPopupOnFiltersTest {

    private val activityRule = ActivityTestRule(MainActivity::class.java, false, true)
    private val webServerRule = WebServerRule {
        userSetup()
        getOneCreditProduct()
    }

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule(),
        DisableAdsRule(),
        arguments = TestMainModuleArguments(
            experimentsOf { Experiments::loanPopupInParameters then true }
        )
    )

    @Test
    fun shouldShowPopupIfNoActiveApplication() {
        webServerRule.routing { getEmptyActiveApplicaiton() }
        performMain { openFilters() }
        performFilter {
            doSearch()
        }
        checkLoanLK { isLoanLK() }
        checkCommon {
            isPrefs {
                MatcherAssert.assertThat("Prefs key should be set", getBoolean(SHOWN_POPUP_PREFS_KEY, false))
            }
        }
    }

    @Test
    fun shouldNotShowPopupIfHaveActiveApplication() {
        webServerRule.routing { getOneActiveApplicationNoProfile() }
        performMain { openFilters() }
        performFilter {
            doSearch()
        }
        checkSearchFeed {
            isEmptyMMNGFilterDisplayed()
        }
    }

    @Test
    fun shouldNotShowPopupIfHaveDraftApplication() {
        webServerRule.routing { getOneDraftApplicationNoProfile() }
        performMain { openFilters() }
        performFilter {
            doSearch()
        }
        checkSearchFeed {
            isEmptyMMNGFilterDisplayed()
        }
    }

    @Test
    fun shouldNotShowPopupAfterClosed() {
        webServerRule.routing { getEmptyActiveApplicaiton() }
        performMain { openFilters() }
        performFilter {
            doSearch()
        }
        performCommon { pressSystemKeyBack() }
        performFilter {
            doSearch()
        }
        checkSearchFeed {
            isEmptyMMNGFilterDisplayed()
        }
    }

    @Test
    fun shouldNotShowPopupIfPrefsWritten() {
        webServerRule.routing { getEmptyActiveApplicaiton() }
        performCommon {
            editPrefs {
                putBoolean(SHOWN_POPUP_PREFS_KEY, true)
            }
        }
        performMain { openFilters() }
        performFilter {
            doSearch()
        }
        checkSearchFeed {
            isEmptyMMNGFilterDisplayed()
        }
    }

    @Test
    fun shouldNotShowPopupIfCreditFilterIsActive() {
        webServerRule.routing { getEmptyActiveApplicaiton() }
        performMain { openFilters() }
        waitSomething(1, TimeUnit.SECONDS)
        performFilter { clickField(PRICE_FIELD_NAME) }
        performPriceFilter { clickCreditSwitch() }
        performFilter {
            clickAcceptButton()
            doSearch()
        }
        checkSearchFeed {
            isEmptyMMNGFilterDisplayed()
        }
    }
}

@RunWith(AndroidJUnit4::class)
class LoanBrokerPopupOnFiltersUnauthorizedTest {

    private val activityRule = ActivityTestRule(MainActivity::class.java, false, true)
    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule,
        DisableAdsRule(),
        arguments = TestMainModuleArguments(
            experimentsOf { Experiments::loanPopupInParameters.then(true) }
        )
    )

    @Test
    fun shouldShowPopupIfNotAuthorized() {
        webServerRule.routing { getEmptyActiveApplicaiton().watch { checkRequestWasNotCalled() } }
        performMain { openFilters() }
        performFilter {
            doSearch()
        }
        checkLoanLK { isLoanLK() }
        checkCommon {
            isPrefs {
                MatcherAssert.assertThat("Prefs key should be set", getBoolean(SHOWN_POPUP_PREFS_KEY, false))
            }
        }
    }
}
