package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialDealStatusDialog
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author scrooge on 01.08.2019.
 */
class RentCommercialDealStatusTest : FilterParamTest() {

    @Test
    fun shouldChangeRentOfficeOffersCountWhenDealStatusDirectRentSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.RETAIL,
            CommercialDealStatus.DIRECT_RENT
        )
    }

    @Test
    fun shouldChangeRentRetailOffersCountWhenDealStatusSubrentSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.OFFICE,
            CommercialDealStatus.SUBRENT
        )
    }

    @Test
    fun shouldChangeRentFreePurposeOffersCountWhenDealStatusSaleOfLeaseRightSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.FREE_PURPOSE,
            CommercialDealStatus.SALE_OF_LEASE_RIGHTS
        )
    }

    @Test
    fun shouldChangeRentPublicCateringOffersCountWhenDealStatusDirectRentSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.PUBLIC_CATERING,
            CommercialDealStatus.DIRECT_RENT
        )
    }

    @Test
    fun shouldChangeRentHotelOffersCountWhenDealStatusSubrentSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.HOTEL,
            CommercialDealStatus.SUBRENT
        )
    }

    @Test
    fun shouldChangeRentAutoRepairOffersCountWhenDealStatusSaleOfLeaseRightSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.AUTO_REPAIR,
            CommercialDealStatus.SALE_OF_LEASE_RIGHTS
        )
    }

    @Test
    fun shouldChangeRentWarehouseOffersCountWhenDealStatusDirectRentSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.WAREHOUSE,
            CommercialDealStatus.DIRECT_RENT
        )
    }

    @Test
    fun shouldChangeRentManufacturingOffersCountWhenDealStatusSubrentSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.MANUFACTURING,
            CommercialDealStatus.SUBRENT
        )
    }

    @Test
    fun shouldChangeRentBusinessOffersCountWhenDealStatusSaleOfLeaseRightSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.BUSINESS,
            CommercialDealStatus.SALE_OF_LEASE_RIGHTS
        )
    }

    @Test
    fun shouldChangeRentLegalAddressOffersCountWhenDealStatusDirectRentSet() {
        shouldChangeOffersCountWhenDealStatusSet(
            CommercialType.LEGAL_ADDRESS,
            CommercialDealStatus.DIRECT_RENT
        )
    }

    private fun shouldChangeOffersCountWhenDealStatusSet(
        commercialType: CommercialType,
        dealStatus: CommercialDealStatus

    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.COMMERCIAL.matcher.invoke(lookup))
                tapOn(lookup.matchesFieldCommercialType())

                performOnCommercialTypeScreen {
                    scrollTo(commercialType.matcher.invoke(lookup))
                        .tapOn()
                    tapOn(lookup.matchesApplyButton())
                }

                scrollToPosition(lookup.matchesFieldOfferDealStatus()).tapOn()

                performOnCommercialDealStatusDialog {
                    tapOn(dealStatus.matcher.invoke(lookup))
                }

                isCommercialOfferDealStatusEquals(dealStatus.expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.COMMERCIAL.param,
                dealStatus.param
            )
        )
    }
}
