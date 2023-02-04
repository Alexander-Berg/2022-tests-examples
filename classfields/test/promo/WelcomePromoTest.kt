package com.yandex.mobile.realty.test.promo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.LauncherMainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.screen.WelcomePromoScreen
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author solovevai on 01.06.2020.
 */
@LargeTest
@Ignore("Replaced by FullscreenWelcomePromoTest")
@RunWith(AndroidJUnit4::class)
class WelcomePromoTest {

    private val activityTestRule = LauncherMainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain = baseChainOf(
        SetupDefaultAppStateRule(welcomePromoShown = false),
        activityTestRule
    )

    @Test
    fun shouldShowPromoAndThenCloseFromToolbar() {
        activityTestRule.launchActivity()

        onScreen<WelcomePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches("/WelcomePromoTest/promo")
            closeButton.click()
        }
        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
        }
        onScreen<WelcomePromoScreen> {
            promoView.doesNotExist()
        }
    }

    @Test
    fun shouldShowPromoAndThenClose() {
        activityTestRule.launchActivity()

        onScreen<WelcomePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches("/WelcomePromoTest/promo")
            okButton.click()
        }
        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
        }
        onScreen<WelcomePromoScreen> {
            promoView.doesNotExist()
        }
    }
}
