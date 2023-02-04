package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnVillageLandTypeDialog
import org.junit.Test

/**
 * @author scrooge on 13.08.2019.
 */
class VillageLandTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenLandTypeDnpSelected() {
        selectLandType(PropertyType.LOT, LandType.DNP)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenLandTypeIgsSelected() {
        selectLandType(PropertyType.HOUSE, LandType.IGS)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenLandTypeLphSelected() {
        selectLandType(PropertyType.LOT, LandType.LPH)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenLandTypeMgsSelected() {
        selectLandType(PropertyType.HOUSE, LandType.MGS)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenLandTypeSntSelected() {
        selectLandType(PropertyType.LOT, LandType.SNT)
    }

    private fun selectLandType(propertyType: PropertyType, landType: LandType) {
        val offerCategory = OfferCategory.PRIMARY.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldVillageLandType()).tapOn()

                performOnVillageLandTypeDialog {
                    tapOn(landType.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isVillageLandTypeEquals(landType.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *offerCategory.params,
                landType.param
            )
        )
    }
}
