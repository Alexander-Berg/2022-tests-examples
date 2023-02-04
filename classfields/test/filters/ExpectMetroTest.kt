package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 27.06.2019.
 */
class ExpectMetroTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenExpectMetroSet() {
        selectApartmentExpectMetro(OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenExpectMetroSet() {
        selectApartmentExpectMetro(OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenExpectMetroSet() {
        selectRoomHouseExpectMetro(DealType.SELL, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenExpectMetroSet() {
        selectRoomHouseExpectMetro(DealType.SELL, PropertyType.HOUSE)
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenExpectMetroSet() {
        selectRoomHouseExpectMetro(DealType.SELL, PropertyType.LOT)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenExpectMetroSet() {
        selectRoomHouseExpectMetro(DealType.RENT, PropertyType.APARTMENT)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenExpectMetroSet() {
        selectRoomHouseExpectMetro(DealType.RENT, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenExpectMetroSet() {
        selectRoomHouseExpectMetro(DealType.RENT, PropertyType.HOUSE)
    }

    private fun selectRoomHouseExpectMetro(dealType: DealType, propertyType: PropertyType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldExpectMetro()).tapOn()

                isChecked(lookup.matchesExpectMetroValue())
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "expectMetro" to "YES"
            )
        )
    }

    private fun selectApartmentExpectMetro(offerCategoryFactory: OfferCategoryFactory) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldExpectMetro()).tapOn()

                isChecked(lookup.matchesExpectMetroValue())
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                "expectMetro" to "YES",
                *offerCategory.params
            )
        )
    }
}
