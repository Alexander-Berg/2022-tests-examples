package com.yandex.mobile.realty.filters

import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.ui.presenter.countParams
import org.junit.Assert.assertEquals
import org.junit.Test

class CountFilterParamsVisitorTest {

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
    fun testRentPeriodParameterNotCounted() {
        val filter = Filter.RentApartment(
            rentTime = Filter.RentTime.LARGE
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
    fun testCountApartmentCategoryNewBuilding() {
        val filter = Filter.SiteApartment()
        assertEquals(1, count(filter))
    }

    @Test
    fun testCountApartmentCategoryOld() {
        val filter = Filter.SellApartment(
            market = Filter.Market.SECONDARY
        )
        assertEquals(1, count(filter))
    }

    @Test
    fun testFilterRentCommercialPrice() {
        val rentCommercialFilter = Filter.RentCommercial(
            priceType = Filter.PriceType.PER_METER,
            price = Range.valueOf(1L, null),
            pricingPeriod = Filter.PricingPeriod.PER_YEAR
        )
        assertEquals(1, count(rentCommercialFilter))
    }

    @Test
    fun testFilterBuyApartmentAnyFullParams() {
        assertEquals(32, count(getPopulatedBuyApartmentAnyFilter()))
    }

    @Test
    fun testCountApartmentCategoryOldFullParams() {
        assertEquals(33, count(getPopulatedBuyApartmentOldFilter()))
    }

    @Test
    fun testFilterBuyApartmentNewBuildingFullParams() {
        assertEquals(28, count(getPopulatedBuyApartmentNewBuildingFilter()))
    }

    @Test
    fun testFilterRentApartmentFullParams() {
        assertEquals(30, count(getPopulatedRentApartmentFilter()))
    }

    @Test
    fun testFilterBuyRoomFullParams() {
        assertEquals(26, count(getPopulatedBuyRoomFilter()))
    }

    @Test
    fun testFilterRentRoomFullParams() {
        assertEquals(26, count(getPopulatedRentRoomFilter()))
    }

    @Test
    fun testFilterBuyHouseFullParams() {
        assertEquals(20, count(getPopulatedBuyHouseFilter()))
    }

    @Test
    fun testFilterRentHouseFullParams() {
        assertEquals(18, count(getPopulatedRentHouseFilter()))
    }

    @Test
    fun testFilterBuyLotFullParams() {
        assertEquals(16, count(getPopulatedBuyLotFilter()))
    }

    @Test
    fun testFilterBuyGarageFullParams() {
        assertEquals(14, count(getPopulatedBuyGarageFitler()))
    }

    @Test
    fun testFilterRentGarageFullParams() {
        assertEquals(17, count(getPopulatedRentGarageFilter()))
    }

    @Test
    fun testFilterBuyCommercialAnyFullParams() {
        assertEquals(13, count(getPopulatedBuyCommercialAnyFilter()))
    }

    @Test
    fun testFilterBuyCommercialLandFullParams() {
        assertEquals(10, count(getPopulatedBuyCommercialLandFilter()))
    }

    @Test
    fun testFilterBuyCommercialOfficeFullParams() {
        assertEquals(21, count(getPopulatedBuyCommercialOfficeFilter()))
    }

    @Test
    fun testFilterBuyCommercialRetailFullParams() {
        assertEquals(20, count(getPopulatedBuyCommercialRetailFilter()))
    }

    @Test
    fun testFilterBuyCommercialFreePurposeFullParams() {
        assertEquals(21, count(getPopulatedBuyCommercialFreePurposeFilter()))
    }

    @Test
    fun testFilterBuyCommercialPublicCateringFullParams() {
        assertEquals(14, count(getPopulatedBuyCommercialPublicCateringFilter()))
    }

    @Test
    fun testFilterBuyCommercialHotelFullParams() {
        assertEquals(14, count(getPopulatedBuyCommercialHotelFilter()))
    }

    @Test
    fun testFilterBuyCommercialAutoRepairFullParams() {
        assertEquals(13, count(getPopulatedBuyCommercialAutoRepairFilter()))
    }

    @Test
    fun testFilterBuyCommercialWarehouseFullParams() {
        assertEquals(17, count(getPopulatedBuyCommercialWarehouseFilter()))
    }

    @Test
    fun testFilterBuyCommercialManufacturingFullParams() {
        assertEquals(17, count(getPopulatedBuyCommercialManufacturingFilter()))
    }

    @Test
    fun testFilterBuyCommercialBusinessFullParams() {
        assertEquals(13, count(getPopulatedBuyCommercialBusinessFilter()))
    }

    @Test
    fun testFilterRentCommercialAnyFullParams() {
        assertEquals(17, count(getPopulatedRentCommercialAnyFilter()))
    }

    @Test
    fun testFilterRentCommercialLandFullParams() {
        assertEquals(14, count(getPopulatedRentCommercialLandFilter()))
    }

    @Test
    fun testFilterRentCommercialOfficeFullParams() {
        assertEquals(28, count(getPopulatedRentCommercialOfficeFilter()))
    }

    @Test
    fun testFilterRentCommercialRetailFullParams() {
        assertEquals(27, count(getPopulatedRentCommercialRetailFilter()))
    }

    @Test
    fun testFilterRentCommercialFreePurposeFullParams() {
        assertEquals(28, count(getPopulatedRentCommercialFreePurposeFilter()))
    }

    @Test
    fun testFilterRentCommercialWarehouseFullParams() {
        assertEquals(24, count(getPopulatedRentCommercialWarehouseFilter()))
    }

    @Test
    fun testFilterRentCommercialPublicCateringFullParams() {
        assertEquals(21, count(getPopulatedRentCommercialPublicCateringFilter()))
    }

    @Test
    fun testFilterRentCommercialHotelFullParams() {
        assertEquals(21, count(getPopulatedRentCommercialHotelFilter()))
    }

    @Test
    fun testFilterRentCommercialAutoRepairFullParams() {
        assertEquals(20, count(getPopulatedRentCommercialAutoRepairFitler()))
    }

    @Test
    fun testFilterRentCommercialManufacturingFullParams() {
        assertEquals(24, count(getPopulatedRentCommercialManufacturingFilter()))
    }

    @Test
    fun testFilterRentCommercialLegalAddressFullParams() {
        assertEquals(21, count(getPopulatedRentCommercialLegalFilter()))
    }

    @Test
    fun testFilterRentCommercialBusinessFullParams() {
        assertEquals(20, count(getPopulatedRentCommercialBusinessFilter()))
    }

    @Test
    fun testVillageHouseFilterFullParams() {
        assertEquals(14, count(getPopulatedVillageHouseFilter()))
    }

    @Test
    fun testVillageLotFilterFullParams() {
        assertEquals(11, count(getPopulatedVillageLotFilter()))
    }

    private fun count(filter: Filter): Int {
        return filter.countParams()
    }
}
