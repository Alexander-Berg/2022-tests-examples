package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.robot.performOnGalleryScreen
import com.yandex.mobile.realty.core.robot.performOnSiteCardScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 11/24/20.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardDecorationTest {

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

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun shouldOpenDecorationGallery() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllInfoExtended.json")
        }

        repeat(3) { index ->
            val name = "test_image_$index"
            createImageOnExternalDir(name, rColor = 255, gColor = 0, bColor = 0)
        }

        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            collapseAppBar()
            waitUntil { containsDecorationImages() }
            scrollRecyclerViewTo(lookup.matchesDecorationImages(), position = 2)
                .tapOn()
        }
        performOnGalleryScreen {
            waitUntil { containsToolbarTitle("ЖК «Имя»") }
            isCallButtonShown()
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
    }
}
