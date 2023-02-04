package com.yandex.mobile.realty.test.favorite

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.screen.Screen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TOfferSnippetView
import com.yandex.mobile.realty.core.view.TSiteSnippetView
import com.yandex.mobile.realty.core.view.TView
import com.yandex.mobile.realty.core.view.TVillageSnippetView
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.ExpectedRequest
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.appendWithDelimiter
import com.yandex.mobile.realty.utils.jsonObject
import com.yandex.mobile.realty.utils.toJsonArray

/**
 * @author shpigun on 19.04.2021
 */
abstract class FavoriteButtonTest {

    protected inline fun <reified T : Screen<T>> testOfferSnippetFavoriteButton(
        offerId: String,
        webServerConfiguration: DispatcherRegistry.() -> Unit = {},
        actionConfiguration: () -> Unit,
        noinline snippetViewSelector: T.() -> TOfferSnippetView,
        offerCategories: ArrayList<String>,
        metricaSource: String
    ) {
        testOfferFavoriteButton<T>(
            offerId = offerId,
            webServerConfiguration = webServerConfiguration,
            actionConfiguration = actionConfiguration,
            buttonViewSelector = { snippetViewSelector.invoke(this).favoriteButton },
            favAddedScreenshot = "FavoriteButtonTest/testOfferSnippetFavoriteButton/added",
            favRemovedScreenshot = "FavoriteButtonTest/testOfferSnippetFavoriteButton/removed",
            offerCategories = offerCategories.toJsonArray(),
            metricaSource = jsonObject {
                "сниппет объявления" to metricaSource
            }
        )
    }

    protected inline fun <reified T : Screen<T>> testSiteSnippetFavoriteButton(
        siteId: String,
        webServerConfiguration: DispatcherRegistry.() -> Unit = {},
        actionConfiguration: () -> Unit,
        noinline snippetViewSelector: T.() -> TSiteSnippetView,
        siteCategories: ArrayList<String>,
        metricaSource: String
    ) {
        testSiteFavoriteButton<T>(
            siteId = siteId,
            webServerConfiguration = webServerConfiguration,
            actionConfiguration = actionConfiguration,
            buttonViewSelector = { snippetViewSelector.invoke(this).favoriteButton },
            favAddedScreenshot = "FavoriteButtonTest/testSiteSnippetFavoriteButton/added",
            favRemovedScreenshot = "FavoriteButtonTest/testSiteSnippetFavoriteButton/removed",
            siteCategories = siteCategories.toJsonArray(),
            metricaSource = jsonObject {
                "сниппет новостройки" to metricaSource
            }
        )
    }

    protected inline fun <reified T : Screen<T>> testVillageSnippetFavoriteButton(
        villageId: String,
        webServerConfiguration: DispatcherRegistry.() -> Unit = {},
        actionConfiguration: () -> Unit,
        noinline snippetViewSelector: T.() -> TVillageSnippetView,
        villageCategories: ArrayList<String>,
        metricaSource: String
    ) {
        testVillageFavoriteButton<T>(
            villageId = villageId,
            webServerConfiguration = webServerConfiguration,
            actionConfiguration = actionConfiguration,
            buttonViewSelector = { snippetViewSelector.invoke(this).favoriteButton },
            favAddedScreenshot = "FavoriteButtonTest/testVillageSnippetFavoriteButton/added",
            favRemovedScreenshot = "FavoriteButtonTest/testVillageSnippetFavoriteButton/removed",
            villageCategories = villageCategories.toJsonArray(),
            metricaSource = jsonObject {
                "сниппет КП" to metricaSource
            }
        )
    }

    protected inline fun <reified T : Screen<T>> testOfferFavoriteButton(
        offerId: String,
        webServerConfiguration: DispatcherRegistry.() -> Unit,
        actionConfiguration: () -> Unit,
        noinline buttonViewSelector: T.() -> TView,
        favAddedScreenshot: String,
        favRemovedScreenshot: String,
        offerCategories: JsonArray,
        metricaSource: JsonElement
    ) {
        val dispatcher = DispatcherRegistry()
        val expectedAddFavoritesRequest = dispatcher.registerOfferFavoritesPatch(addedId = offerId)
        val expectedRemoveFavoritesRequest = dispatcher.registerOfferFavoritesPatch(
            removedId = offerId
        )
        dispatcher.apply { webServerConfiguration.invoke(this) }
        configureWebServer(dispatcher)
        actionConfiguration.invoke()
        val addFavMetricaEvent = event("Добавление в избранное") {
            "Категория объявления" to offerCategories
            "Источник" to metricaSource
            "id" to offerId
        }
        onScreen<T> {
            val favoriteButton = buttonViewSelector.invoke(this)
            favoriteButton.click()
            waitUntil { addFavMetricaEvent.isOccurred() }
            waitUntil { expectedAddFavoritesRequest.isOccured() }
            snackbarView(getResourceString(R.string.add_favor_explanation))
                .waitUntil { isCompletelyDisplayed() }
            favoriteButton.isViewStateMatches(favAddedScreenshot)
            favoriteButton.click()
            waitUntil { expectedRemoveFavoritesRequest.isOccured() }
            favoriteButton.isViewStateMatches(favRemovedScreenshot)
        }
    }

