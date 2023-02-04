package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnHouseAreaDialog
import org.junit.Test

/**
 * @author scrooge on 26.06.2019.
 */
class HouseAreaTest : FilterParamTest() {

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenMinHouseAreaSet() {
        selectSellPrimaryHouseArea(houseAreaMin = 100, expected = "от 100 м²")
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenMaxHouseAreaSet() {
        selectSellPrimaryHouseArea(houseAreaMax = 100, expected = "до 100 м²")
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenMinMaxHouseAreaSet() {
        selectSellPrimaryHouseArea(houseAreaMin = 1, houseAreaMax = 100, expected = "1 – 100 м²")
    }

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenMinHouseAreaSet() {
        selectSellNonPrimaryHouseArea(OfferCategory.ANY, areaMin = 27, expected = "от 27 м²")
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenMinMaxHouseAreaSet() {
        selectSellNonPrimaryHouseArea(
            OfferCategory.SECONDARY,
            areaMin = 1,
            areaMax = 100,
            expected = "1 – 100 м²"
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenMinMaxHouseAreaSet() {
        selectRentHouseArea(areaMin = 1, areaMax = 100, expected = "1 – 100 м²")
    }

    private fun selectSellPrimaryHouseArea(
        houseAreaMin: Int? = null,
        houseAreaMax: Int? = null,
        expected: String
    ) {
        val offerCategory = OfferCategory.PRIMARY.invoke(PropertyType.HOUSE)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.HOUSE.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldHouseArea()).tapOn()

                performOnHouseAreaDialog {
                    waitUntilKeyboardAppear()
                    houseAreaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    houseAreaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isHouseAreaEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.HOUSE.param,
                "houseAreaMin" to houseAreaMin?.toString(),
                "houseAreaMax" to houseAreaMax?.toString(),
                *offerCategory.params
            )
        )
    }

    private fun selectSellNonPrimaryHouseArea(
        offerCategoryFactory: OfferCategoryFactory,
        areaMin: Int? = null,
        areaMax: Int? = null,
        expected: String
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.HOUSE)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.HOUSE.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldHouseArea()).tapOn()
                performOnHouseAreaDialog {
                    waitUntilKeyboardAppear()
                    areaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    areaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isHouseAreaEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.HOUSE.param,
                "areaMin" to areaMin?.toString(),
                "areaMax" to areaMax?.toString(),
                *offerCategory.params
            )
        )
    }

    private fun selectRentHouseArea(
        areaMin: Int? = null,
        areaMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.HOUSE.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldHouseArea()).tapOn()
                performOnHouseAreaDialog {
                    waitUntilKeyboardAppear()
                    areaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    areaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isHouseAreaEquals(expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.HOUSE.param,
                "areaMin" to areaMin?.toString(),
                "areaMax" to areaMax?.toString()
            )
        )
    }
}
