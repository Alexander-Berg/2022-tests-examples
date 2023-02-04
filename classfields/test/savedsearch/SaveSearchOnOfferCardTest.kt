package com.yandex.mobile.realty.test.savedsearch

import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.EventMatcher
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FavoriteScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.LastFloorDialogScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.PriceDialogScreen
import com.yandex.mobile.realty.core.screen.RenovationTypeScreen
import com.yandex.mobile.realty.core.screen.SaveSearchResultScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchFiltersListDialogScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.ExpectedRequest
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.filters.LastFloor
import com.yandex.mobile.realty.test.filters.RenovationType
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author merionkov on 09.06.2022.
 */
class SaveSearchOnOfferCardTest : BaseTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        MetricaEventsRule(),
        activityTestRule,
    )

    @Test
    fun shouldSaveSearchFromOfferCard() {
        val dispatcher = DispatcherRegistry()
        repeat(7) { dispatcher.registerOfferSearch() }
        dispatcher.registerOffer()
        val expectedSaveRequest = dispatcher.registerSaveSearch()
        val expectedSubscribeRequest = dispatcher.registerSavedSearchSubscription()
        configureWebServer(dispatcher)
        activityTestRule.launchActivity()
        onScreen<SearchListScreen> {
            filterButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorSecondary.click()
            roomsCountSelectorOne.click()
            roomsCountSelectorTwo.click()
            priceValue.click()
        }
        onScreen<PriceDialogScreen> {
            valueFrom.replaceText(1_000_000.toString())
            valueTo.replaceText(5_000_000.toString())
            okButton.click()
        }
        onScreen<FiltersScreen> {
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<OfferCardScreen> {
            saveSearchItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("saveSearchBlock"))
            saveSearchButton.click()
            saveSearchEvent().waitUntil { isOccurred() }
        }
        onScreen<SaveSearchResultScreen> {
            proceedToSavedSearchesButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<FavoriteScreen> {
            subscriptionsTabView.isSelected()
            expectedSaveRequest.waitUntil { isOccured() }
            expectedSubscribeRequest.waitUntil { isOccured() }
        }
    }

    @Test
    fun shouldSaveSearchFromFiltersDialogOnOfferCard() {
        val dispatcher = DispatcherRegistry()
        repeat(9) { dispatcher.registerOfferSearch() }
        dispatcher.registerOffer()
        val expectedSaveRequest = dispatcher.registerSaveSearch()
        val expectedSubscribeRequest = dispatcher.registerSavedSearchSubscription()
        configureWebServer(dispatcher)
        activityTestRule.launchActivity()
        onScreen<SearchListScreen> {
            filterButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorSecondary.click()
            roomsCountSelectorOne.click()
            roomsCountSelectorTwo.click()
            priceValue.click()
        }
        onScreen<PriceDialogScreen> {
            valueFrom.replaceText(1_000_000.toString())
            valueTo.replaceText(5_000_000.toString())
            okButton.click()
        }
        onScreen<FiltersScreen> {
            listView.scrollTo(renovationTypeField).click()
        }
        onScreen<RenovationTypeScreen> {
            renovationTypeView(RenovationType.COSMETIC_DONE).click()
            positiveButton.click()
        }
        onScreen<FiltersScreen> {
            listView.scrollTo(lastFloorItem).click()
        }
        onScreen<LastFloorDialogScreen> {
            listView.scrollTo(LastFloor.EXCEPT_LAST_FLOOR.matcher).click()
        }
        onScreen<FiltersScreen> {
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet("0")
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<OfferCardScreen> {
            saveSearchItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("saveSearchBlock"))
            showFiltersButton.click()
        }
        onScreen<SearchFiltersListDialogScreen> {
            actionButton.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("filtersDialog"))
            actionButton.click()
            saveSearchEvent().waitUntil { isOccurred() }
        }
        onScreen<SaveSearchResultScreen> {
            proceedToSavedSearchesButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<FavoriteScreen> {
            subscriptionsTabView.isSelected()
            expectedSaveRequest.waitUntil { isOccured() }
            expectedSubscribeRequest.waitUntil { isOccured() }
        }
    }

    private fun saveSearchEvent(): EventMatcher {
        return event("Сохранить поиск") {
            "гео" to "по области экрана"
            "Категория поиска" to jsonArrayOf(
                "Sell",
                "Flat_Sell, Room_Sell",
                "Flat",
                "Flat_Sell",
                "SecondaryFlat_Sell",
            )
            "Источник" to "Карточка офера"
            "id" to "saved-search-1"
        }
    }

    private fun DispatcherRegistry.registerOfferSearch(): ExpectedRequest {
        return register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            },
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("cardWithViews.json")
            },
        )
    }

    private fun DispatcherRegistry.registerSaveSearch(): ExpectedRequest {
        return register(
            request {
                method("PUT")
                path("2.0/savedSearch/saved-search-1")
            },
            response {
                assetBody("savedSearchesTest/saveSearchOnList.json")
            },
        )
    }

    private fun DispatcherRegistry.registerSavedSearchSubscription(): ExpectedRequest {
        return register(
            request {
                method("PUT")
                path("2.0/savedSearch/saved-search-1/subscription/push")
            },
            response {
                assetBody("savedSearchesTest/subscribeSavedSearchOnList.json")
            },
        )
    }
}
