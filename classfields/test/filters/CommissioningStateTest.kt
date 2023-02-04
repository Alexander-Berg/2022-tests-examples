package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommissioningStateDialog
import org.junit.Test

/**
 * @author scrooge on 29.08.2019.
 */
class CommissioningStateTest : FilterParamTest() {

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenCommissioningStateFinishedSelected() {
        selectCommissioningStateFinished(PropertyType.APARTMENT, CommissioningState.FINISHED)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenCommissioningStateFinishedSelected() {
        selectCommissioningStateFinished(PropertyType.HOUSE, CommissioningState.FINISHED)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenCommissioningStateBeingBuiltSelected() {
        selectCommissioningStateFinished(PropertyType.APARTMENT, CommissioningState.BEING_BUILT)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenCommissioningStateBeingBuiltSelected() {
        selectCommissioningStateFinished(PropertyType.HOUSE, CommissioningState.BEING_BUILT)
    }

    private fun selectCommissioningStateFinished(
        propertyType: PropertyType,
        commissioningState: CommissioningState
    ) {
        val offerCategory = OfferCategory.PRIMARY.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldCommissioningState()).tapOn()

                performOnCommissioningStateDialog {
                    tapOn(commissioningState.matcher.invoke(lookup))
                }
                isCommissioningStateEquals(commissioningState.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *offerCategory.params,
                commissioningState.param
            )
        )
    }
}
