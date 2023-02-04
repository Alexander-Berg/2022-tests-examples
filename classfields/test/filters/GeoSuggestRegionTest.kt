package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.screen.AllSuggestScreen
import com.yandex.mobile.realty.core.screen.GeoIntentScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Test

/**
 * @author shpigun on 2019-08-05
 */
class GeoSuggestRegionTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(PropertyType.APARTMENT, OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellSiteApartmentOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(PropertyType.APARTMENT, OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.SELL, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(PropertyType.HOUSE, OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(PropertyType.HOUSE, OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellSecondaryLotOffersCountWhenGeoSuggestSet() {
        selectGeoSuggestRegion(PropertyType.LOT, OfferCategory.SECONDARY)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenGeoSuggestSet() {
        selectGeoSuggestRegion(PropertyType.LOT, OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.SELL, PropertyType.COMMERCIAL)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.SELL, PropertyType.GARAGE)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.RENT, PropertyType.APARTMENT)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.RENT, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.RENT, PropertyType.HOUSE)
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.RENT, PropertyType.COMMERCIAL)
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenGeoSuggestRegionSet() {
        selectGeoSuggestRegion(DealType.RENT, PropertyType.GARAGE)
    }

    private fun selectGeoSuggestRegion(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            webServerConfiguration = {
                listGeoSuggest()
                regionInfo()
                registerSearchCountOnly(
                    EXPECTED_TOTAL_COUNT,
                    dealType.param,
                    propertyType.param,
                    "rgid" to "417899"
                )
            },
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesGeoSuggestField()).tapOn()

                onScreen<GeoIntentScreen> {
                    searchView.typeText("Saint-Petersburg")

                    onScreen<AllSuggestScreen> {
                        geoSuggestItem("город Санкт-Петербург")
                            .waitUntil { view.isCompletelyDisplayed() }
                            .click()
                    }

                    waitUntil { submitButton.containsText(" $EXPECTED_TOTAL_COUNT ") }
                    pressBack()
                }

                geoSuggestEquals("Город Санкт-Петербург")
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "rgid" to "417899"
            )
        )
    }

    private fun selectGeoSuggestRegion(
        propertyType: PropertyType,
        offerCategoryFactory: OfferCategoryFactory,
        expectedTotalCount: Int = 10
    ) {
        val offerCategory = offerCategoryFactory.invoke(propertyType)
        shouldChangeOffersCount(
            webServerConfiguration = {
                listGeoSuggest()
                regionInfo()
                registerSearchCountOnly(
                    expectedTotalCount,
                    DealType.SELL.param,
                    propertyType.param,
                    *offerCategory.params,
                    "rgid" to "417899"
                )
            },
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesGeoSuggestField()).tapOn()

                onScreen<GeoIntentScreen> {
                    searchView.typeText("Saint-Petersburg")

                    onScreen<AllSuggestScreen> {
                        geoSuggestItem("город Санкт-Петербург")
                            .waitUntil { view.isCompletelyDisplayed() }
                            .click()
                    }

                    waitUntil { submitButton.containsText(" $expectedTotalCount ") }
                    pressBack()
                }

                geoSuggestEquals("Город Санкт-Петербург")
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *offerCategory.params,
                "rgid" to "417899"
            )
        )
    }

    private fun DispatcherRegistry.listGeoSuggest() {
        register(
            request {
                path("1.0/geosuggest.json")
                queryParam("text", "Saint-Petersburg")
            },
            response {
                assetBody("geoSuggestSaintPetersburg.json")
            }
        )
    }

    private fun DispatcherRegistry.regionInfo() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", "417899")
            },
            response {
                assetBody("regionInfo417899.json")
            }
        )
    }
}
