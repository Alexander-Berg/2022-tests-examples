package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnApartmentTypeDialog
import org.junit.Test

/**
 * @author scrooge on 03.07.2019.
 */
class ApartmentTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenOnlyApartmentsSelected() {
        selectApartmentType(OfferCategory.ANY, ApartmentType.YES)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenNonApartmentsSelected() {
        selectApartmentType(OfferCategory.PRIMARY, ApartmentType.NO)
    }

    private fun selectApartmentType(
        offerCategoryFactory: OfferCategoryFactory,
        apartments: ApartmentType
    ) {
        val apartmentCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(apartmentCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldApartmentType())
                    .tapOn()
                performOnApartmentTypeDialog {
                    tapOn(apartments.matcher.invoke(lookup))
                }
                isApartmentTypeEquals(apartments.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                apartments.param,
                *apartmentCategory.params
            )
        )
    }
}
