package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnWallTypeDialog
import org.junit.Test

/**
 * @author scrooge on 13.08.2019.
 */
class WallTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenWoodSelected() {
        selectWallType(WallType.WOOD)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenFrameSelected() {
        selectWallType(WallType.FRAME)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenWallTypeBrickSelected() {
        selectWallType(WallType.BRICK)
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenWallTypeTimberFramingSelected() {
        selectWallType(WallType.TIMBER_FRAMING)
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenWallTypeConcreteSelected() {
        selectWallType(WallType.CONCRET)
    }

    private fun selectWallType(wallType: WallType) {
        val offerCategory = OfferCategory.PRIMARY(PropertyType.HOUSE)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.HOUSE.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldWallType()).tapOn()

                performOnWallTypeDialog {
                    tapOn(wallType.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isWallTypeEquals(wallType.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.HOUSE.param,
                *offerCategory.params,
                wallType.param
            )
        )
    }
}
