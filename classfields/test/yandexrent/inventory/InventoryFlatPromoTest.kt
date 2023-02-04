package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.EventMatcher
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.InventoryFormScreen
import com.yandex.mobile.realty.core.screen.InventoryPreviewScreen
import com.yandex.mobile.realty.core.screen.RentFlatInventoryPromoScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.services.registerTenantRentFlat
import com.yandex.mobile.realty.test.yandexrent.OWNER_REQUEST_ID
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 6/28/22.
 */
@LargeTest
class InventoryFlatPromoTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            ownerRentFlatInventoryPromoShown = false,
            tenantRentFlatInventoryPromoShown = false
        ),
        MetricaEventsRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowOwnerPromoAndThenOpenInventoryForm() {
        configureWebServer {
            registerOwnerRentFlat(notification = fillInventoryNotification())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatInventoryPromoScreen> {
            waitUntil { ownerStoryView.isLoaded() }
            ownerStepEvent(1).waitUntil { isOccurred() }

            ownerStoryView.tapOnRightHalf()
            ownerStepEvent(2).waitUntil { isOccurred() }

            ownerStoryView.tapOnRightHalf()
            ownerStepEvent(3).waitUntil { isOccurred() }

            fillInventoryButton.waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<InventoryFormScreen> {
            waitUntil { toolbarTitleView.isTextEquals("Помещения и объекты") }

            val formEvent = event("Аренда. Опись. Собственник. Переход к формированию описи") {
                "Источник" to jsonObject {
                    "Карточка квартиры" to "Промо-визард создания описи собственника"
                }
            }
            waitUntil { formEvent.isOccurred() }
        }
    }

    @Test
    fun shouldShowTenantPromoAndThenOpenInventoryPreview() {
        configureWebServer {
            registerTenantRentFlat(notification = confirmInventoryNotification())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatInventoryPromoScreen> {
            waitUntil { tenantStoryView.isLoaded() }
            tenantStepEvent(1).waitUntil { isOccurred() }

            tenantStoryView.tapOnRightHalf()
            tenantStepEvent(2).waitUntil { isOccurred() }

            tenantStoryView.tapOnRightHalf()
            tenantStepEvent(3).waitUntil { isOccurred() }

            confirmInventoryButton.waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<InventoryPreviewScreen> {
            waitUntil { toolbarTitleView.isTextEquals("Опись имущества") }

            val formEvent = event("Аренда. Опись. Жилец. Переход к просмотру описи") {
                "Источник" to jsonObject {
                    "Карточка квартиры" to "Промо-визард подписания описи жильца"
                }
            }
            waitUntil { formEvent.isOccurred() }
        }
    }

    @Test
    fun shouldShowOwnerPromoAndThenClose() {
        configureWebServer {
            registerOwnerRentFlat(notification = fillInventoryNotification())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatInventoryPromoScreen> {
            waitUntil { ownerStoryView.isLoaded() }
            closeStoryButton.click()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowTenantPromoAndThenClose() {
        configureWebServer {
            registerTenantRentFlat(notification = confirmInventoryNotification())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatInventoryPromoScreen> {
            waitUntil { tenantStoryView.isLoaded() }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldNotShowPromosIfNoNotifications() {
        configureWebServer {
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    private fun fillInventoryNotification(): JsonObject {
        return jsonObject {
            "ownerNeedToFillOutInventory" to jsonObject {
                "ownerRequestId" to OWNER_REQUEST_ID
            }
        }
    }

    private fun confirmInventoryNotification(): JsonObject {
        return jsonObject {
            "tenantNeedToConfirmInventory" to jsonObject {
                "ownerRequestId" to OWNER_REQUEST_ID
            }
        }
    }

    private fun ownerStepEvent(step: Int): EventMatcher {
        return event("Промо-визард описи собственника. Показ визарда") {
            "Шаг" to step
        }
    }

    private fun tenantStepEvent(step: Int): EventMatcher {
        return event("Промо-визард описи жильца. Показ визарда") {
            "Шаг" to step
        }
    }
}
