package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnBuildingTypeDialog
import org.junit.Test

/**
 * @author scrooge on 05.07.2019.
 */
class BuildingTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAnyOffersCountWhenBuildingTypeBrickSelected() {
        selectSellApartmentBuildingType(OfferCategory.ANY, BuildingType.BRICK)
    }

    @Test
    fun shouldChangeNewBuildingOffersCountWhenBuildingTypePanelSelected() {
        selectSellApartmentBuildingType(OfferCategory.PRIMARY, BuildingType.PANEL)
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenBuildingTypeStalinSelected() {
        selectSellApartmentBuildingType(OfferCategory.SECONDARY, BuildingType.STALIN)
    }

    @Test
    fun shouldChangeApartmentAnyOffersCountWhenBuildingTypeBrezhnevSelected() {
        selectSellApartmentBuildingType(OfferCategory.ANY, BuildingType.BREZHNEV)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenBuildingTypeMonolitBrickSelected() {
        selectRentApartmentBuildingType(BuildingType.MONOLIT_BRICK)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenBuildingTypeMonolitSelected() {
        selectRoomBuildingType(DealType.SELL, BuildingType.MONOLIT)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenBuildingTypeBlockSelected() {
        selectRoomBuildingType(DealType.RENT, BuildingType.BLOCK)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenBuildingTypeKhrushchevSelected() {
        selectRoomBuildingType(DealType.SELL, BuildingType.KHRUSHCHEV)
    }

    private fun selectSellApartmentBuildingType(
        offerCategoryFactory: OfferCategoryFactory,
        buildingType: BuildingType
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldBuildingType()).tapOn()

                performOnBuildingTypeDialog {
                    scrollToPosition(buildingType.matcher.invoke(lookup)).tapOn()
                    tapOn(lookup.matchesPositiveButton())
                }

                isBuildingTypeEquals(buildingType.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                buildingType.param,
                *offerCategory.params
            )
        )
    }

    private fun selectRentApartmentBuildingType(buildingType: BuildingType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldBuildingType()).tapOn()

                performOnBuildingTypeDialog {
                    scrollToPosition(buildingType.matcher.invoke(lookup)).tapOn()
                    tapOn(lookup.matchesPositiveButton())
                }

                isBuildingTypeEquals(buildingType.expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.APARTMENT.param,
                buildingType.param
            )
        )
    }

    private fun selectRoomBuildingType(dealType: DealType, buildingType: BuildingType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.ROOM.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldBuildingType()).tapOn()

                performOnBuildingTypeDialog {
                    scrollToPosition(buildingType.matcher.invoke(lookup)).tapOn()
                    tapOn(lookup.matchesPositiveButton())
                }

                isBuildingTypeEquals(buildingType.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.ROOM.param,
                buildingType.param
            )
        )
    }
}
