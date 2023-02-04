package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnDecorationDialog
import org.junit.Test

/**
 * @author scrooge on 25.07.2019.
 */
class DecorationTest : FilterParamTest() {

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenDecorationRoughSelected() {
        selectDecoration(Decoration.ROUGH)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenDecorationCleanSelected() {
        selectDecoration(Decoration.CLEAN)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenDecorationTurnkeySelected() {
        selectDecoration(Decoration.TURNKEY)
    }

    private fun selectDecoration(decoration: Decoration) {
        val offerCategory = OfferCategory.PRIMARY(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldDecoration()).tapOn()

                performOnDecorationDialog {
                    tapOn(decoration.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isDecorationEquals(decoration.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                *offerCategory.params,
                decoration.param
            )
        )
    }
}
