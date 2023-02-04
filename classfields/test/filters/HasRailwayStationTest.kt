package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 09.08.2019.
 */
class HasRailwayStationTest : FilterParamTest() {

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenHasRailwayStationSelected() {
        selectHasRailwayStation(PropertyType.HOUSE)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenHasRailwayStationSelected() {
        selectHasRailwayStation(PropertyType.LOT)
    }

    private fun selectHasRailwayStation(propertyType: PropertyType) {
        val offerCategory = OfferCategory.PRIMARY.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldHasRailwayStation()).tapOn()

                isChecked(lookup.matchesHasRailwayStationValue())
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                "hasRailwayStation" to "YES",
                *offerCategory.params
            )
        )
    }
}
