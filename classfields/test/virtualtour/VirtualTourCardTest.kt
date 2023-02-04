package com.yandex.mobile.realty.test.virtualtour

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
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
 * @author misha-kozlov on 4/16/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class VirtualTourCardTest {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowMatterportVirtualTourFromCardGallery() {
        shouldShowVirtualTourFromCardGallery("VirtualTourCardTest/offer_with_matterport_tour.json")
    }

    @Test
    fun shouldShowIframeVirtualTourFromCardGallery() {
        shouldShowVirtualTourFromCardGallery("VirtualTourCardTest/offer_with_iframe_tour.json")
    }

    private fun shouldShowVirtualTourFromCardGallery(fileName: String) {
        configureWebServer {
            registerOffer(fileName)
        }

        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(VIRTUAL_TOUR_URL), null)

        onScreen<OfferCardScreen> {
            waitUntil { photoCounter.isCompletelyDisplayed() }
            galleryView.swipeLeft()
            galleryView
                .isViewStateMatches(
                    "VirtualTourCardTest/" +
                        "shouldShowVirtualTourFromCardGallery/tourGalleryItem"
                )
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(VIRTUAL_TOUR_URL) }
            toolbarTitle.isTextEquals(R.string.tour_3d)
            menuButton.click()
            tourNotVisibleButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<ConfirmationDialogScreen> {
            waitUntil { titleView.isCompletelyDisplayed() }
            root.isViewStateMatches(
                "VirtualTourCardTest/shouldShowVirtualTourFromCardGallery/tourNotVisibleDialog"
            )
            confirmButton.click()
        }

        intended(matchesExternalViewUrlIntent(VIRTUAL_TOUR_URL))
    }

    @Test
    fun shouldShowMatterportVirtualTourFromFullscreenGallery() {
        shouldShowVirtualTourFromFullscreenGallery(
            "VirtualTourCardTest/offer_with_matterport_tour.json"
        )
    }

    @Test
    fun shouldShowIframeVirtualTourFromFullscreenGallery() {
        shouldShowVirtualTourFromFullscreenGallery(
            "VirtualTourCardTest/offer_with_iframe_tour.json"
        )
    }

    private fun shouldShowVirtualTourFromFullscreenGallery(fileName: String) {
        configureWebServer {
            registerOffer(fileName)
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { photoCounter.isCompletelyDisplayed() }
            galleryView.click()
        }
        onScreen<GalleryScreen> {
            galleryView
                .waitUntil { isCompletelyDisplayed() }
                .apply { swipeLeft() }
                .isViewStateMatches(
                    "VirtualTourCardTest/" +
                        "shouldShowVirtualTourFromFullscreenGallery/tourGalleryItem"
                )
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(VIRTUAL_TOUR_URL) }
        }
    }

    private fun DispatcherRegistry.registerOffer(fileName: String) {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    companion object {

        private const val OFFER_ID = "1"
        private const val VIRTUAL_TOUR_URL = "https://supertour/top?only-content=true"
    }
}
