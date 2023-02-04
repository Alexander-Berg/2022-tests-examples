package com.yandex.mobile.realty.test.galleryPlan

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TView
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
 * @author pvl-zolotov 2021-10-18
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlanButtonsVisibilityTest {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowPlanButtonsFromCardGallery() {
        configureWebServer {
            registerOffer()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { photoCounter.isCompletelyDisplayed() }
            checkAllSlides(apartmentPlanButton, floorPlanButton, galleryView, this)
        }
    }

    @Test
    fun shouldShowPlanButtonsFromFullscreenGallery() {
        configureWebServer {
            registerOffer()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { photoCounter.isCompletelyDisplayed() }
            galleryView.click()
        }
        onScreen<GalleryScreen> {
            galleryView.waitUntil { isCompletelyDisplayed() }
            checkAllSlides(apartmentPlanButton, floorPlanButton, galleryView, this)
        }
    }

    private fun checkAllSlides(
        apartmentPlanButton: TView,
        floorPlanButton: TView,
        gallery: TView,
        screen: Screen<*>
    ) {
        screen.apply {
            apartmentPlanButton.waitUntil { isHidden() }
            floorPlanButton.waitUntil { isCompletelyDisplayed() }

            gallery.swipeLeft()

            apartmentPlanButton.waitUntil { isHidden() }
            floorPlanButton.waitUntil { isHidden() }

            gallery.swipeLeft()

            apartmentPlanButton.waitUntil { isCompletelyDisplayed() }
            floorPlanButton.waitUntil { isHidden() }

            gallery.swipeLeft()

            apartmentPlanButton.waitUntil { isCompletelyDisplayed() }
            floorPlanButton.waitUntil { isHidden() }

            gallery.swipeLeft()

            apartmentPlanButton.waitUntil { isHidden() }
            floorPlanButton.waitUntil { isHidden() }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("PlanButtonTest/offer_plan_apartment_and_floor.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "1"
    }
}