    protected inline fun <reified T : Screen<T>> testSiteFavoriteButton(
        siteId: String,
        webServerConfiguration: DispatcherRegistry.() -> Unit,
        actionConfiguration: () -> Unit,
        noinline buttonViewSelector: T.() -> TView,
        favAddedScreenshot: String,
        favRemovedScreenshot: String,
        siteCategories: JsonArray,
        metricaSource: JsonElement
    ) {
        val dispatcher = DispatcherRegistry()
        val expectedAddFavoritesRequest = dispatcher.registerSiteFavoritesPatch(addedId = siteId)
        val expectedRemoveFavoritesRequest =
            dispatcher.registerSiteFavoritesPatch(removedId = siteId)
        dispatcher.apply { webServerConfiguration.invoke(this) }
        configureWebServer(dispatcher)
        actionConfiguration.invoke()
        val addFavMetricaEvent = event("Добавление в избранное") {
            "Категория объявления" to siteCategories
            "Источник" to metricaSource
            "id" to "site_$siteId"
        }
        onScreen<T> {
            val favoriteButton = buttonViewSelector.invoke(this)
            favoriteButton.click()
            waitUntil { addFavMetricaEvent.isOccurred() }
            waitUntil { expectedAddFavoritesRequest.isOccured() }
            favoriteButton.isViewStateMatches(favAddedScreenshot)
            favoriteButton.click()
            waitUntil { expectedRemoveFavoritesRequest.isOccured() }
            favoriteButton.isViewStateMatches(favRemovedScreenshot)
        }
    }

    protected inline fun <reified T : Screen<T>> testVillageFavoriteButton(
        villageId: String,
        webServerConfiguration: DispatcherRegistry.() -> Unit,
        actionConfiguration: () -> Unit,
        noinline buttonViewSelector: T.() -> TView,
        favAddedScreenshot: String,
        favRemovedScreenshot: String,
        villageCategories: JsonArray,
        metricaSource: JsonElement
    ) {
        val dispatcher = DispatcherRegistry()
        val expectedAddFavoritesRequest =
            dispatcher.registerVillageFavoritesPatch(addedId = villageId)
        val expectedRemoveFavoritesRequest = dispatcher.registerVillageFavoritesPatch(
            removedId = villageId
        )
        dispatcher.apply { webServerConfiguration.invoke(this) }
        configureWebServer(dispatcher)
        actionConfiguration.invoke()
        val addFavMetricaEvent = event("Добавление в избранное") {
            "Категория объявления" to villageCategories
            "Источник" to metricaSource
            "id" to "village_$villageId"
        }
        onScreen<T> {
            val favoriteButton = buttonViewSelector.invoke(this)
            favoriteButton.click()
            waitUntil { addFavMetricaEvent.isOccurred() }
            waitUntil { expectedAddFavoritesRequest.isOccured() }
            favoriteButton.isViewStateMatches(favAddedScreenshot)
            favoriteButton.click()
            waitUntil { expectedRemoveFavoritesRequest.isOccured() }
            favoriteButton.isViewStateMatches(favRemovedScreenshot)
        }
    }

    protected fun DispatcherRegistry.registerOfferFavoritesPatch(
        addedId: String? = null,
        removedId: String? = null
    ): ExpectedRequest {
        return registerFavoritesPatch(addedId, removedId)
    }

    protected fun DispatcherRegistry.registerSiteFavoritesPatch(
        addedId: String? = null,
        removedId: String? = null
    ): ExpectedRequest {
        return registerFavoritesPatch(addedId?.let { "site_$it" }, removedId?.let { "site_$it" })
    }

    protected fun DispatcherRegistry.registerVillageFavoritesPatch(
        addedId: String? = null,
        removedId: String? = null
    ): ExpectedRequest {
        return registerFavoritesPatch(
            addedId?.let { "village_$it" },
            removedId?.let { "village_$it" }
        )
    }

    private fun DispatcherRegistry.registerFavoritesPatch(
        addedId: String?,
        removedId: String?
    ): ExpectedRequest {
        return register(
            request {
                method("PATCH")
                path("1.0/favorites.json")
                val body = buildString {
                    addedId?.let { id ->
                        append("\"add\": [\"")
                        append(id)
                        append("\"]")
                    }

                    removedId?.let { id ->
                        appendWithDelimiter("\"remove\": [\"", ", ")
                        append(id)
                        append("\"]")
                    }
                }
                body("{$body}")
            },
            response {
                setBody(
                    buildString {
                        append("{\"response\": {")
                        addedId?.let { id ->
                            append("\"relevant\": [\"")
                            append(id)
                            append("\"]")
                        }
                        append("]}}")
                    }
                )
            }

        )
    }

    companion object {

        const val TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_ADDED = "FavoriteButtonTest/" +
            "testTransparentToolbarFavoriteButton/added"
        const val TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_REMOVED_WITH_SHADOW =
            "FavoriteButtonTest/testTransparentToolbarFavoriteButton/removedWithShadow"
        const val TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_REMOVED_WITHOUT_SHADOW =
            "FavoriteButtonTest/testTransparentToolbarFavoriteButton/removedWithoutShadow"
        const val WHITE_TOOLBAR_FAVORITE_BUTTON_ADDED = "FavoriteButtonTest/" +
            "testWhiteToolbarFavoriteButton/added"
        const val WHITE_TOOLBAR_FAVORITE_BUTTON_REMOVED = "FavoriteButtonTest/" +
            "testWhiteToolbarFavoriteButton/removed"
    }
}
