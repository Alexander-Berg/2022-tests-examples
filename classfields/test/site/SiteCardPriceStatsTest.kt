package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.screen.SitePriceStatsScreen
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

@RunWith(AndroidJUnit4::class)
class SiteCardPriceStatsTest : BaseTest() {

    private var activityTestRule = SiteCardActivityTestRule(SITE_ID, launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldGoToStatsScreen() {
        configureWebServer {
            registerSiteWithOfferStat("offerWithNeutralStats")
            registerPriceStats()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            appBar.collapse()
            listView.waitUntil { contains(priceStatsItem) }
            listView.scrollByFloatingButtonHeight()
            priceStatsButton.click()
        }
        onScreen<SitePriceStatsScreen> {
            contentView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowStatsBlock() {
        configureWebServer {
            registerSiteWithOfferStat("offerWithoutStats")
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            appBar.collapse()
            listView.waitUntil {
                contains(galleryItem)
                doesNotContain(priceStatsItem)
            }
        }
    }

    @Test
    fun shouldShowPositiveStatsBlock() {
        testDisplayedBlockAppearance(
            responseFileName = "offerWithPositiveStats",
            screenshotFileName = "positiveStats",
        )
    }

    @Test
    fun shouldShowNegativeStatsBlock() {
        testDisplayedBlockAppearance(
            responseFileName = "offerWithNegativeStats",
            screenshotFileName = "negativeStats",
        )
    }

    @Test
    fun shouldShowNeutralStatsBlock() {
        testDisplayedBlockAppearance(
            responseFileName = "offerWithNeutralStats",
            screenshotFileName = "neutralStats",
        )
    }

    @Test
    fun shouldShowNotChangingStatsBlock() {
        testDisplayedBlockAppearance(
            responseFileName = "offerWithNotChangingStats",
            screenshotFileName = "notChangingStats",
        )
    }

    private fun testDisplayedBlockAppearance(
        responseFileName: String,
        screenshotFileName: String,
    ) {
        configureWebServer {
            registerSiteWithOfferStat(responseFileName)
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            appBar.collapse()
            listView.waitUntil { contains(priceStatsItem) }
            listView.scrollByFloatingButtonHeight()
            root.isViewStateMatches(getTestRelatedFilePath(screenshotFileName))
        }
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat(responseFileName: String) {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
            },
            response {
                assetBody("SiteCardPriceStatsTest/$responseFileName.json")
            }
        )
    }

    private fun DispatcherRegistry.registerPriceStats() {
        register(
            request {
                method("GET")
                path("2.0/newbuilding/$SITE_ID/price-statistics-series")
            },
            response {
                assetBody("SitePriceStatsScreenTest/priceStats.json")
            }
        )
    }

    private companion object {
        const val SITE_ID = "0"
    }
}
