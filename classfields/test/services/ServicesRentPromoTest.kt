package com.yandex.mobile.realty.test.services

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 17.08.2021
 */
@LargeTest
class ServicesRentPromoTest {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowRentPromoAndOpenLanding() {
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentPromoItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesRentPromoTest/unauthorizedUserPromo")

            rentPromoRentOutButton.click()
        }

        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(OWNER_LANDING_URL) }
        }
    }

    @Test
    fun shouldShowRentPromoAndOpenLk() {
        configureWebServer {
            registerTenantServicesInfo()
            registerEmptyRentFlats()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(RENT_LK_URL), null)

        onScreen<ServicesScreen> {
            rentPromoItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesRentPromoTest/rentUserPromo")

            rentPromoLkButton.click()
        }

        intended(matchesExternalViewUrlIntent(RENT_LK_URL))
    }

    @Test
    fun shouldShowRentPromoAndStartSearch() {
        configureWebServer {
            registerYandexRentSearch()
        }

        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentPromoItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesRentPromoTest/unauthorizedUserPromo")

            rentPromoRentButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet("0").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowRentPromoWithOffersCount() {
        configureWebServer {
            registerOffersCount()
        }

        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentPromoItem
                .waitUntil {
                    listView.contains(this)
                    rentPromoRentButton.isTextEquals("Смотреть 204 предложения")
                }
                .isViewStateMatches("ServicesRentPromoTest/promoWithOffersCount")
        }
    }

    private fun DispatcherRegistry.registerEmptyRentFlats() {
        register(
            request {
                path("2.0/rent/user/me/flats")
            },
            response {
                setBody("{\"response\": {\"flats\": []}}")
            }
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

    private fun DispatcherRegistry.registerOffersCount() {
        register(
            request {
                path("2.0/offers/number")
                queryParam("rgid", "587795")
                queryParam("category", "APARTMENT")
                queryParam("type", "RENT")
                queryParam("rentTime", "LARGE")
                queryParam("yandexRent", "YES")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "number" to 204
                    }
                }
            }
        )
    }

    private companion object {

        const val OWNER_LANDING_URL =
            "https://arenda.test.vertis.yandex.ru/app/owner/?only-content=true"
        const val RENT_LK_URL = "https://arenda.test.vertis.yandex.ru/lk"
    }
}
