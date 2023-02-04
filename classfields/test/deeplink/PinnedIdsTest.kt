package com.yandex.mobile.realty.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.DeepLinkIntentCommand
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author shpigun on 27/03/2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PinnedIdsTest {

    @JvmField
    @Rule
    val ruleChain = baseChainOf(SetupDefaultAppStateRule())

    @Test
    fun shouldOpenPinnedOfferId() {
        val url = "https://realty.yandex.ru/sankt-peterburg/kupit/kvartira/?pinnedOfferId=2"
        configureWebServer {
            registerDeepLink(
                responseFileName = "pinnedIdTest/deepLinkPinnedOfferId.json",
                url = url
            )
            registerRegionInfoSPB()
            registerListSearch(
                responseFileName = "pinnedIdTest/offerWithSiteSearchOfferPinned.json",
                params = arrayOf(
                    "pinnedOfferId" to "2",
                    "objectType" to "OFFER"
                )
            )
            registerListSearch(
                responseFileName = "offerWithSiteSearchDefaultSorting.json",
                params = arrayOf("objectType" to "OFFER"),
                excludeKey = "pinnedOfferId"
            )
        }

        DeepLinkIntentCommand.execute(url)

        onScreen<SearchListScreen> {
            offerSnippet("2").waitUntil { listView.contains(this) }
            filterButton.click()
        }
        onScreen<FiltersScreen> {
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet("1").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldOpenPinnedSiteId() {
        val url = "https://realty.yandex.ru/sankt-peterburg/kupit/novostrojka/?pinnedSiteId=2"
        configureWebServer {
            registerDeepLink(
                responseFileName = "pinnedIdTest/deepLinkPinnedSiteId.json",
                url = url
            )
            registerRegionInfoSPB()
            registerListSearch(
                responseFileName = "pinnedIdTest/offerWithSiteSearchSitePinned.json",
                params = arrayOf(
                    "pinnedSiteId" to "2",
                    "objectType" to "NEWBUILDING"
                )
            )
            registerListSearch(
                responseFileName = "offerWithSiteSearchSite.json",
                params = arrayOf("objectType" to "NEWBUILDING"),
                excludeKey = "pinnedSiteId"
            )
        }

        DeepLinkIntentCommand.execute(url)

        onScreen<SearchListScreen> {
            siteSnippet("2").waitUntil { listView.contains(this) }
            filterButton.click()
        }
        onScreen<FiltersScreen> {
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            siteSnippet("1").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldOpenPinnedVillageId() {
        val url = "https://realty.yandex.ru/sankt-peterburg/kupit/kottedzhnye-poselki/" +
            "?pinnedVillageId=1"
        configureWebServer {
            registerDeepLink(
                responseFileName = "pinnedIdTest/deepLinkPinnedVillageId.json",
                url = url
            )
            registerRegionInfoSPB()
            registerListSearch(
                responseFileName = "pinnedIdTest/offerWithSiteSearchVillagePinned.json",
                params = arrayOf(
                    "pinnedVillageId" to "1",
                    "objectType" to "VILLAGE"
                )
            )

            registerListSearch(
                responseFileName = "offerWithSiteSearchVillage.json",
                params = arrayOf("objectType" to "VILLAGE"),
                excludeKey = "pinnedVillageId"
            )
        }

        DeepLinkIntentCommand.execute(url)

        onScreen<SearchListScreen> {
            villageSnippet("1").waitUntil { listView.contains(this) }
            filterButton.click()
        }
        onScreen<FiltersScreen> {
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            villageSnippet("2").waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerDeepLink(responseFileName: String, url: String) {
        register(
            request {
                path("1.0/deeplink.json")
                body("{\"url\":\"$url\"}")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerListSearch(
        responseFileName: String,
        params: Array<Pair<String, String?>>,
        excludeKey: String? = null
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                for (item in params) {
                    item.let { (name, value) ->
                        value?.let { queryParam(name, it) }
                    }
                }
                excludeKey?.let { key ->
                    excludeQueryParamKey(key)
                }
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerRegionInfoSPB() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
            },
            response {
                assetBody("regionInfo417899.json")
            }
        )
    }
}
