package com.yandex.mobile.realty.test.promo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.LauncherMainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FullscreenWelcomePromoScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 12/7/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FullscreenWelcomePromoTest {

    private val activityTestRule = LauncherMainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain = baseChainOf(
        SetupDefaultAppStateRule(welcomePromoShown = false),
        activityTestRule
    )

    @Test
    fun shouldShowPromoAndThenShowMore() {
        activityTestRule.launchActivity()

        onScreen<FullscreenWelcomePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches("FullscreenWelcomePromoTest/promo")
            okButton.click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(OWNER_LANDING_URL) }
        }
        onScreen<FullscreenWelcomePromoScreen> {
            promoView.doesNotExist()
        }
    }

    @Test
    fun shouldShowPromoAndThenClose() {
        activityTestRule.launchActivity()

        onScreen<FullscreenWelcomePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches("FullscreenWelcomePromoTest/promo")
            closeButton.click()
        }
        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
        }
        onScreen<FullscreenWelcomePromoScreen> {
            promoView.doesNotExist()
        }
    }

    private companion object {

        const val OWNER_LANDING_URL =
            "https://arenda.test.vertis.yandex.ru/app/owner/?only-content=true"
    }
}
