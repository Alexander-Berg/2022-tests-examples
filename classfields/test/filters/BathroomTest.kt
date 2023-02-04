package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnBathroomDialog
import org.junit.Test

/**
 * @author scrooge on 03.07.2019.
 */
class BathroomTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenBathroomMatchedSelected() {
        selectApartmentBathroom(OfferCategory.ANY, Bathroom.MATCHED)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenBathroomSeparatedSelected() {
        selectApartmentBathroom(OfferCategory.PRIMARY, Bathroom.SEPARATED)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenBathroomTwoAndMoreSelected() {
        selectRoomBathroom(Bathroom.TWO_AND_MORE)
    }

    private fun selectRoomBathroom(bathroomUnit: Bathroom) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.ROOM.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldBathroom())
                    .tapOn()

                performOnBathroomDialog {
                    tapOn(bathroomUnit.matcher.invoke(lookup))
                }
                isBathroomEquals(bathroomUnit.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.ROOM.param,
                bathroomUnit.param
            )
        )
    }

    private fun selectApartmentBathroom(
        offerCategoryFactory: OfferCategoryFactory,
        bathroomUnit: Bathroom
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldBathroom())
                    .tapOn()

                performOnBathroomDialog {
                    tapOn(bathroomUnit.matcher.invoke(lookup))
                }
                isBathroomEquals(bathroomUnit.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                bathroomUnit.param,
                *offerCategory.params
            )
        )
    }
}
