package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 26.07.2019.
 */
class ShowOutdatedTest : FilterParamTest() {

    @Test
    fun shouldChangeSiteOffersCountWhenShowOutdatedSet() {
        val apartmentCategory = OfferCategory.PRIMARY(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(apartmentCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldShowOutdated()).tapOn()

                isChecked(lookup.matchesShowOutdatedValue())
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                *apartmentCategory.params,
                "showOutdated" to "YES"
            )
        )
    }
}
