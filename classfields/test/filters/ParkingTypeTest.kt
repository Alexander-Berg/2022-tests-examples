package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnParkingTypeDialog
import org.junit.Test

/**
 * @author scrooge on 03.07.2019.
 */
class ParkingTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenParkingTypeClosedSelected() {
        selectSellApartmentParkingType(OfferCategory.ANY, ParkingType.CLOSED)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenParkingTypeUndergroundSelected() {
        selectSellApartmentParkingType(OfferCategory.PRIMARY, ParkingType.UNDERGROUND)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenParkingTypeUndergroundSelected() {
        selectParkingType(DealType.SELL, PropertyType.ROOM, ParkingType.UNDERGROUND)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenParkingTypeOpenSelected() {
        selectParkingType(DealType.RENT, PropertyType.APARTMENT, ParkingType.OPEN)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenParkingTypeClosedSelected() {
        selectParkingType(DealType.RENT, PropertyType.ROOM, ParkingType.CLOSED)
    }

    private fun selectSellApartmentParkingType(
        offerCategoryFactory: OfferCategoryFactory,
        parkingType: ParkingType
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldParkingType()).tapOn()

                performOnParkingTypeDialog {
                    tapOn(parkingType.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isParkingTypeEquals(parkingType.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                parkingType.param,
                *offerCategory.params
            )
        )
    }

    private fun selectParkingType(
        dealType: DealType,
        propertyType: PropertyType,
        parkingType: ParkingType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldParkingType()).tapOn()

                performOnParkingTypeDialog {
                    tapOn(parkingType.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isParkingTypeEquals(parkingType.expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                parkingType.param
            )
        )
    }
}
