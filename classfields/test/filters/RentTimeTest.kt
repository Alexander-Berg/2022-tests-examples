package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author rogovalex on 09/06/2019.
 */
class RentTimeTest : FilterParamTest() {

    @Test
    fun shouldChangeRentApartmentOffersCountWhenRentTimeShortSelected() {
        selectRentTime(PropertyType.APARTMENT, RentTime.SHORT)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenRentTimeLongSelected() {
        selectRentTime(PropertyType.APARTMENT, RentTime.LARGE)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenRentTimeShortSelected() {
        selectRentTime(PropertyType.ROOM, RentTime.SHORT)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenRentTimeLongSelected() {
        selectRentTime(PropertyType.ROOM, RentTime.LARGE)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenRentTimeShortSelected() {
        selectRentTime(PropertyType.HOUSE, RentTime.SHORT)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenRentTimeLongSelected() {
        selectRentTime(PropertyType.HOUSE, RentTime.LARGE)
    }

    private fun selectRentTime(propertyType: PropertyType, rentTime: RentTime) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(rentTime.matcher.invoke(lookup))
            },
            params = arrayOf(
                DealType.RENT.param,
                propertyType.param,
                rentTime.param
            )
        )
    }
}
