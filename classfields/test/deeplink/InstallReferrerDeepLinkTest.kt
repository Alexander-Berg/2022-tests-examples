package com.yandex.mobile.realty.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.activity.LauncherMainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.InstallReferrerRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author merionkov on 13/01/2021.
 */
@RunWith(AndroidJUnit4::class)
class InstallReferrerDeepLinkTest {

    private val activityRule = LauncherMainActivityTestRule(launchActivity = false)

    private val installReferrerRule = InstallReferrerRule()

    @JvmField
    @Rule
    val ruleChain = baseChainOf(activityRule, installReferrerRule)

    @Test
    fun shouldOpenSiteCard() {
        installReferrerRule.setData("https://realty.yandex.ru/newbuilding/1")
        configureWebServer {
            registerRegionInfoSPB()
            registerSiteCard()
        }
        activityRule.launchActivity()
        onScreen<SiteCardScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }.isTextEquals("ЖК «Имя»")
        }
    }

    @Test
    fun shouldOpenSiteSearch() {
        installReferrerRule.setData("https://realty.yandex.ru/sankt-peterburg/kupit/novostrojka")
        configureWebServer {
            registerRegionInfoSPB()
            registerSearchDeepLink()
        }
        activityRule.launchActivity()
        onScreen<SearchListScreen> {
            listView.waitUntil { isCompletelyDisplayed() }
            filterButton.click()
        }
        onScreen<FiltersScreen> {
            isBuySelected()
            isApartmentSelected()
            isSiteSelected()
            geoSuggestValue.isTextEquals("Город Санкт-Петербург")
        }
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

    private fun DispatcherRegistry.registerSiteCard() {
        register(
            request {
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", "1")
            },
            response {
                assetBody("siteWithOfferStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSearchDeepLink() {
        register(
            request {
                path("1.0/deeplink.json")
                jsonBody {
                    "url" to "https://realty.yandex.ru/sankt-peterburg/kupit/novostrojka"
                }
            },
            response {
                assetBody("deepLinkTest/deepLinkSiteListSpb.json")
            }
        )
    }
}
