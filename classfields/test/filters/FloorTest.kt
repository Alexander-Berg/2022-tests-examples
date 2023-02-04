package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnFloorDialog
import org.junit.Test

/**
 * @author scrooge on 17.06.2019.
 */
class FloorTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenMinFloorSet() {
        selectSellApartmentFloor(OfferCategory.ANY, floorMin = 1, expected = "от 1")
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenMinMaxFloorSet() {
        selectSellApartmentFloor(
            OfferCategory.PRIMARY,
            floorMin = 1,
            floorMax = 99,
            expected = "1 – 99"
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenMaxFloorSet() {
        selectFloor(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT,
            floorMax = 99,
            expected = "до 99"
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenMinMaxFloorSet() {
        selectFloor(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM,
            floorMin = 1,
            floorMax = 99,
            expected = "1 – 99"
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenMinFloorSet() {
        selectFloor(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM,
            floorMin = 1,
            expected = "от 1"
        )
    }

    private fun selectSellApartmentFloor(
        offerCategoryFactory: OfferCategoryFactory,
        floorMin: Int? = null,
        floorMax: Int? = null,
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
                scrollToPosition(lookup.matchesFieldFloor()).tapOn()

                performOnFloorDialog {
                    waitUntilKeyboardAppear()
                    floorMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    floorMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isFloorEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                "floorMin" to floorMin?.toString(),
                "floorMax" to floorMax?.toString(),
                *offerCategory.params
            )
        )
    }

    private fun selectFloor(
        dealType: DealType,
        propertyType: PropertyType,
        floorMin: Int? = null,
        floorMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldFloor()).tapOn()

                performOnFloorDialog {
                    waitUntilKeyboardAppear()
                    floorMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    floorMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isFloorEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "floorMin" to floorMin?.toString(),
                "floorMax" to floorMax?.toString()
            )
        )
    }
}
