package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author andrikeev on 22/06/2020.
 */
class WithExcerptReportTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAllOffersCountWhenWithExcerptReportSet() {
        shouldChangeOffersCountWhenWithExcerptReportSet(OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenWithExcerptReportSet() {
        shouldChangeOffersCountWhenWithExcerptReportSet(OfferCategory.SECONDARY)
    }

    private fun shouldChangeOffersCountWhenWithExcerptReportSet(
        offerCategoryFactory: OfferCategoryFactory
    ) {
        val propertyType = PropertyType.APARTMENT
        val offerCategory = offerCategoryFactory.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldWithExcerptReport()).tapOn()

                isChecked(lookup.matchesWithExcerptReportValue())
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                "withExcerptsOnly" to "YES",
                *offerCategory.params
            )
        )
    }
}
