package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnSiteCardScreen
import com.yandex.mobile.realty.core.robot.performOnYouTubePlayerScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author sorokinandrei on 11/24/20.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardVideoTest {

    private var activityTestRule = SiteCardActivityTestRule(
        siteId = "0",
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldOpenVideo() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllInfoExtended.json")
        }

        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            collapseAppBar()
            waitUntil { containsVideoPreview() }
            tapOn(lookup.matchesVideoPreview())
        }
        performOnYouTubePlayerScreen {
            containsVideoView(VIDEO_ID)
        }
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat(responseFileName: String) {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteCardTest/$responseFileName")
            }
        )
    }

    companion object {
        private const val SITE_ID = "0"
        private const val VIDEO_ID = "videoId"
    }
}
