package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.RentInventoryFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.InventoryFormScreen
import com.yandex.mobile.realty.core.screen.InventoryPromoScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.domain.model.yandexrent.InventoryFormContext
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 06.05.2022
 */
@LargeTest
class InventoryPromoTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentInventoryFormActivityTestRule(
        context = InventoryFormContext.Flat.FillInventoryNotification,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(ownerInventoryPromoShown = false),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowPromoAndThenCloseFromToolbar() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryPromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("promo"))
            closeButton.click()
        }
        onScreen<InventoryFormScreen> {
            waitUntil { proceedButton.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldShowPromoAndThenClose() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryPromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("promo"))
            okButton.click()
        }
        onScreen<InventoryFormScreen> {
            waitUntil { proceedButton.isCompletelyDisplayed() }
        }
    }
}
