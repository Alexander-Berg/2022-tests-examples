package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnFloorsDialog
import org.junit.Test

/**
 * @author scrooge on 05.07.2019.
 */
class FloorsTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenFloorsSet() {
        selectSellApartmentFloors(OfferCategory.ANY, floorsMin = 9, expected = "от 9")
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenFloorsSet() {
        selectSellApartmentFloors(
            OfferCategory.PRIMARY,
            floorsMin = 21,
            floorsMax = 99,
            expected = "21 – 99"
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenFloorsSet() {
        selectFloors(DealType.SELL, PropertyType.ROOM, floorsMax = 99, expected = "до 99")
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenFloorsSet() {
        selectFloors(DealType.SELL, PropertyType.HOUSE, floorsMin = 7, expected = "от 7")
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenFloorsSet() {
        selectFloors(DealType.RENT, PropertyType.APARTMENT, floorsMin = 1, expected = "от 1")
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenFloorsSet() {
        selectFloors(DealType.RENT, PropertyType.ROOM, floorsMax = 10, expected = "до 10")
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenFloorsSet() {
        selectFloors(
            DealType.RENT,
            PropertyType.HOUSE,
            floorsMin = 1,
            floorsMax = 5,
            expected = "1 – 5"
        )
    }

    private fun selectSellApartmentFloors(
        offerCategoryFactory: OfferCategoryFactory,
        floorsMin: Int? = null,
        floorsMax: Int? = null,
        expected: String
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldFloors()).tapOn()

                performOnFloorsDialog {
                    waitUntilKeyboardAppear()
                    floorsMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    floorsMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isFloorsEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                "minFloors" to floorsMin?.toString(),
                "maxFloors" to floorsMax?.toString(),
                *offerCategory.params
            )
        )
    }

    private fun selectFloors(
        dealType: DealType,
        propertyType: PropertyType,
        floorsMin: Int? = null,
        floorsMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldFloors()).tapOn()

                performOnFloorsDialog {
                    waitUntilKeyboardAppear()
                    floorsMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    floorsMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isFloorsEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "minFloors" to floorsMin?.toString(),
                "maxFloors" to floorsMax?.toString()
            )
        )
    }
}
