package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author rogovalex on 09/06/2019.
 */
class CommercialTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellCommercialOffersCountWhenTypeLand() {
        selectCommercialType(DealType.SELL, CommercialType.LAND)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenTypeOffice() {
        selectCommercialType(DealType.RENT, CommercialType.OFFICE)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenTypeRetail() {
        selectCommercialType(DealType.SELL, CommercialType.RETAIL)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenFreePurpose() {
        selectCommercialType(DealType.RENT, CommercialType.FREE_PURPOSE)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenWarehouse() {
        selectCommercialType(DealType.SELL, CommercialType.WAREHOUSE)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenPublicCatering() {
        selectCommercialType(DealType.RENT, CommercialType.PUBLIC_CATERING)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenHotel() {
        selectCommercialType(DealType.SELL, CommercialType.HOTEL)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenAutoRepair() {
        selectCommercialType(DealType.RENT, CommercialType.AUTO_REPAIR)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenManufacturing() {
        selectCommercialType(DealType.SELL, CommercialType.MANUFACTURING)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenBusiness() {
        selectCommercialType(DealType.RENT, CommercialType.BUSINESS)
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenLegalAddress() {
        selectCommercialType(DealType.RENT, CommercialType.LEGAL_ADDRESS)
    }

    private fun selectCommercialType(dealType: DealType, commercialType: CommercialType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.COMMERCIAL.matcher.invoke(lookup))
                tapOn(lookup.matchesFieldCommercialType())
                performOnCommercialTypeScreen {
                    scrollTo(commercialType.matcher.invoke(lookup))
                        .tapOn()
                    tapOn(lookup.matchesApplyButton())
                }
                commercialTypeEquals(commercialType.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                commercialType.param
            )
        )
    }
}
