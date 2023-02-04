package com.yandex.mobile.realty.test.services

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentUtilitiesPromoScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 2/21/22.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TenantRentUtilitiesPromoTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain = baseChainOf(
        SetupDefaultAppStateRule(tenantRentUtilitiesPromoShown = false),
        activityTestRule
    )

    @Test
    fun shouldShowPromoTenant() {
        configureWebServer {
            registerTenantRentFlat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentUtilitiesPromoScreen> {
            waitUntil { tenantStoryView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowPromoOwner() {
        configureWebServer {
            registerOwnerRentFlat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
        }

        onScreen<RentUtilitiesPromoScreen> {
            waitUntil { tenantStoryView.doesNotExist() }
        }
    }
}
