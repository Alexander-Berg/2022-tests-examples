package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnBuiltYearDialog
import org.junit.Test

/**
 * @author scrooge on 03.07.2019.
 */
class BuiltYearTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenBuiltYearMinSet() {
        shouldChangeOffersCountWhenBuiltYearSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT,
            builtYearMin = 1700,
            expected = "от 1700"
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenBuiltYearMinMaxSet() {
        shouldChangeOffersCountWhenBuiltYearSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT,
            builtYearMin = 2000,
            builtYearMax = 2019,
            expected = "2000 – 2019"
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenBuiltYearMaxSet() {
        shouldChangeOffersCountWhenBuiltYearSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM,
            builtYearMax = 2020,
            expected = "до 2020"
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenBuiltYearMinMaxSet() {
        shouldChangeOffersCountWhenBuiltYearSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM,
            builtYearMin = 2000,
            builtYearMax = 2019,
            expected = "2000 – 2019"
        )
    }

    private fun shouldChangeOffersCountWhenBuiltYearSet(
        dealType: DealType,
        propertyType: PropertyType,
        builtYearMin: Int? = null,
        builtYearMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldBuiltYear()).tapOn()
                performOnBuiltYearDialog {
                    waitUntilKeyboardAppear()
                    builtYearMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    builtYearMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isBuiltYearEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "builtYearMin" to builtYearMin?.toString(),
                "builtYearMax" to builtYearMax?.toString()
            )
        )
    }
}
