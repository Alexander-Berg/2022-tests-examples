package com.yandex.mobile.realty.test.yandexrent.showings

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ShowingsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentShowingsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.APARTMENT_IMAGE_URL
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.showing
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.showingWidget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 29.06.2022
 */
@LargeTest
class ShowingsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ShowingsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MetricaEventsRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowEmptyScreenWithAction() {
        configureWebServer {
            registerShowings(showings = emptyList())
            registerYandexRentSearch()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            event("Аренда. Переход к списку показов")
                .waitUntil { isOccurred() }
            fullscreenEmptyItem
                .waitUntil { isCompletelyDisplayed() }
                .also {
                    root.isViewStateMatches(getTestRelatedFilePath("empty"))
                }
                .invoke { actionButton.click() }
        }
        onScreen<SearchListScreen> {
            offerSnippet("0").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowFullscreenError() {
        configureWebServer {
            registerShowingsError()
            registerShowings(showings = emptyList())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            fullscreenErrorItem
                .waitUntil { isCompletelyDisplayed() }
                .also {
                    root.isViewStateMatches(getTestRelatedFilePath("error"))
                }
                .retryButton
                .click()

            fullscreenEmptyItem
                .waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldShowShowings() {
        configureWebServer {
            registerShowings()
            registerYandexRentSearch()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            showingHeaderItem(FIRST_SHOWING)
                .waitUntil { listView.contains(this) }

            listView.isViewStateMatches(getTestRelatedFilePath("content"))

            otherOffersButton
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<SearchListScreen> {
            offerSnippet("0").waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerShowings() {
        registerShowings(
            showings = listOf(
                showing(
                    showingId = FIRST_SHOWING,
                    imageUrl = APARTMENT_IMAGE_URL,
                    roommates = listOf(ROOMMATE_NAME),
                    rentAmount = RENT_AMOUNT,
                    widget = showingWidget(html = WIDGET_TEXT)
                ),
                showing(
                    showingId = SECOND_SHOWING,
                    imageUrl = APARTMENT_IMAGE_URL,
                    roommates = listOf(ROOMMATE_NAME),
                    rentAmount = RENT_AMOUNT,
                    widget = showingWidget(header = SHOWING_HEADER, html = WIDGET_TEXT)
                ),
            )
        )
    }

    private fun DispatcherRegistry.registerShowingsError() {
        register(
            request {
                path("2.0/rent/user/me/showings")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerYandexRentSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("rentTime", "LARGE")
                queryParam("yandexRent", "YES")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchPage0.json")
            }
        )
    }

    private companion object {

        const val FIRST_SHOWING = "showingId0001"
        const val SECOND_SHOWING = "showingId0002"
        const val SHOWING_HEADER = "Собственник готов сдать квартиру"
        const val WIDGET_TEXT = "Всё идет по плану"
        const val ROOMMATE_NAME = "Михаил"
        const val RENT_AMOUNT = 10_000_000
    }
}
