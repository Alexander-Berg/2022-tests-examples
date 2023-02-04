package com.yandex.mobile.realty.test.yandexrent.nps

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentNpsPromoScreen
import com.yandex.mobile.realty.core.screen.RentNpsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ADDRESS
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.services.registerOwnerRentFlats
import com.yandex.mobile.realty.test.services.registerOwnerServicesInfo
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 6/16/22.
 */
@LargeTest
class RentNpsPromoTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(rentNpsPromoShown = false),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowPromoAndOpenNps() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerOwnerRentFlat(notification = npsNotification())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentNpsPromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("promo"))

            okButton.click()
        }

        onScreen<RentNpsScreen> {
            waitUntil { scoreView.isCompletelyDisplayed() }

            toolbarCloseButton.click()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowPromoAndClose() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerOwnerRentFlat(notification = npsNotification())
            registerOwnerRentFlat(notification = npsNotification())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentNpsPromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            toolbarCloseButton.click()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    private fun npsNotification(): JsonObject {
        return jsonObject {
            "netPromoterScoreNotification" to jsonObject {}
        }
    }
}
