package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
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
 * @author pvl-zolotov on 29.05.2022
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardVirtualToursTest : BaseTest() {

    private var activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(activityTestRule)

    @Test
    fun shouldShowAndOpenOverview360() {
        configureWebServer {
            registerSiteWithOffersStatOverview360()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(overviews360) }
            listView.scrollByFloatingButtonHeight()
            listView.isVirtualToursStateMatches(getTestRelatedFilePath("overview360"))

            virtualTourItem("Общий вид, строительство").click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(OVERVIEW_360_URL) }
            toolbarTitle.isTextEquals(R.string.overview_360)
        }
    }

    @Test
    fun shouldShowAndOpenTour3d() {
        configureWebServer {
            registerSiteWithOffersStatTour3d()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(tours3d) }
            listView.scrollByFloatingButtonHeight()
            listView.isVirtualToursStateMatches(getTestRelatedFilePath("tour3d"))

            virtualTourItem("Территория и входные группы").click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(TOUR_3D_URL) }
            toolbarTitle.isTextEquals(R.string.tour_3d)
        }
    }

    @Test
    fun shouldShowTour3dAndOverview360() {
        configureWebServer {
            registerSiteWithOffersStatTour3dAndOverview360()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(overviews360) }
            waitUntil { listView.contains(tours3d) }
        }
    }

    private fun DispatcherRegistry.registerSiteWithOffersStatOverview360() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("SiteCardVirtualToursTest/siteWithOfferStatOverview360.json")
            },
        )
    }

    private fun DispatcherRegistry.registerSiteWithOffersStatTour3d() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("SiteCardVirtualToursTest/siteWithOfferStatTour3d.json")
            },
        )
    }

    private fun DispatcherRegistry.registerSiteWithOffersStatTour3dAndOverview360() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("SiteCardVirtualToursTest/siteWithOfferStatTour3dAndOverview360.json")
            },
        )
    }

    private companion object {

        const val SITE_ID = "0"
        private const val TOUR_3D_URL = "https://tour3d/top?only-content=true"
        private const val OVERVIEW_360_URL = "https://overview360/top?only-content=true"
    }
}
