package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnLotAreaDialog
import org.junit.Test

/**
 * @author scrooge on 13.08.2019.
 */
class VillageLotAreaTest : FilterParamTest() {

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenMinLotAreaSet() {
        selectLotArea(
            propertyType = PropertyType.LOT,
            lotAreaMin = 1,
            expected = "от 1 сот."
        )
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenMaxLotAreaSet() {
        selectLotArea(
            propertyType = PropertyType.HOUSE,
            lotAreaMax = 1000,
            expected = "до 1000 сот."
        )
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenMinMaxLotAreaSet() {
        selectLotArea(
            propertyType = PropertyType.LOT,
            lotAreaMin = 15,
            lotAreaMax = 2000,
            expected = "15 – 2000 сот."
        )
    }

    private fun selectLotArea(
        propertyType: PropertyType,
        lotAreaMin: Int? = null,
        lotAreaMax: Int? = null,
        expected: String
    ) {
        val offerCategory = OfferCategory.PRIMARY.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldVillageLotArea()).tapOn()
                performOnLotAreaDialog {
                    waitUntilKeyboardAppear()
                    lotAreaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    lotAreaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isVillageLotAreaEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *offerCategory.params,
                "lotAreaMin" to lotAreaMin?.toString(),
                "lotAreaMax" to lotAreaMax?.toString()
            )
        )
    }
}
