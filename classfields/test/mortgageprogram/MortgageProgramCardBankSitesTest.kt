package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createStandardProgram
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 30.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramCardBankSitesTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createStandardProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(activityTestRule)

    @Test
    fun shouldShowBankSites() {
        configureWebServer {
            registerMortgageProgramBankSearchSuccess()
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
            listView.contains(sectionTitleItem(BANK_SITES_TITLE))
                .isViewStateMatches(
                    "MortgageProgramCardBankSitesTest/shouldShowBankSites/title"
                )
            listView.scrollTo(siteSnippet(SITE_ID))
                .click()
        }
        onScreen<SiteCardScreen> {
            titleView.waitUntil { isTextEquals(SITE_NAME) }
        }
    }

    @Test
    fun shouldShowBankSitesError() {
        configureWebServer {
            registerMortgageProgramBankSearchError()
            registerMortgageProgramBankSearchSuccess()
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            bankSitesErrorItem.waitUntil { listView.contains(this) }
            listView.contains(sectionTitleItem(BANK_SITES_TITLE))
            bankSitesErrorItem.view.click()

            siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerMortgageProgramBankSearch(response: MockResponse) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("bankId", BANK_ID)
                queryParam("rgid", RGID)
            },
            response
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramBankSearchSuccess() {
        registerMortgageProgramBankSearch(
            response {
                assetBody("MortgageProgramCardBankSitesTest/bankSites.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramBankSearchError() {
        registerMortgageProgramBankSearch(error())
    }

    companion object {

        private const val RGID = "587795"
        private const val BANK_ID = "10"
        private const val SITE_ID = "10"
        private const val SITE_NAME = "Апарт-комплекс «Royal Park» 10"

        private const val BANK_SITES_TITLE = "Новостройки, аккредитованные банком"
    }
}
