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
 * @author shpigun on 2019-10-04
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListSnippetDeveloperTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun testSearchListSiteSnippetDeveloper() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetDeveloperTest/offerWithSiteSearchSiteDeveloper.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    developerView.isTextEquals("Застройщик ГК «КОРТРОС»")
                }
        }
    }

    @Test
    fun testSearchListSiteSnippetSeveralDevelopers() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetDeveloperTest/offerWithSiteSearchSiteSeveralDevelopers.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    developerView.isTextEquals("Застройщики: ГК «КОРТРОС», ГК ПИК")
                }
        }
    }

    @Test
    fun testSearchListVillageSnippetDeveloper() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetDeveloperTest/offerWithSiteSearchVillageDeveloper.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            villageSnippet("1823206")
                .waitUntil { listView.contains(this) }
                .invoke {
                    developerView.isTextEquals("Застройщик Бавария CLUB")
                }
        }
    }

    @Test
    fun testSearchListVillageSnippetSeveralDevelopers() {
        configureWebServer {
            registerOfferWithSiteSearch(
                "searchListSnippetDeveloperTest" +
                    "/offerWithSiteSearchVillageSeveralDevelopers.json"
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            villageSnippet("1823206")
                .waitUntil { listView.contains(this) }
                .invoke {
                    developerView.isTextEquals("Застройщики: Бавария CLUB, Эко-Лоджик")
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
