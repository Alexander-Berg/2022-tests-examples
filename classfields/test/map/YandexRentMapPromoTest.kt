package com.yandex.mobile.realty.test.map

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.metrica.EventMatcher
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.screen.YandexRentMapPromoScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.map.entities.YandexRentPromoEntity
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 12/22/21
 */
@LargeTest
class YandexRentMapPromoTest : BaseTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            filter = Filter.RentApartment(),
            yandexRentMapPromoShowCount = 0,
        ),
        MetricaEventsRule(),
        activityTestRule,
    )

    @Test
    fun shouldShowYandexRentPromoObjectAndOpenOwnerLanding() {
        openPromo(targetStep = 4) {
            openOwnerLandingButton.click()

            event("Промо-визард аренды на карте. Нажатие на кнопку \"Сдать\"")
                .waitUntil { isOccurred() }
        }

        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(OWNER_LANDING_URL) }

            event("Аренда. Открытие лендинга собственника") { "Источник" to "Промо-визард аренды на карте" }
                .waitUntil { isOccurred() }

            pressBack()
        }

        checkPromoDoesNotExist(pinId = REGULAR_OFFER_ENTITY_ID)
    }

    @Test
    fun shouldShowYandexRentPromoObjectAndOpenRentSearch() {
        openPromo(targetStep = 4) {
            openRentSearchButton.click()

            event("Промо-визард аренды на карте. Нажатие на кнопку \"Выбрать свою\"")
                .waitUntil { isOccurred() }
        }

        onScreen<SearchMapScreen> {
            mapView.doesNotContainPlacemark(YandexRentPromoEntity.ID)
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            waitUntil { yandexRentValue.isChecked() }

            pressBack()
        }

        checkPromoDoesNotExist(pinId = YANDEX_RENT_OFFER_ENTITY_ID)
    }

    @Test
    fun shouldShowYandexRentPromoObjectAndOpenReferralPage() {
        openPromo(targetStep = 5) {
            registerResultOkIntent(matchesExternalViewUrlIntent(REFERRAL_URL))

            openReferralPageButton.click()

            event("Промо-визард аренды на карте. Нажатие на кнопку \"Посоветовать\"")
                .waitUntil { isOccurred() }

            intended(matchesExternalViewUrlIntent(REFERRAL_URL))
        }

        checkPromoDoesNotExist(pinId = REGULAR_OFFER_ENTITY_ID)
    }

    @Test
    fun shouldShowYandexRentPromoObjectAndHide() {
        configureWebServer {
            registerMapSearch()
            registerYandexRentAvailable()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            with(mapView) {
                waitUntil { isCompletelyDisplayed() }
                zoomTo(RENT_ZOOM)
                waitUntil { containsPlacemark(YandexRentPromoEntity.ID) }
                clickOnPlacemark(YandexRentPromoEntity.ID)
            }
        }

        onScreen<YandexRentMapPromoScreen> {
            storyView.waitUntil { isLoaded() }

            closeStoryButton.click()
        }

        onScreen<SearchMapScreen> {
            mapView.doesNotContainPlacemark(YandexRentPromoEntity.ID)
        }
    }

    @Test
    fun shouldNotShowYandexRentPromoObject() {
        configureWebServer {
            registerMapSearch()
            registerYandexRentAvailable(available = false)
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            with(mapView) {
                waitUntil { isCompletelyDisplayed() }
                zoomTo(RENT_ZOOM)
                waitUntil { containsPlacemark(DEFAULT_OFFER_ENTITY_ID) }
                doesNotContainPlacemark(YandexRentPromoEntity.ID)
            }
        }
    }

    @Test
    fun shouldOnlyShowPromoObjectWhenRent() {
        configureWebServer {
            registerMapSearch(type = "RENT", responseFileName = "mapSearchDefault.json")
            registerMapSearch(type = "SELL", responseFileName = "mapSearchSell.json")
            registerYandexRentAvailable()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            with(mapView) {
                waitUntil { isCompletelyDisplayed() }
                zoomTo(RENT_ZOOM)
                waitUntil { containsPlacemark(YandexRentPromoEntity.ID) }
            }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            submitButton.click()
        }

        onScreen<SearchMapScreen> {
            with(mapView) {
                waitUntil { isCompletelyDisplayed() }
                zoomTo(RENT_ZOOM)
                waitUntil { containsPlacemark(SELL_OFFER_ENTITY_ID) }
                doesNotContainPlacemark(YandexRentPromoEntity.ID)
            }
        }
    }

    private fun openPromo(targetStep: Int, onPromoScreen: YandexRentMapPromoScreen.() -> Unit) {
        configureWebServer {
            registerMapSearchYandexRent()
            registerMapSearch()
            registerYandexRentAvailable()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            with(mapView) {
                waitUntil { isCompletelyDisplayed() }
                zoomTo(RENT_ZOOM)
                waitUntil { containsPlacemark(YandexRentPromoEntity.ID) }
                clickOnPlacemark(YandexRentPromoEntity.ID)
            }
        }

        onScreen<YandexRentMapPromoScreen> {
            storyView.waitUntil { isLoaded() }

            storyStepEvent(1).waitUntil { isOccurred() }

            for (step in 2..targetStep) {
                storyView.tapOnRightHalf()
                storyStepEvent(step).waitUntil { isOccurred() }
            }

            onPromoScreen.invoke(this)
        }
    }

    private fun checkPromoDoesNotExist(pinId: String) {
        onScreen<SearchMapScreen> {
            waitUntil { mapView.containsPlacemark(pinId) }

            mapView.doesNotContainPlacemark(YandexRentPromoEntity.ID)
        }
    }

    private fun DispatcherRegistry.registerMapSearch(
        type: String = "RENT",
        responseFileName: String = "mapSearchDefault.json"
    ) {
        repeat(2) {
            register(
                request {
                    path("1.0/pointStatisticSearch.json")
                    queryParam("type", type)
                },
                response {
                    assetBody("YandexRentMapPromoTest/$responseFileName")
                },
            )
        }
    }

    private fun DispatcherRegistry.registerMapSearchYandexRent() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
                queryParam("yandexRent", "YES")
            },
            response {
                assetBody("YandexRentMapPromoTest/mapSearchYandexRent.json")
            },
        )
    }

    private fun DispatcherRegistry.registerYandexRentAvailable(
        available: Boolean = true
    ) {
        repeat(2) {
            register(
                request {
                    method("GET")
                    path("2.0/rent/is-point-rent")
                },
                response {
                    jsonBody {
                        "response" to jsonObject {
                            "isPointInsidePolygon" to available
                        }
                    }
                }
            )
        }
    }

    private fun storyStepEvent(step: Int): EventMatcher {
        return event("Промо-визард аренды на карте. Показ визарда") {
            "Шаг" to step
        }
    }

    private companion object {

        const val RENT_ZOOM = 14f
        const val DEFAULT_OFFER_ENTITY_ID = "1"
        const val SELL_OFFER_ENTITY_ID = "100"
        const val REGULAR_OFFER_ENTITY_ID = "1"
        const val YANDEX_RENT_OFFER_ENTITY_ID = "10"

        const val OWNER_LANDING_URL = "https://arenda.test.vertis.yandex.ru/app/owner/?only-content=true"
        const val REFERRAL_URL = "https://forms.yandex.ru/surveys/10031419.b73cfe7311fc0eb1341c316f76b3629f52149d9a"
    }
}
