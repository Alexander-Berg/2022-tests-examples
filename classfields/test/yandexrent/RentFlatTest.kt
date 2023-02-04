package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.EventMatcher
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.provider.model.ShownRentInsurancePromo
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.CONTRACT_ID
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.RENT_ROLE_OWNER
import com.yandex.mobile.realty.test.services.contractInfo
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.services.registerRentFlat
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 28.03.2022
 */
@LargeTest
class RentFlatTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    private val shownRentInsurancePromo = ShownRentInsurancePromo().apply {
        contractId = CONTRACT_ID
    }

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        MetricaEventsRule(),
        activityTestRule,
        DatabaseRule(
            DatabaseRule.createAddShownRentInsurancePromoStatement(shownRentInsurancePromo)
        )
    )

    @Test
    fun shouldShowFullscreenError() {
        configureWebServer {
            registerRentFlatError()
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .retryButton
                .click()

            flatHeaderItem
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun checkToolbarWithInsurance() {
        val contractInfo = contractInfo(insuranceIsActive = true)
        configureWebServer {
            registerRentFlat(
                rentRole = RENT_ROLE_OWNER,
                contractInfo = contractInfo,
                notifications = listOf(
                    jsonObject {
                        "fallback" to jsonObject {
                            "title" to "Заполните анкету жильца"
                            "subtitle" to "Это нужно для проверки.\n".repeat(30)
                        }
                    },
                    jsonObject {
                        "fallback" to jsonObject {
                            "title" to "Оплата не прошла"
                        }
                    }
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }

            root.isViewStateMatches(getTestRelatedFilePath("expanded"))
            listView.scrollTo(notificationItem("Оплата не прошла"))

            root.isViewStateMatches(getTestRelatedFilePath("collapsed"))
        }
    }

    @Test
    fun switchTabs() {
        configureWebServer {
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationsEvent = openTabEvent("Уведомления")
        val docsEvent = openTabEvent("Документы")

        onScreen<RentFlatScreen> {
            waitUntil { listView.contains(flatHeaderItem) }

            waitUntil { notificationsEvent.isOccurred() }
            flatHeaderItem
                .view
                .invoke { documentsButton.click() }

            waitUntil { docsEvent.isOccurred() }
            flatHeaderItem
                .view
                .invoke { notificationsButton.click() }

            waitUntil { notificationsEvent.isOccurred(2) }
        }
    }

    private fun openTabEvent(tabName: String): EventMatcher {
        return event("Аренда. Карточка квартиры. Переход к разделу") {
            "Раздел" to tabName
        }
    }

    private fun DispatcherRegistry.registerRentFlatError() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID")
                queryParam("includeFeature", "SERVICES_AND_NOTIFICATIONS")
                queryParam("includeFeature", "NOTIFICATION_FALLBACKS")
                queryParam("includeFeature", "USER_NOTIFICATIONS")
            },
            error()
        )
    }
}
