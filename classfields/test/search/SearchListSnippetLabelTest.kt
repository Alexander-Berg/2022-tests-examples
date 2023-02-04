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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrikeev on 03/03/2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListSnippetLabelTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldShowNewBuildingLabelOnly() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetLabelTest/offerWithSiteSearchNewBuilding.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    propertyLabelView.isTextEquals("Новостройка")
                    excerptLabelView.isHidden()
                    inactivityLabelView.isHidden()
                }
        }
    }

    @Test
    fun shouldShowOwnerLabelOnly() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetLabelTest/offerWithSiteSearchOwner.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    propertyLabelView.isTextEquals("Собственник")
                    excerptLabelView.isHidden()
                    inactivityLabelView.isHidden()
                }
        }
    }

    @Test
    fun shouldShowNewBuildingWithOwnerLabelOnly() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetLabelTest/offerWithSiteSearchNewBuildingWithOwner.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    propertyLabelView.isTextEquals("Новостройка, Собственник")
                    excerptLabelView.isHidden()
                    inactivityLabelView.isHidden()
                }
        }
    }

    @Test
    fun shouldShowExcerptLabelOnly() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetLabelTest/offerWithSiteSearchWithExcerpt.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    propertyLabelView.isHidden()
                    excerptLabelView.isCompletelyDisplayed()
                    inactivityLabelView.isHidden()
                }
        }
    }

    @Test
    fun shouldShowPropertyAndExcerptLabels() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetLabelTest" +
                    "/offerWithSiteSearchNewBuildingWithOwnerAndExcerpt.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    propertyLabelView.isTextEquals("Новостройка, Собственник")
                    excerptLabelView.isCompletelyDisplayed()
                    inactivityLabelView.isHidden()
                }
        }
    }

    @Test
    fun shouldShowNewBuildingWithOwnerAndInactiveLabel() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetLabelTest" +
                    "/offerWithSiteSearchNewBuildingWithOwnerAndInactive.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    propertyLabelView.isTextEquals("Новостройка, Собственник")
                    excerptLabelView.isHidden()
                    inactivityLabelView.isCompletelyDisplayed()
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
