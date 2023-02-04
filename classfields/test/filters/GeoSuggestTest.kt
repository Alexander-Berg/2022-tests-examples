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
 * @author shpigun on 2019-07-25
 */
class GeoSuggestTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(PropertyType.APARTMENT, OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellSiteApartmentOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(PropertyType.APARTMENT, OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.SELL, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeSellSecondaryHouseOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(PropertyType.HOUSE, OfferCategory.SECONDARY)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(PropertyType.HOUSE, OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(PropertyType.LOT, OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(PropertyType.LOT, OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.SELL, PropertyType.COMMERCIAL)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.SELL, PropertyType.GARAGE)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.RENT, PropertyType.APARTMENT)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.RENT, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.RENT, PropertyType.HOUSE)
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.RENT, PropertyType.COMMERCIAL)
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenGeoSuggestSet() {
        selectGeoSuggest(DealType.RENT, PropertyType.GARAGE)
    }

    private fun selectGeoSuggest(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            webServerConfiguration = {
                listGeoSuggest()
                registerSearchCountOnly(
                    EXPECTED_TOTAL_COUNT,
                    dealType.param,
                    propertyType.param,
                    "metroGeoId" to "20480",
                    "rgid" to "165705"
                )
            },
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesGeoSuggestField()).tapOn()

                onScreen<GeoIntentScreen> {
                    searchView.typeText("metro")

                    onScreen<AllSuggestScreen> {
                        geoSuggestItem("метро Площадь Революции")
                            .waitUntil { view.isCompletelyDisplayed() }
                            .click()
                    }

                    waitUntil { submitButton.containsText(" $EXPECTED_TOTAL_COUNT ") }
                    pressBack()
                }
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "metroGeoId" to "20480",
                "rgid" to "165705"
            )
        )
    }

    private fun selectGeoSuggest(
        propertyType: PropertyType,
        offerCategoryFactory: OfferCategoryFactory,
        expectedTotalCount: Int = 10
    ) {
        val offerCategory = offerCategoryFactory.invoke(propertyType)
        shouldChangeOffersCount(
            webServerConfiguration = {
                listGeoSuggest()
                registerSearchCountOnly(
                    expectedTotalCount,
                    DealType.SELL.param,
                    propertyType.param,
                    *offerCategory.params,
                    "metroGeoId" to "20480",
                    "rgid" to "165705"
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
                    searchView.typeText("metro")

                    onScreen<AllSuggestScreen> {
                        geoSuggestItem("метро Площадь Революции")
                            .waitUntil { view.isCompletelyDisplayed() }
                            .click()
                    }

                    waitUntil { submitButton.containsText(" $expectedTotalCount ") }
                    pressBack()
                }
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *offerCategory.params,
                "metroGeoId" to "20480",
                "rgid" to "165705"
            )
        )
    }

    private fun DispatcherRegistry.listGeoSuggest(
        offerCategory: OfferCategory? = null
    ) {
        register(
            request {
                path("1.0/geosuggest.json")
                queryParam("text", "metro")
                offerCategory?.geoSuggestParams?.forEach { (name, value) ->
                    queryParam(name, value)
                }
            },
            response {
                assetBody("geoSuggestMetro.json")
            }
        )
    }
}
