package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import com.yandex.mobile.realty.core.robot.performOnEntranceTypeDialog
import org.junit.Test

/**
 * @author scrooge on 01.08.2019.
 */
class EntranceTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellAnyCommercialOffersCountWhenEntranceTypeSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentAnyCommercialOffersCountWhenEntranceTypeSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeSellOfficeOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.OFFICE,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentOfficeOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.RETAIL,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellRetailOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.RETAIL,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentRetailOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.OFFICE,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellFreePurposeOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.FREE_PURPOSE,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentFreePurposeOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.FREE_PURPOSE,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellPublicCateringOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.PUBLIC_CATERING,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentPublicCateringOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.PUBLIC_CATERING,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellHotelOffersCountWhenTypeEntranceSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.HOTEL,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentHotelOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.HOTEL,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellAutoRepairOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.AUTO_REPAIR,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentAutoRepairOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.AUTO_REPAIR,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellWarehouseOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.WAREHOUSE,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentWarehouseOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.WAREHOUSE,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellManufacturingOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.MANUFACTURING,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentManufacturingOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.MANUFACTURING,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeSellBusinessOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.SELL,
            CommercialType.BUSINESS,
            EntranceType.SEPARATE
        )
    }

    @Test
    fun shouldChangeRentBusinessOffersCountWhenEntranceTypeCommonSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.BUSINESS,
            EntranceType.COMMON
        )
    }

    @Test
    fun shouldChangeRentLegalAddressOffersCountWhenEntranceTypeSeparateSet() {
        shouldChangeOffersCountWhenEntranceTypeSet(
            DealType.RENT,
            CommercialType.LEGAL_ADDRESS,
            EntranceType.SEPARATE
        )
    }

    private fun shouldChangeOffersCountWhenEntranceTypeSet(
        dealType: DealType,
        commercialType: CommercialType,
        entranceType: EntranceType

    ) {
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

                scrollToPosition(lookup.matchesFieldEntranceType()).tapOn()

                performOnEntranceTypeDialog {
                    tapOn(entranceType.matcher.invoke(lookup))
                }

                isCommercialEntraceTypeEquals(entranceType.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                entranceType.param
            )
        )
    }

    private fun shouldChangeOffersCountWhenEntranceTypeSet(
        dealType: DealType,
        entranceType: EntranceType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.COMMERCIAL.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldEntranceType()).tapOn()

                performOnEntranceTypeDialog {
                    tapOn(entranceType.matcher.invoke(lookup))
                }

                isCommercialEntraceTypeEquals(entranceType.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                entranceType.param
            )
        )
    }
}
