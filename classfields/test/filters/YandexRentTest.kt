package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author andrey-bgm on 05/02/2021.
 */
class YandexRentTest : FilterParamTest() {

    @Test
    fun shouldChangeRentApartmentOffersCountWhenYandexRentSet() {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(lookup.matchesRentTimeSelectorLong())
                scrollToPosition(lookup.matchesYandexRentField())
                tapOn(lookup.matchesYandexRentField())

                isChecked(lookup.matchesYandexRentValue())
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.APARTMENT.param,
                RentTime.LARGE.param,
                "yandexRent" to "YES"
            )
        )
    }
}
