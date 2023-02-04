package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createStandardProgram
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 02.08.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageCardBankSitesFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createStandardProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeSiteFavoriteState() {
        testSiteSnippetFavoriteButton<MortgageProgramCardScreen>(
            siteId = SITE_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<MortgageProgramCardScreen> {
                    siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SITE_ID).view },
            webServerConfiguration = { registerSite() },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "на карточке ипотечной программы"
        )
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "1"
    }
}
