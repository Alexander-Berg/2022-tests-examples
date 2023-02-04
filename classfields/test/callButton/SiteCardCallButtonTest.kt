package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnGalleryScreen
import com.yandex.mobile.realty.core.robot.performOnSiteCardScreen
import com.yandex.mobile.realty.core.robot.performOnSiteSpecialsScreen
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
 * Created by Alena Malchikhina on 24.07.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardCallButtonTest : CallButtonTest() {

    private val activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
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
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_CARD_TOP",
            currentScreen = "NEWBUILDING_CARD"
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = "карточка новостройки",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteCardScreen {
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
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_CARD_TOP",
            currentScreen = "NEWBUILDING_CARD"
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = "карточка новостройки",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteCardScreen {
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
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "CARD_GALLERY",
            currentScreen = "NEWBUILDING_GALLERY"
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = "галерея у новостройки",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteCardScreen {
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
    fun shouldStartCallWhenSiteSpecialsCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_SPECIALS",
            currentScreen = "NEWBUILDING_SPECIALS"
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = "специальные предложения новостройки",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            collapseAppBar()
            containsSpecialProposals()
            tapOn(lookup.matchesSpecialProposalView("Скидка 20%"))
        }

        performOnSiteSpecialsScreen {
            waitUntil { isCallButtonShown() }
            tapOn(lookup.matchesCallButton())

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenSimilarSnippetCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SIMILAR_SITE_ID,
            eventPlace = "NEWBUILDING_RECOMMENDATIONS",
            currentScreen = "NEWBUILDING_CARD"
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSimilarSites()
        dispatcher.registerSitePhone(SIMILAR_SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SIMILAR_SITE_ID,
            source = siteSnippet("в блоке похожих ЖК"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            collapseAppBar()

            waitUntil { containsSimilarSiteSnippet(SIMILAR_SITE_ID) }
            performOnSiteSnippet(SIMILAR_SITE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenSiteDeveloperSnippetCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = DEVELOPER_SITE_ID,
            eventPlace = "NEWBUILDING_DEVELOPER_OBJECT",
            currentScreen = "NEWBUILDING_CARD"
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerOfferWithSiteSearch()
        dispatcher.registerSitePhone(DEVELOPER_SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = DEVELOPER_SITE_ID,
            source = siteSnippet("в блоке других объектов от застройщика"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            collapseAppBar()

            waitUntil { containsDeveloperSiteSnippet(DEVELOPER_SITE_ID) }
            performOnSiteSnippet(DEVELOPER_SITE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowCallErrorWhenBadResponseReceived() {
        configureWebServer {
            registerSiteWithOfferStat()
        }
        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Не\u00a0удалось позвонить. Попробуйте позже.") }
        }
    }

    @Test
    fun shouldShowCallErrorWhenEmptyResponseReceived() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerEmptySitePhones(SITE_ID)
        }
        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Не\u00a0удалось позвонить. Попробуйте позже.") }
        }
    }

    @Test
    fun shouldShowNetworkErrorWhenInternetDisabled() {
        configureWebServer {
            registerSiteWithOfferStat()
        }
        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            internetRule.turnOff()

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Нет соединения с интернетом") }
        }
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteCardTest/siteWithOfferStatFiveDetails.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarSites() {
        register(
            request {
                method("GET")
                path("1.0/newbuilding/siteLikeSearch")
                queryParam("siteId", SITE_ID)
                queryParam("excludeSiteId", SITE_ID)
                queryParam("pageSize", "4")
            },
            response {
                assetBody("siteLikeSearch.json")
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
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    companion object {
        private const val SITE_ID = "0"
        private const val SIMILAR_SITE_ID = "2"
        private const val DEVELOPER_SITE_ID = "1"
    }
}
