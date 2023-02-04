package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.VillageCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnGalleryScreen
import com.yandex.mobile.realty.core.robot.performOnVillageCardScreen
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.VillageCardScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 24.07.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class VillageCardCallButtonTest : CallButtonTest() {

    private val activityTestRule = VillageCardActivityTestRule(
        villageId = VILLAGE_ID,
        launchActivity = false
    )
    private val internetRule = InternetRule()

    @JvmField
    @Rule
    val rule: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        internetRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenFloatingCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "VILLAGE_CARD_TOP",
            currentScreen = "VILLAGE_CARD"
        )
        dispatcher.registerVillageCard()
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = "карточка КП",
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnVillageCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenContentCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "VILLAGE_CARD_TOP",
            currentScreen = "VILLAGE_CARD"
        )
        dispatcher.registerVillageCard()
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = "карточка КП",
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnVillageCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            collapseAppBar()
            scrollToPosition(lookup.matchesCommButtons())
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesContentCallButton())

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenGalleryCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "CARD_GALLERY",
            currentScreen = "VILLAGE_GALLERY"
        )
        dispatcher.registerVillageCard()
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = "галерея у КП",
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnVillageCardScreen {
            waitUntil { isGalleryViewShown() }
            tapOn(lookup.matchesGalleryView())
        }

        performOnGalleryScreen {
            waitUntil { isCallButtonShown() }
            tapOn(lookup.matchesCallButton())

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenVillageDeveloperSnippetCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_DEVELOPER_ID,
            eventPlace = "VILLAGE_DEVELOPER_OBJECT",
            currentScreen = "VILLAGE_CARD"
        )
        dispatcher.registerVillageCard()
        dispatcher.registerOfferWithSiteSearch()
        dispatcher.registerVillagePhone(VILLAGE_DEVELOPER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_DEVELOPER_ID,
            source = villageSnippet("в блоке других объектов от застройщика"),
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<VillageCardScreen> {
            appBar.collapse()

            villageSnippet(VILLAGE_DEVELOPER_ID)
                .waitUntil { listView.contains(this) }
                .callButton.click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowCallErrorWhenBadResponseReceived() {
        configureWebServer {
            registerVillageCard()
        }
        activityTestRule.launchActivity()

        performOnVillageCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Не\u00a0удалось позвонить. Попробуйте позже.") }
        }
    }

    @Test
    fun shouldShowCallErrorWhenEmptyResponseReceived() {
        configureWebServer {
            registerVillageCard()
            registerEmptyVillagePhones(VILLAGE_ID)
        }
        activityTestRule.launchActivity()

        performOnVillageCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Не\u00a0удалось позвонить. Попробуйте позже.") }
        }
    }

    @Test
    fun shouldShowNetworkErrorWhenInternetDisabled() {
        configureWebServer {
            registerVillageCard()
        }
        activityTestRule.launchActivity()

        performOnVillageCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            internetRule.turnOff()

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Нет соединения с интернетом") }
        }
    }

    private fun DispatcherRegistry.registerVillageCard() {
        register(
            request {
                path("2.0/village/$VILLAGE_ID/card")
            },
            response {
                assetBody("villageCard.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearch() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {
        private const val VILLAGE_ID = "0"
        private const val VILLAGE_DEVELOPER_ID = "2"
    }
}
