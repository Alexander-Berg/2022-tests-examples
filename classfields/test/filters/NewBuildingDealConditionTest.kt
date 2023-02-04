package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 22.07.2019.
 */
class NewBuildingDealConditionTest : FilterParamTest() {

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenDiscountSelected() {
        shouldChangeOffersCountDealConditionSet(DealCondition.DISCOUNT)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenMortgageSelected() {
        shouldChangeOffersCountDealConditionSet(DealCondition.MORTGAGE)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenInstallmentSelected() {
        shouldChangeOffersCountDealConditionSet(DealCondition.INSTALLMENT)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenFz214Selected() {
        shouldChangeOffersCountDealConditionSet(DealCondition.FZ_214)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenMaternityFundsSelected() {
        shouldChangeOffersCountDealConditionSet(DealCondition.MATERNITY_FUNDS)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenMilitaryMortgageSelected() {
        shouldChangeOffersCountDealConditionSet(DealCondition.MILITARY_MORTGAGE)
    }

    private fun shouldChangeOffersCountDealConditionSet(dealCondition: DealCondition) {
        val category = OfferCategory.PRIMARY.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(category.matcher.invoke(lookup))

                scrollToPosition(dealCondition.fieldMatcher.invoke(lookup))
                    .tapOn()

                isChecked(dealCondition.valueMatcher.invoke(lookup))
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                dealCondition.param,
                *category.params
            )
        )
    }
}
