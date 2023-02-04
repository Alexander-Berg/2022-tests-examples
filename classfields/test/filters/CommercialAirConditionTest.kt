package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author scrooge on 31.07.2019.
 */
class CommercialAirConditionTest : FilterParamTest() {

    @Test
    fun shouldChangeRentCommercialOffersCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.RENT,
            CommercialType.OFFICE
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.SELL,
            CommercialType.OFFICE
        )
    }

    @Test
    fun shouldChangeRentCommercialRetailCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.RENT,
            CommercialType.RETAIL
        )
    }

    @Test
    fun shouldChangeSellCommercialRetailCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.SELL,
            CommercialType.RETAIL
        )
    }

    @Test
    fun shouldChangeRentCommercialFreePurposeCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.RENT,
            CommercialType.FREE_PURPOSE
        )
    }

    @Test
    fun shouldChangeSellCommercialFreePurposeCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.SELL,
            CommercialType.FREE_PURPOSE
        )
    }

    @Test
    fun shouldChangeRentCommercialWarehouseCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.RENT,
            CommercialType.WAREHOUSE
        )
    }

    @Test
    fun shouldChangeSellCommercialWarehouseCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.SELL,
            CommercialType.WAREHOUSE
        )
    }

    @Test
    fun shouldChangeRentCommercialManufacturingCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.RENT,
            CommercialType.MANUFACTURING
        )
    }

    @Test
    fun shouldChangeSellCommercialManufacturingCountWhenHasAirConditionSet() {
        shouldChangeOffersCountWhenHasAirConditionSet(
            DealType.SELL,
            CommercialType.MANUFACTURING
        )
    }

    private fun shouldChangeOffersCountWhenHasAirConditionSet(
        dealType: DealType,
        commercialType: CommercialType
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

                scrollToPosition(lookup.matchesFieldHasAirCondition()).tapOn()

                isChecked(lookup.matchesHasAirConditionValue())
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                "hasAircondition" to "YES"
            )
        )
    }
}
