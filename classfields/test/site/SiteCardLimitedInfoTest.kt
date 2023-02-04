package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
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
 * @author shpigun on 18.12.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardLimitedInfoTest : BaseTest() {

    private var activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun showSiteInfoInLimitedCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviews()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            siteInfoSectionTitleItem.waitUntil { listView.contains(this) }

            listView.scrollTo(siteInfoSectionTitleItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            review(REVIEW_ID).waitUntil { listView.contains(this) }

            listView.isItemsStateMatches(
                getTestRelatedFilePath("info"),
                siteInfoSectionTitleItem,
                commButtons
            )
        }
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteCardTest/siteWithOfferStatAllInfoLimited.json")
            }
        )
    }

    private fun DispatcherRegistry.registerReviews() {
        register(
            request {
                method("POST")
                path("2.0/graphql")
                jsonPartialBody {
                    "operationName" to "GetReviews"
                    "variables" to JsonObject().apply { addProperty("id", SITE_PERMALINK) }
                }
            },
            response {
                assetBody("siteCardTest/siteReviews.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "0"
        private const val REVIEW_ID = "B16Urs9-EwxdAtunIhYduI7Eqx6h1Pkb"
        private const val SITE_PERMALINK = "182242396448"
    }
}
