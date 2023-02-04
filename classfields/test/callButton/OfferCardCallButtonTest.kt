package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnGalleryScreen
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnPhonesDialog
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author misha-kozlov on 22.04.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OfferCardCallButtonTest : CallButtonTest() {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)
    private val internetRule = InternetRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        internetRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenContentCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "CARD_TOP",
            currentScreen = "OFFER_CARD"
        )
        dispatcher.registerOffer()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = "карточка объявления",
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnOfferCardScreen {
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
    fun shouldStartCallWhenFloatingCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "CARD_TOP",
            currentScreen = "OFFER_CARD"
        )
        dispatcher.registerOffer()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = "карточка объявления",
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenSimilarOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = SIMILAR_OFFER_ID,
            eventPlace = "CARD_RECOMMENDATIONS",
            currentScreen = "OFFER_CARD"
        )
        dispatcher.registerOffer()
        dispatcher.registerSimilarOffers()
        dispatcher.registerOfferPhone(SIMILAR_OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = SIMILAR_OFFER_ID,
            source = offerSnippet("в блоке похожих объявлений"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            collapseAppBar()

            waitUntil { containsSimilarOfferSnippet(SIMILAR_OFFER_ID) }
            performOnSimilarOfferSnippet(SIMILAR_OFFER_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenGalleryCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "CARD_GALLERY",
            currentScreen = "OFFER_GALLERY"
        )
        dispatcher.registerOffer()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = "галерея с фото",
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnOfferCardScreen {
            waitUntil { isPhotosCounterShown() }

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
    fun shouldStartCallWhenFewPhoneNumbersExists() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "CARD_TOP",
            currentScreen = "OFFER_CARD"
        )
        dispatcher.registerOffer()
        dispatcher.registerFewOfferPhones(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = "карточка объявления",
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            tapOn(lookup.matchesFloatingCallButton())
        }
        performOnPhonesDialog {
            waitUntil { isPhoneShown(PHONE) }
            tapOn(lookup.matchesPhone(PHONE))

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowCallErrorWhenBadResponseReceived() {
        configureWebServer {
            registerOffer()
        }
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Не\u00a0удалось позвонить. Попробуйте позже.") }
        }
    }

    @Test
    fun shouldShowCallErrorWhenEmptyResponseReceived() {
        configureWebServer {
            registerOffer()
            registerEmptyOfferPhones(OFFER_ID)
        }
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Не\u00a0удалось позвонить. Попробуйте позже.") }
        }
    }

    @Test
    fun shouldShowNetworkErrorWhenInternetDisabled() {
        configureWebServer {
            registerOffer()
        }
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            internetRule.turnOff()

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Нет соединения с интернетом") }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("callButtonTest/cardWithViewsLarge.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                assetBody("callButtonTest/similar.json")
            }
        )
    }

    private companion object {
        private const val OFFER_ID = "0"
        private const val SIMILAR_OFFER_ID = "1"
    }
}
