package com.yandex.mobile.realty.test.yandexrent.showingcard

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.ShowingCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.RentShowingCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.SHOWING_ID
import com.yandex.mobile.realty.test.yandexrent.showings.Showings
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class ShowingCardTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ShowingCardActivityTestRule(showingId = SHOWING_ID, launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MetricaEventsRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun openShowingCard() {
        configureWebServer {
            registerShowingDetailsError()
            registerShowingDetails(
                body = Showing.body(
                    widget = Showing.widgetWithUrlFallback(),
                    generalInfo = Showing.fullGeneralInfo(),
                    houseServices = Showing.fullHouseServices(),
                    facilities = Showing.fullFacilities()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingCardScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .invoke { retryButton.click() }

            waitUntil { listView.contains(headerItem) }
            listView.isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun openOfferCard() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.body(widget = Showing.widgetWithUrlFallback())
            )
            registerOfferCard()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingCardScreen> {
            headerItem
                .waitUntil { listView.contains(this) }
                .invoke { openOfferButton.click() }
        }

        onScreen<OfferCardScreen> {
            priceView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun openHtmlLink() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.body(widget = Showing.widgetWithUrlFallback())
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(Showing.WIDGET_LINK_URL), null)

        onScreen<RentShowingCardScreen> {
            headerItem
                .waitUntil { listView.contains(this) }
                .invoke { notificationView.tapOnLinkText(Showing.WIDGET_LINK_TEXT) }

            waitUntil { intended(matchesExternalViewUrlIntent(Showing.WIDGET_LINK_URL)) }
        }
    }

    @Test
    fun openVirtualTour() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.body(widget = Showing.widgetWithUrlFallback())
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingCardScreen> {
            virtualTourItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(Showing.VIRTUAL_TOUR_URL) }
            toolbarTitle.isTextEquals(R.string.tour_3d)
        }
    }

    @Test
    fun openRoommatesList() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.body(widget = Showing.widgetWithUrlFallback())
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingCardScreen> {
            roommatesItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<WebViewScreen> {
            webView.waitUntil { isPageUrlEquals(Showings.ROOMMATES_LIST_URL) }
            event("Аренда. Переход к просмотру списка сожителей") {
                "Источник" to "Карточка показа"
            }.waitUntil { isOccurred() }
        }
    }

    @Test
    fun openFallbackUrl() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.body(
                    widget = Showing.widgetWithUrlFallback(),
                    generalInfo = Showing.simpleGeneralInfo()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(Showing.FALLBACK_URL), null)

        onScreen<RentShowingCardScreen> {
            accentActionButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            waitUntil { intended(matchesExternalViewUrlIntent(Showing.FALLBACK_URL)) }
        }
    }

    @Test
    fun showUpdateFallback() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.body(
                    widget = Showing.widgetWithUpdateFallback(),
                    generalInfo = Showing.simpleGeneralInfo()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerMarketIntent()

        onScreen<RentShowingCardScreen> {
            notificationItem(getResourceString(R.string.yandex_rent_update_fallback_title))
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }

            intended(matchesMarketIntent())
            listView.isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun openAlmostEmptyShowingCard() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.simpleBody(widget = Showing.widgetWithoutAction())
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingCardScreen> {
            waitUntil { listView.contains(headerItem) }
            listView.isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun openShowingWithInfoWidget() {
        checkShowingWithWidget(Showings.SHOWING_TYPE_INFO)
    }

    @Test
    fun openShowingWithAccentWidget() {
        checkShowingWithWidget(Showings.SHOWING_TYPE_ACCENT)
    }

    @Test
    fun openShowingWithImportantWidget() {
        checkShowingWithWidget(Showings.SHOWING_TYPE_IMPORTANT)
    }

    @Test
    fun openShowingWithWarningWidget() {
        checkShowingWithWidget(Showings.SHOWING_TYPE_WARNING)
    }

    @Test
    fun openShowingWithUnknownTypeWidget() {
        checkShowingWithWidget(Showings.SHOWING_TYPE_UNKNOWN)
    }

    private fun checkShowingWithWidget(type: String) {
        configureWebServer {
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetWithUrlFallback(
                        type = type
                    )
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingCardScreen> {
            waitUntil { listView.contains(headerItem) }
            listView.isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    private fun DispatcherRegistry.registerShowingDetailsError() {
        registerShowingDetails(error())
    }

    private fun DispatcherRegistry.registerOfferCard() {
        register(
            request {
                path("1.0/cardWithViews.json")
                queryParam("id", Showing.OFFER_ID)
            },
            response {
                assetBody("cardWithViews.json")
            }
        )
    }
}
