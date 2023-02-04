package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 10.07.2019.
 */
class LandTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellLotOffersCountWhenIgsSelected() {
        shouldChangeOffersCountLotTypeSet(TypeLot.IGS)
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenGardenSelected() {
        shouldChangeOffersCountLotTypeSet(TypeLot.GARDEN)
    }

    private fun shouldChangeOffersCountLotTypeSet(
        lotType: TypeLot
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.LOT.matcher.invoke(lookup))

                scrollToPosition(lotType.fieldMatcher.invoke(lookup))
                    .tapOn()

                isChecked(lotType.valueMatcher.invoke(lookup))
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.LOT.param,
                lotType.param
            )
        )
    }
}
