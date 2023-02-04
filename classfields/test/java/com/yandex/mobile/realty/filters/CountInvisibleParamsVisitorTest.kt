package com.yandex.mobile.realty.filters

import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.ui.presenter.countSavedSearchHiddenParams
import org.junit.Assert.assertEquals
import org.junit.Test

class CountInvisibleParamsVisitorTest {

    @Test
    fun testCountEmptyBuyApartmentFilter() {
        val filter = Filter.SellApartment()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyRentApartmentFilter() {
        val filter = Filter.RentApartment()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyBuyRoomFilter() {
        val filter = Filter.SellRoom()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyRentRoomFilter() {
        val filter = Filter.RentRoom()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyBuyHouseFilter() {
        val filter = Filter.SellHouse()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyRentHouseFilter() {
        val filter = Filter.RentHouse()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyBuyLotFilter() {
        val filter = Filter.SellLot()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyBuyGarageFilter() {
        val filter = Filter.SellGarage()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyRentGarageFilter() {
        val filter = Filter.RentGarage()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyBuyCommercialFilter() {
        val filter = Filter.SellCommercial()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountEmptyRentCommercialFilter() {
        val filter = Filter.RentCommercial()
        assertEquals(0, count(filter))
    }

    @Test
    fun testFilterFloatParameter() {
        val filter = Filter.SellApartment(
            ceilingHeightMin = 0.1f
        )
        assertEquals(1, count(filter))
    }

    @Test
    fun testFilterFlagParameter() {
        val filter = Filter.SellApartment(
            showFromAgents = false
        )
        assertEquals(1, count(filter))
    }

    @Test
    fun testFilterSingleSelectParameter() {
        val filter = Filter.SellApartment(
            demolition = false
        )
        assertEquals(1, count(filter))
    }

    @Test
    fun testFilterRangeParameter() {
        val filter = Filter.RentApartment(
            kitchenArea = Range.valueOf(1L, null)
        )
        assertEquals(1, count(filter))
    }

    @Test
    fun testFilterMultiselectParameter() {
        val filter = Filter.SellApartment(
            buildingType = setOf(Filter.BuildingType.BRICK, Filter.BuildingType.BLOCK)
        )
        assertEquals(1, count(filter))
    }

    @Test
    fun testRoomCountParameterNotCounted() {
        val filter = Filter.SellApartment(
            roomsCount = setOf(Filter.RoomsCount.ONE)
        )
        assertEquals(0, count(filter))
    }

    @Test
    fun testRentPeriodParameterNotCounted() {
        val filter = Filter.RentApartment(
            rentTime = Filter.RentTime.LARGE
        )
        assertEquals(0, count(filter))
    }

    @Test
    fun testFilterGarageTypeParameterNotCounted() {
        val filter = Filter.SellGarage(
            garageType = setOf(Filter.GarageType.BOX)
        )
        assertEquals(0, count(filter))
    }

    @Test
    fun testFilterCommercialTypeParameterNotCounted() {
        val filter = Filter.SellCommercial(
            commercialType = setOf(Filter.CommercialType.LAND)
        )
        assertEquals(0, count(filter))
    }

    @Test
    fun testFilterMetroDistance() {
        val filter = Filter.SellApartment(
            metroRemoteness = Filter.MetroRemoteness(5, Filter.MetroRemoteness.Unit.ON_TRANSPORT)
        )
        assertEquals(1, count(filter))
    }

    @Test
    fun testCountApartmentCategoryNewBuildingNotCounted() {
        val filter = Filter.SiteApartment()
        assertEquals(0, count(filter))
    }

    @Test
    fun testCountApartmentCategoryOldNotCounted() {
        val filter = Filter.SellApartment(
            market = Filter.Market.SECONDARY
        )
        assertEquals(0, count(filter))
    }

    @Test
    fun testFilterRentCommercialPriceNotCounted() {
        val rentCommercialFilter = Filter.RentCommercial(
            priceType = Filter.PriceType.PER_METER,
            price = Range.valueOf(1L, null),
            pricingPeriod = Filter.PricingPeriod.PER_YEAR
        )
        assertEquals(0, count(rentCommercialFilter))
    }

    @Test
    fun testFilterBuyApartmentAnyFullParams() {
        assertEquals(29, count(getPopulatedBuyApartmentAnyFilter()))
    }

    @Test
    fun testCountApartmentCategoryOldFullParams() {
        assertEquals(29, count(getPopulatedBuyApartmentOldFilter()))
    }

    @Test
    fun testFilterBuyApartmentNewBuildingFullParams() {
        assertEquals(24, count(getPopulatedBuyApartmentNewBuildingFilter()))
    }

    @Test
    fun testFilterBuyApartmentNewBuildingSamoletFullParams() {
        assertEquals(22, count(getPopulatedBuyApartmentNewBuildingSamoletFilter()))
    }

    @Test
    fun testFilterRentApartmentFullParams() {
        assertEquals(27, count(getPopulatedRentApartmentFilter()))
    }

    @Test
    fun testFilterBuyRoomFullParams() {
        assertEquals(24, count(getPopulatedBuyRoomFilter()))
    }

    @Test
    fun testFilterRentRoomFullParams() {
        assertEquals(24, count(getPopulatedRentRoomFilter()))
    }

    @Test
    fun testFilterBuyHouseFullParams() {
        assertEquals(16, count(getPopulatedBuyHouseFilter()))
    }

    @Test
    fun testFilterRentHouseFullParams() {
        assertEquals(15, count(getPopulatedRentHouseFilter()))
    }

    @Test
    fun testFilterBuyLotFullParams() {
        assertEquals(13, count(getPopulatedBuyLotFilter()))
    }

    @Test
    fun testFilterBuyGarageFullParams() {
        assertEquals(12, count(getPopulatedBuyGarageFitler()))
    }

    @Test
    fun testFilterRentGarageFullParams() {
        assertEquals(15, count(getPopulatedRentGarageFilter()))
    }

    @Test
    fun testFilterBuyCommercialAnyFullParams() {
        assertEquals(12, count(getPopulatedBuyCommercialAnyFilter()))
    }

    @Test
    fun testFilterBuyCommercialLandFullParams() {
        assertEquals(8, count(getPopulatedBuyCommercialLandFilter()))
    }

    @Test
    fun testFilterBuyCommercialOfficeFullParams() {
        assertEquals(19, count(getPopulatedBuyCommercialOfficeFilter()))
    }

    @Test
    fun testFilterBuyCommercialRetailFullParams() {
        assertEquals(18, count(getPopulatedBuyCommercialRetailFilter()))
    }

    @Test
    fun testFilterBuyCommercialFreePurposeFullParams() {
        assertEquals(19, count(getPopulatedBuyCommercialFreePurposeFilter()))
    }

    @Test
    fun testFilterBuyCommercialPublicCateringFullParams() {
        assertEquals(12, count(getPopulatedBuyCommercialPublicCateringFilter()))
    }

    @Test
    fun testFilterBuyCommercialHotelFullParams() {
        assertEquals(12, count(getPopulatedBuyCommercialHotelFilter()))
    }

    @Test
    fun testFilterBuyCommercialAutoRepairFullParams() {
        assertEquals(11, count(getPopulatedBuyCommercialAutoRepairFilter()))
    }

    @Test
    fun testFilterBuyCommercialWarehouseFullParams() {
        assertEquals(15, count(getPopulatedBuyCommercialWarehouseFilter()))
    }

    @Test
    fun testFilterBuyCommercialManufacturingFullParams() {
        assertEquals(15, count(getPopulatedBuyCommercialManufacturingFilter()))
    }

    @Test
    fun testFilterBuyCommercialBusinessFullParams() {
        assertEquals(11, count(getPopulatedBuyCommercialBusinessFilter()))
    }

    @Test
    fun testFilterRentCommercialAnyFullParams() {
        assertEquals(16, count(getPopulatedRentCommercialAnyFilter()))
    }

    @Test
    fun testFilterRentCommercialLandFullParams() {
        assertEquals(12, count(getPopulatedRentCommercialLandFilter()))
    }

    @Test
    fun testFilterRentCommercialOfficeFullParams() {
        assertEquals(26, count(getPopulatedRentCommercialOfficeFilter()))
    }

    @Test
    fun testFilterRentCommercialRetailFullParams() {
        assertEquals(25, count(getPopulatedRentCommercialRetailFilter()))
    }

    @Test
    fun testFilterRentCommercialFreePurposeFullParams() {
        assertEquals(26, count(getPopulatedRentCommercialFreePurposeFilter()))
    }

    @Test
    fun testFilterRentCommercialWarehouseFullParams() {
        assertEquals(22, count(getPopulatedRentCommercialWarehouseFilter()))
    }

    @Test
    fun testFilterRentCommercialPublicCateringFullParams() {
        assertEquals(19, count(getPopulatedRentCommercialPublicCateringFilter()))
    }

    @Test
    fun testFilterRentCommercialHotelFullParams() {
        assertEquals(19, count(getPopulatedRentCommercialHotelFilter()))
    }

    @Test
    fun testFilterRentCommercialAutoRepairFullParams() {
        assertEquals(18, count(getPopulatedRentCommercialAutoRepairFitler()))
    }

    @Test
    fun testFilterRentCommercialManufacturingFullParams() {
        assertEquals(22, count(getPopulatedRentCommercialManufacturingFilter()))
    }

    @Test
    fun testFilterRentCommercialLegalAddressFullParams() {
        assertEquals(19, count(getPopulatedRentCommercialLegalFilter()))
    }

    @Test
    fun testFilterRentCommercialBusinessFullParams() {
        assertEquals(18, count(getPopulatedRentCommercialBusinessFilter()))
    }

    @Test
    fun testVillageHouseFilterFullParams() {
        assertEquals(9, count(getPopulatedVillageHouseFilter()))
    }

    @Test
    fun testVillageLotFilterFullParams() {
        assertEquals(8, count(getPopulatedVillageLotFilter()))
    }

    private fun count(filter: Filter): Int {
        return filter.countSavedSearchHiddenParams()
    }
}
