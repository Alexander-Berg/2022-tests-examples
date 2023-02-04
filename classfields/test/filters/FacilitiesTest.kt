package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnFacilitiesDialog
import org.junit.Test

/**
 * @author scrooge on 09.07.2019.
 */
class FacilitiesTest : FilterParamTest() {

    @Test
    fun shouldChangeRentApartmentOffersCountWhenDishwasherSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.APARTMENT,
            Facilities.DISHWASHER
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenRefrigeratorSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.APARTMENT,
            Facilities.REFRIGERATOR
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenAirConditionSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.APARTMENT,
            Facilities.AIR_CONDITION
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenTelevisionSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.APARTMENT,
            Facilities.TELEVISION
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenWashingMachineSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.APARTMENT,
            Facilities.WASHING_MACHINE
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenWithChildrenSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.APARTMENT,
            Facilities.WITH_CHILDREN
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenWithPetsSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.APARTMENT,
            Facilities.WITH_PETS
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenDishwasherSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.ROOM,
            Facilities.DISHWASHER
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenRefrigeratorSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.ROOM,
            Facilities.REFRIGERATOR
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenAirConditionSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.ROOM,
            Facilities.AIR_CONDITION
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenTelevisionSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.ROOM,
            Facilities.TELEVISION
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWashingMachineSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.ROOM,
            Facilities.WASHING_MACHINE
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithChildrenSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.ROOM,
            Facilities.WITH_CHILDREN
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithPetsSelected() {
        shouldChangeOffersCountWhenFacilitiesSet(
            PropertyType.ROOM,
            Facilities.WITH_PETS
        )
    }

    private fun shouldChangeOffersCountWhenFacilitiesSet(
        propertyType: PropertyType,
        facilities: Facilities
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldFacilities())
                    .tapOn()
                performOnFacilitiesDialog {
                    tapOn(facilities.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }
                isFacilitiesEquals(facilities.expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                propertyType.param,
                facilities.param
            )
        )
    }
}
