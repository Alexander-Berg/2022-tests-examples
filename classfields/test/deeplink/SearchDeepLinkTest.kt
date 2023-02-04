package com.yandex.mobile.realty.test.deeplink

import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.allure.step
import com.yandex.mobile.realty.core.DeepLinkIntentCommand
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.filters.DealType
import com.yandex.mobile.realty.test.filters.PropertyType
import com.yandex.mobile.realty.test.search.Sorting
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author merionkov on 11/01/2021.
 */
@RunWith(Parameterized::class)
class SearchDeepLinkTest(dealPath: String, propertyPath: String, private val sorting: Sorting) {

    enum class SearchType { OFFER_LIST, SITE_LIST, VILLAGE_LIST }

    private val searchType = when (propertyPath) {
        "novostrojka" -> SearchType.SITE_LIST
        "kottedzhnye-poselki" -> SearchType.VILLAGE_LIST
        else -> SearchType.OFFER_LIST
    }

    private val dealType = when (dealPath) {
        "kupit" -> DealType.SELL
        "snyat" -> DealType.RENT
        else -> throw IllegalArgumentException("Unknown deal type value")
    }

    private val propertyType = when (propertyPath) {
        "novostrojka", "kvartira" -> PropertyType.APARTMENT
        "komnata" -> PropertyType.ROOM
        "kottedzhnye-poselki", "dom" -> PropertyType.HOUSE
        "uchastok" -> PropertyType.LOT
        "kommercheskaya-nedvizhimost" -> PropertyType.COMMERCIAL
        "garazh" -> PropertyType.GARAGE
        else -> throw IllegalArgumentException("Unknown property type value")
    }

    private val deepLink = "https://realty.yandex.ru" +
        "/sankt-peterburg_i_leningradskaya_oblast" +
        "/$dealPath" +
        "/$propertyPath" +
        "/?sort=${sorting.value}"

    private val activityRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain = baseChainOf(activityRule)

    @Test
    fun shouldOpenSearchDeepLink() {
        configureWebServer {
            registerRegionInfoSPB()
            registerSearchDeepLink()
            registerSearchList()
            registerSearchList()
        }
        DeepLinkIntentCommand.execute(deepLink)
        verifySearchScreen()
        step("Закрываем приложение") {
            activityRule.finishActivity()
        }
        step("Открываем приложение") {
            activityRule.launchActivity()
        }
        verifySearchScreen()
    }

    private fun verifySearchScreen() {
        onScreen<SearchListScreen> {
            sortingItem
                .waitUntil { listView.contains(this) }
                .invoke { labelView.isTextEquals(sorting.expected) }
            filterButton.click()
        }
        onScreen<FiltersScreen> {
            when (dealType) {
                DealType.SELL -> isBuySelected()
                DealType.RENT -> isRentSelected()
            }
            when (propertyType) {
                PropertyType.APARTMENT -> isApartmentSelected()
                PropertyType.ROOM -> isRoomSelected()
                PropertyType.HOUSE -> isHouseSelected()
                PropertyType.LOT -> isAreaSelected()
                PropertyType.COMMERCIAL -> isCommercialSelected()
                PropertyType.GARAGE -> isGarageSelected()
            }
            when (searchType) {
                SearchType.SITE_LIST -> isSiteSelected()
                SearchType.VILLAGE_LIST -> isVillageSelected()
                else -> Unit
            }
            listView.scrollTo(geoSuggestField)
            geoSuggestValue.isTextEquals("Санкт-Петербург и ЛО")
        }
    }

    private fun DispatcherRegistry.registerSearchDeepLink() {
        register(
            request {
                path("1.0/deeplink.json")
                jsonBody { "url" to deepLink }
            },
            response {
                val propertyTypeValue = when (propertyType) {
                    PropertyType.APARTMENT -> "APARTMENT"
                    PropertyType.ROOM -> "ROOMS"
                    PropertyType.HOUSE -> "HOUSE"
                    PropertyType.LOT -> "LOT"
                    PropertyType.COMMERCIAL -> "COMMERCIAL"
                    PropertyType.GARAGE -> "GARAGE"
                }
                setBody(
                    """
                            {
                                "response": {
                                    "action": "$searchType",
                                    "region": {
                                        "rgid": 741965,
                                        "fullName": "Санкт-Петербург и ЛО",
                                        "shortName": "Санкт-Петербург и ЛО",
                                        "point": {
                                            "latitude": 59.938953,
                                            "longitude": 30.31564,
                                            "defined": true
                                        },
                                        "lt": {
                                            "latitude": 61.329765,
                                            "longitude": 26.580238,
                                            "defined": true
                                        },
                                        "rb": {
                                            "latitude": 58.417088,
                                            "longitude": 35.72142,
                                            "defined": true
                                        },
                                        "searchParams": { "rgid": ["741965"] }
                                    },
                                    "params": [
                                        {
                                            "name": "sort",
                                            "values": ["${sorting.value}"]
                                        },
                                        {
                                            "name": "category",
                                            "values": ["$propertyTypeValue"]
                                        },
                                        {
                                            "name": "type",
                                            "values": ["${dealType.value}"]
                                        }
                                    ]
                                }
                            }
                            """
                )
            }
        )
    }

    private fun DispatcherRegistry.registerRegionInfoSPB() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
            },
            response {
                assetBody("regionInfo417899.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSearchList() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("sort", sorting.value)
            },
            response {
                assetBody("offerWithSiteSearchSelectedSorting.json")
            }
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1} sorted by {2}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf("kupit", "novostrojka", Sorting.PRICE),
                arrayOf("kupit", "kottedzhnye-poselki", Sorting.PRICE_DESC),
                arrayOf("kupit", "uchastok", Sorting.LOT_AREA),
                arrayOf("kupit", "kvartira", Sorting.RELEVANCE),
                arrayOf("snyat", "kvartira", Sorting.CONFIDENCE),
                arrayOf("kupit", "komnata", Sorting.FLOOR),
                arrayOf("snyat", "komnata", Sorting.FLOOR_DESC),
                arrayOf("kupit", "dom", Sorting.LOT_AREA),
                arrayOf("snyat", "dom", Sorting.LOT_AREA_DESC),
                arrayOf("kupit", "garazh", Sorting.AREA),
                arrayOf("snyat", "garazh", Sorting.AREA_DESC),
                arrayOf("kupit", "kommercheskaya-nedvizhimost", Sorting.PRICE_PER_SQUARE),
                arrayOf("snyat", "kommercheskaya-nedvizhimost", Sorting.PRICE_PER_SQUARE_DESC),
            )
        }
    }
}
