package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 2019-10-04
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListSnippetSpecialTest : BaseTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun testSearchListSiteSnippetSpecialSale() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetSpecialTest/offerWithSiteSearchSpecialSale.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("specialSale"))
                }
        }
    }

    @Test
    fun testSearchListSiteSnippetSpecialInstallment() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetSpecialTest/offerWithSiteSearchSpecialInstallment.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("specialInstallment"))
                }
        }
    }

    @Test
    fun testSearchListSiteSnippetSpecialMortgage() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetSpecialTest/offerWithSiteSearchSpecialMortgage.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("specialMortgage"))
                }
        }
    }

    @Test
    fun testSearchListSiteSnippetSpecialGift() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetSpecialTest/offerWithSiteSearchSpecialGift.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("specialGift"))
                }
        }
    }

    @Test
    fun testSearchListSiteSnippetSpecialDiscount() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetSpecialTest/offerWithSiteSearchSpecialDiscount.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("specialDiscount"))
                }
        }
    }

    @Test
    fun testSearchListSiteSnippetSpecialUnknown() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetSpecialTest/offerWithSiteSearchSpecialUnknown.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("specialUnknown"))
                }
        }
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearch(responseFileName: String) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }
}
