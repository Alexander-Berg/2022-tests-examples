package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnAreaDialog
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author scrooge on 01.07.2019.
 */
class CommercialAreaTest : FilterParamTest() {

    @Test
    fun shouldChangeSellOfficeOffersCountWhenMinAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.OFFICE,
            areaMin = 27,
            expected = "от 27 м²"
        )
    }

    @Test
    fun shouldChangeSellRetailOffersCountWhenMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.RETAIL,
            areaMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeSellAnyCommercialOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            areaMin = 1,
            areaMax = 50,
            expected = "1 – 50 м²"
        )
    }

    @Test
    fun shouldChangeRentAnyCommercialOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            areaMin = 50,
            areaMax = 100,
            expected = "50 – 100 м²"
        )
    }

    @Test
    fun shouldChangeSellFreePurposeOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.FREE_PURPOSE,
            areaMin = 1,
            areaMax = 100,
            expected = "1 – 100 м²"
        )
    }

    @Test
    fun shouldChangeSellWarehouseOffersCountWhenMinAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.WAREHOUSE,
            areaMin = 27,
            expected = "от 27 м²"
        )
    }

    @Test
    fun shouldChangeSellPublicCateringOffersCountWhenMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.PUBLIC_CATERING,
            areaMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeSellHotelOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.HOTEL,
            areaMin = 1,
            areaMax = 100,
            expected = "1 – 100 м²"
        )
    }

    @Test
    fun shouldChangeSellAutoRepairCountWhenMinAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.AUTO_REPAIR,
            areaMin = 27,
            expected = "от 27 м²"
        )
    }

    @Test
    fun shouldChangeSellManufacturingOffersCountWhenMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.MANUFACTURING,
            areaMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeSellBusinessOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.SELL,
            commercialType = CommercialType.BUSINESS,
            areaMin = 1,
            areaMax = 100,
            expected = "1 – 100 м²"
        )
    }

    @Test
    fun shouldChangeRentOfficeOffersCountWhenMinAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.OFFICE,
            areaMin = 27,
            expected = "от 27 м²"
        )
    }

    @Test
    fun shouldChangeRentRetailOffersCountWhenMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.RETAIL,
            areaMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeRentFreePurposeOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.FREE_PURPOSE,
            areaMin = 1,
            areaMax = 100,
            expected = "1 – 100 м²"
        )
    }

    @Test
    fun shouldChangeRentWarehouseOffersCountWhenMinAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.WAREHOUSE,
            areaMin = 27,
            expected = "от 27 м²"
        )
    }

    @Test
    fun shouldChangeRentPublicCateringOffersCountWhenMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.PUBLIC_CATERING,
            areaMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeRentHotelOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.HOTEL,
            areaMin = 1,
            areaMax = 100,
            expected = "1 – 100 м²"
        )
    }

    @Test
    fun shouldChangeRentAutoRepairCountWhenMinAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.AUTO_REPAIR,
            areaMin = 27,
            expected = "от 27 м²"
        )
    }

    @Test
    fun shouldChangeRentManufacturingOffersCountWhenMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.MANUFACTURING,
            areaMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeRentBusinessOffersCountWhenMinMaxAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.BUSINESS,
            areaMin = 1,
            areaMax = 100,
            expected = "1 – 100 м²"
        )
    }

    @Test
    fun shouldChangeRentLegalAddressCountWhenMinAreaSet() {
        shouldChangeCommercialOffersCountWhenAreaSet(
            dealType = DealType.RENT,
            commercialType = CommercialType.LEGAL_ADDRESS,
            areaMin = 27,
            expected = "от 27 м²"
        )
    }

    private fun shouldChangeCommercialOffersCountWhenAreaSet(
        dealType: DealType,
        commercialType: CommercialType,
        areaMin: Int? = null,
        areaMax: Int? = null,
        expected: String
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

                tapOn(lookup.matchesFieldCommercialArea())
                performOnAreaDialog {
                    waitUntilKeyboardAppear()
                    areaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    areaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }

                isCommercialAreaEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                commercialType.param,
                "areaMin" to areaMin?.toString(),
                "areaMax" to areaMax?.toString()
            )
        )
    }

    private fun shouldChangeCommercialOffersCountWhenAreaSet(
        dealType: DealType,
        areaMin: Int? = null,
        areaMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.COMMERCIAL.matcher.invoke(lookup))
                tapOn(lookup.matchesFieldCommercialArea())
                performOnAreaDialog {
                    waitUntilKeyboardAppear()
                    areaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    areaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }

                isCommercialAreaEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                "areaMin" to areaMin?.toString(),
                "areaMax" to areaMax?.toString()
            )
        )
    }
}
