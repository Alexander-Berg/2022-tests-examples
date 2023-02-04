package com.yandex.mobile.realty.test.virtualtour

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
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 4/16/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class VirtualTourSnippetTest : BaseTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldShowMatterportVirtualTourInGallery() {
        shouldShowVirtualTourInGallery("VirtualTourSnippetTest/offer_with_matterport_tour.json")
    }

    @Test
    fun shouldShowIframeVirtualTourInGallery() {
        shouldShowVirtualTourInGallery("VirtualTourSnippetTest/offer_with_iframe_tour.json")
    }

    private fun shouldShowVirtualTourInGallery(fileName: String) {
        configureWebServer {
            registerSearch(fileName)
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .apply { galleryView.scrollTo(virtualTourItem) }
                .galleryView
                .isViewStateMatches(getTestRelatedFilePath("tourGalleryItem"))
        }
    }

    private fun DispatcherRegistry.registerSearch(fileName: String) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    companion object {

        private const val OFFER_ID = "1"
    }
}
