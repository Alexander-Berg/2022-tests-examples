package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 26.07.2019.
 */
class OnlySamoletTest : FilterParamTest() {

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenOnlySamoletSet() {
        val offerCategory = OfferCategory.PRIMARY(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldOnlySamolet()).tapOn()

                isChecked(lookup.matchesOnlySamoletValue())
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                *offerCategory.params,
                "developerId" to "102320"
            )
        )
    }
}
