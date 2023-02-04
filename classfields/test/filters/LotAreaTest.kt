package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import com.yandex.mobile.realty.core.robot.performOnLotAreaDialog
import org.junit.Test

/**
 * @author scrooge on 26.06.2019.
 */
class LotAreaTest : FilterParamTest() {

    @Test
    fun shouldChangeSellLotOffersCountWhenMinLotAreaSet() {
        shouldChangeOffersCountWhenLotAreaSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.HOUSE,
            lotAreaMin = 1,
            expected = "от 1 сот."
        )
    }

    @Test
    fun shouldChangeRentLotOffersCountWhenMaxLotAreaSet() {
        shouldChangeOffersCountWhenLotAreaSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE,
            lotAreaMax = 12,
            expected = "до 12 сот."
        )
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenMinMaxLotAreaSet() {
        shouldChangeOffersCountWhenLotAreaSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.LOT,
            lotAreaMin = 1,
            lotAreaMax = 100,
            expected = "1 – 100 сот."
        )
    }

    @Test
    fun shouldChangeRentCommercialLandOffersCountWhenMinLotAreaSet() {
        shouldChangeOffersCountWhenLotAreaSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.LAND,
            lotAreaMin = 333,
            expected = "от 333 сот."
        )
    }

    @Test
    fun shouldChangeSellCommercialLandOffersCountWhenMinMaxLotAreaSet() {
        shouldChangeOffersCountWhenLotAreaSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.LAND,
            lotAreaMin = 150,
            lotAreaMax = 2000,
            expected = "150 – 2000 сот."
        )
    }

    private fun shouldChangeOffersCountWhenLotAreaSet(
        dealType: DealType,
        propertyType: PropertyType,
        commercialType: CommercialType? = null,
        lotAreaMin: Int? = null,
        lotAreaMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))

                commercialType?.let {
                    tapOn(lookup.matchesFieldCommercialType())
                    performOnCommercialTypeScreen {
                        scrollTo(commercialType.matcher.invoke(lookup))
                            .tapOn()
                        tapOn(lookup.matchesApplyButton())
                    }
                }

                scrollToPosition(lookup.matchesFieldLotArea()).tapOn()
                performOnLotAreaDialog {
                    waitUntilKeyboardAppear()
                    lotAreaMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    lotAreaMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isLotAreaEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                commercialType?.param,
                "lotAreaMin" to lotAreaMin?.toString(),
                "lotAreaMax" to lotAreaMax?.toString()
            )
        )
    }
}
