package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author rogovalex on 09/06/2019.
 */
class ApartmentCategoryTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenAnySelected() {
        selectApartmentCategory(OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenNewSelected() {
        selectApartmentCategory(OfferCategory.PRIMARY)
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenSecondarySelected() {
        selectApartmentCategory(OfferCategory.SECONDARY)
    }

    private fun selectApartmentCategory(offerCategoryFactory: OfferCategoryFactory) {
        val apartmentCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(apartmentCategory.matcher.invoke(lookup))
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                *apartmentCategory.params
            )
        )
    }
}
