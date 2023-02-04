package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnAreaDialog
import org.junit.Test

/**
 * @author scrooge on 10.06.2019.
 */
class FlatAreaTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenMinFlatAreaSet() {
        selectSellApartmentFlatArea(OfferCategory.ANY, areaMin = 27, expected = "от 27 м²")
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenMaxFlatAreaSet() {
        selectSellApartmentFlatArea(OfferCategory.SECONDARY, areaMax = 100, expected = "до 100 м²")
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenMinFlatAreaSet() {
        selectSellApartmentFlatArea(OfferCategory.PRIMARY, areaMin = 99, expected = "от 99 м²")
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenMinMaxFlatAreaSet() {
        selectRentApartmentFlatArea(areaMin = 1, areaMax = 100, expected = "1 – 100 м²")
    }

    private fun selectSellApartmentFlatArea(
        offerCategoryFactory: OfferCategoryFactory,
        areaMin: Int? = null,
        areaMax: Int? = null,
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
                scrollToPosition(lookup.matchesFieldFlatArea()).tapOn()
                performOnAreaDialog {
                    waitUntilKeyboardAppear()
                    areaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    areaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isFlatAreaEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                "areaMin" to areaMin?.toString(),
                "areaMax" to areaMax?.toString(),
                *offerCategory.params
            )
        )
    }

    private fun selectRentApartmentFlatArea(
        areaMin: Int? = null,
        areaMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldFlatArea()).tapOn()
                performOnAreaDialog {
                    waitUntilKeyboardAppear()
                    areaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    areaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isFlatAreaEquals(expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.APARTMENT.param,
                "areaMin" to areaMin?.toString(),
                "areaMax" to areaMax?.toString()
            )
        )
    }
}
