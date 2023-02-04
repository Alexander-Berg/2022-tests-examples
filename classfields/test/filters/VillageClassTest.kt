package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnVillageClassDialog
import org.junit.Test

/**
 * @author scrooge on 13.08.2019.
 */
class VillageClassTest : FilterParamTest() {

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenBuildingClassEconomySelected() {
        shouldChangeOffersCountWhenVillageClassSet(PropertyType.LOT, VillageClass.ECONOM)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenBuildingClassComfortSelected() {
        shouldChangeOffersCountWhenVillageClassSet(PropertyType.HOUSE, VillageClass.COMFORT)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenBuildingClassComfortPlusSelected() {
        shouldChangeOffersCountWhenVillageClassSet(PropertyType.LOT, VillageClass.COMFORT_PLUS)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenBuildingClassBusinessSelected() {
        shouldChangeOffersCountWhenVillageClassSet(PropertyType.HOUSE, VillageClass.BUSINESS)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenBuildingClassEliteSelected() {
        shouldChangeOffersCountWhenVillageClassSet(PropertyType.LOT, VillageClass.ELITE)
    }

    private fun shouldChangeOffersCountWhenVillageClassSet(
        propertyType: PropertyType,
        villageClass: VillageClass
    ) {
        val offerCategory = OfferCategory.PRIMARY(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldVillageClass()).tapOn()

                performOnVillageClassDialog {
                    tapOn(villageClass.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isVillageClassEquals(villageClass.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *offerCategory.params,
                villageClass.param
            )
        )
    }
}
