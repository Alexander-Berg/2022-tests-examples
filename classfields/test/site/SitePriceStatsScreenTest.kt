package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.activity.SitePriceStatsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.allure.step
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SitePriceStatsScreen
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
class SitePriceStatsScreenTest : BaseTest() {

    private var activityTestRule = SitePriceStatsActivityTestRule(
        siteId = SITE_ID,
        siteName = SITE_NAME,
        launchActivity = false,
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun interactWithPriceStats() {
        configureWebServer {
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
        activityTestRule.launchActivity()
        onScreen<SitePriceStatsScreen> {
            contentView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("studioHalfYear"))
            periodYearButton.click()
            root.isViewStateMatches(getTestRelatedFilePath("studioYear"))
            step("Выделяем точку в зоне \"заморозки\"") {
                chartView.highlightValue(2, 0)
            }
            root.isViewStateMatches(getTestRelatedFilePath("studioYearHighlighted"))
            periodAllButton.click()
            root.isViewStateMatches(getTestRelatedFilePath("studioAll"))
            twoRoomsButton.click()
            root.isViewStateMatches(getTestRelatedFilePath("twoRoomsAll"))
            periodHalfYearButton.click()
            root.isViewStateMatches(getTestRelatedFilePath("twoRoomsHalfYear"))
            fourPlusRoomsButton.click()
            root.isViewStateMatches(getTestRelatedFilePath("fourPlusRooms"))
        }
    }

    private companion object {
        const val SITE_ID = "0"
        const val SITE_NAME = "ЖК «Название»"
    }
}
