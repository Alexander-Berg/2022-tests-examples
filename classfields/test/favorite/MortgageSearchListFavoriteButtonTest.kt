package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 28.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageSearchListFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = MortgageProgramListActivityTestRule(
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
    fun shouldChangeOfferFavoriteState() {
        testOfferSnippetFavoriteButton<MortgageProgramListScreen>(
            offerId = OFFER_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<MortgageProgramListScreen> {
                    offerSnippet(OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            webServerConfiguration = {
                registerOffer()
                registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
                registerMortgageProgramSearch(
                    "mortgageProgramSearchDefault.json",
                    rgid = RGID
                )
            },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "в блоке подходящих квартир ипотечных программ"
        )
    }

    @Test
    fun shouldChangeSiteFavoriteState() {
        testSiteSnippetFavoriteButton<MortgageProgramListScreen>(
            siteId = SITE_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<MortgageProgramListScreen> {
                    siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SITE_ID).view },
            webServerConfiguration = {
                registerSite()
                registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
                registerMortgageProgramSearch(
                    "mortgageProgramSearchDefault.json",
                    rgid = RGID
                )
            },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "в блоке подходящих квартир ипотечных программ"
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        responseFileName: String,
        page: Int = 0,
        rgid: String?
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                rgid?.let { queryParam("rgid", it) }
                queryParam("page", page.toString())
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val RGID = "587795"
    }
}
