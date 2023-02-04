package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCeilingHeightDialog
import org.junit.Test

/**
 * @author rogovalex on 09/06/2019.
 */
class CeilingHeightTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenCeilingHeightCentimeters250Selected() {
        selectSellApartmentCeilingHeight(OfferCategory.ANY, CeilingHeight.CENTIMETERS_250)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenCeilingHeightCentimeters250Selected() {
        selectSellApartmentCeilingHeight(OfferCategory.PRIMARY, CeilingHeight.CENTIMETERS_250)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenCeilingHeightCentimeters270Selected() {
        selectRentApartmentCeilingHeight(CeilingHeight.CENTIMETERS_270)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenCeilingHeightCentimeters300Selected() {
        selectRoomCeilingHeight(DealType.SELL, CeilingHeight.CENTIMETERS_300)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenCeilingHeightCentimeters400Selected() {
        selectRoomCeilingHeight(DealType.RENT, CeilingHeight.CENTIMETERS_400)
    }

    private fun selectSellApartmentCeilingHeight(
        offerCategoryFactory: OfferCategoryFactory,
        ceilingHeight: CeilingHeight
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldCeilingHeight())
                    .tapOn()

                performOnCeilingHeightDialog {
                    tapOn(ceilingHeight.matcher.invoke(lookup))
                }
                isCeilingHeightEquals(ceilingHeight.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                ceilingHeight.param,
                *offerCategory.params
            )
        )
    }

    private fun selectRentApartmentCeilingHeight(ceilingHeight: CeilingHeight) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldCeilingHeight())
                    .tapOn()

                performOnCeilingHeightDialog {
                    tapOn(ceilingHeight.matcher.invoke(lookup))
                }
                isCeilingHeightEquals(ceilingHeight.expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.APARTMENT.param,
                ceilingHeight.param
            )
        )
    }

    private fun selectRoomCeilingHeight(dealType: DealType, ceilingHeight: CeilingHeight) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.ROOM.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldCeilingHeight())
                    .tapOn()

                performOnCeilingHeightDialog {
                    tapOn(ceilingHeight.matcher.invoke(lookup))
                }
                isCeilingHeightEquals(ceilingHeight.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.ROOM.param,
                ceilingHeight.param
            )
        )
    }
}
