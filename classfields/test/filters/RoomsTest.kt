package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author rogovalex on 09/06/2019.
 */
class RoomsTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenStudioChecked() {
        selectRoomsCount(DealType.SELL, RoomsTotal.STUDIO)
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenOneRoomChecked() {
        selectRoomsCount(DealType.RENT, RoomsTotal.ONE)
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenTwoRoomChecked() {
        selectRoomsCount(DealType.SELL, RoomsTotal.TWO)
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenThreeRoomChecked() {
        selectRoomsCount(DealType.RENT, RoomsTotal.THREE)
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenFourPlusRoomChecked() {
        selectRoomsCount(DealType.SELL, RoomsTotal.FOUR_PLUS)
    }

    private fun selectRoomsCount(dealType: DealType, roomsTotal: RoomsTotal) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(roomsTotal.matcher.invoke(lookup))
            },
            params = arrayOf(
                dealType.param,
                PropertyType.APARTMENT.param,
                roomsTotal.param
            )
        )
    }
}
