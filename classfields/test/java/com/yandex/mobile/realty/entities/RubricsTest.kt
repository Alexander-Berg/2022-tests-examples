package com.yandex.mobile.realty.entities

import com.yandex.mobile.realty.domain.Rubric
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.entities.filters.ApartmentCategory
import com.yandex.mobile.realty.entities.filters.DealType
import com.yandex.mobile.realty.entities.filters.HouseCategory
import com.yandex.mobile.realty.entities.filters.LotCategory
import com.yandex.mobile.realty.entities.filters.PropertyType
import com.yandex.mobile.realty.filters.dependency.FilterScreenRubricProvider
import com.yandex.mobile.realty.utils.getRubric
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author rogovalex on 09.05.18.
 */
class RubricsTest {

    @Test
    fun testGetApartmentSellRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.BUY, PropertyType.APARTMENT)
        assertEquals(Rubric.APARTMENT_SELL, rubric)
    }

    @Test
    fun testGetRoomsSellRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.BUY, PropertyType.ROOM)
        assertEquals(Rubric.ROOMS_SELL, rubric)
    }

    @Test
    fun testGetHouseSellRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.BUY, PropertyType.HOUSE)
        assertEquals(Rubric.HOUSE_SELL, rubric)
    }

    @Test
    fun testGetLotSellRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.BUY, PropertyType.AREA)
        assertEquals(Rubric.LOT_SELL, rubric)
    }

    @Test
    fun testGetCommercialSellRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.BUY, PropertyType.COMMERCIAL)
        assertEquals(Rubric.COMMERCIAL_SELL, rubric)
    }

    @Test
    fun testGetGarageSellRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.BUY, PropertyType.GARAGE)
        assertEquals(Rubric.GARAGE_SELL, rubric)
    }

    @Test
    fun testGetApartmentRentRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.RENT, PropertyType.APARTMENT)
        assertEquals(Rubric.APARTMENT_RENT, rubric)
    }

    @Test
    fun testGetRoomsRentRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.RENT, PropertyType.ROOM)
        assertEquals(Rubric.ROOMS_RENT, rubric)
    }

    @Test
    fun testGetHouseRentRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.RENT, PropertyType.HOUSE)
        assertEquals(Rubric.HOUSE_RENT, rubric)
    }

    @Test
    fun testGetCommercialRentRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.RENT, PropertyType.COMMERCIAL)
        assertEquals(Rubric.COMMERCIAL_RENT, rubric)
    }

    @Test
    fun testGetGarageRentRubric() {
        val rubric = FilterScreenRubricProvider.getRubric(DealType.RENT, PropertyType.GARAGE)
        assertEquals(Rubric.GARAGE_RENT, rubric)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetLotRentRubric() {
        FilterScreenRubricProvider.getRubric(DealType.RENT, PropertyType.AREA)
    }

    @Test
    fun testGetApartmentSellRubricByAnyApartmentCategory() {
        val rubric = FilterScreenRubricProvider.getBuyApartmentRubric(ApartmentCategory.ANY)
        assertEquals(Rubric.APARTMENT_SELL, rubric)
    }

    @Test
    fun testGetApartmentSellRubricByOldApartmentCategory() {
        val rubric = FilterScreenRubricProvider.getBuyApartmentRubric(ApartmentCategory.OLD)
        assertEquals(Rubric.APARTMENT_SELL, rubric)
    }

    @Test
    fun testGetApartmentSellNewBuildingRubric() {
        val rubric = FilterScreenRubricProvider
            .getBuyApartmentRubric(ApartmentCategory.NEW_BUILDING)
        assertEquals(Rubric.APARTMENT_SELL_NEWBUILDING, rubric)
    }

    @Test
    fun testGetHouseSellVillageRubric() {
        val rubric = FilterScreenRubricProvider.getBuyHouseRubric(HouseCategory.VILLAGE)
        assertEquals(Rubric.HOUSE_SELL_VILLAGE, rubric)
    }

    @Test
    fun testGetHouseSellAnyRubric() {
        val rubric = FilterScreenRubricProvider.getBuyHouseRubric(HouseCategory.ANY)
        assertEquals(Rubric.HOUSE_SELL, rubric)
    }

    @Test
    fun testGetHouseSellSecondaryRubric() {
        val rubric = FilterScreenRubricProvider.getBuyHouseRubric(HouseCategory.SECONDARY)
        assertEquals(Rubric.HOUSE_SELL, rubric)
    }

    @Test
    fun testGetLotSellVillageRubric() {
        val rubric = FilterScreenRubricProvider.getBuyLotRubric(LotCategory.VILLAGE)
        assertEquals(Rubric.LOT_SELL_VILLAGE, rubric)
    }

    @Test
    fun testGetLotSellAnyRubric() {
        val rubric = FilterScreenRubricProvider.getBuyLotRubric(LotCategory.ANY)
        assertEquals(Rubric.LOT_SELL, rubric)
    }

    @Test
    fun testGetLotSellSecondaryRubric() {
        val rubric = FilterScreenRubricProvider.getBuyLotRubric(LotCategory.SECONDARY)
        assertEquals(Rubric.LOT_SELL, rubric)
    }

    @Test
    fun testGetApartmentSellAnyRubricByFilter() {
        val rubric = Filter.SellApartment().getRubric()
        assertEquals(Rubric.APARTMENT_SELL, rubric)
    }

    @Test
    fun testGetApartmentSellNewBuildingRubricByFilter() {
        val rubric = Filter.SiteApartment().getRubric()
        assertEquals(Rubric.APARTMENT_SELL_NEWBUILDING, rubric)
    }

    @Test
    fun testGetApartmentSellOldRubricByFilter() {
        val rubric = Filter.SellApartment(market = Filter.Market.SECONDARY).getRubric()
        assertEquals(Rubric.APARTMENT_SELL, rubric)
    }

    @Test
    fun testGetRoomSellRubricByFilter() {
        val rubric = Filter.SellRoom().getRubric()
        assertEquals(Rubric.ROOMS_SELL, rubric)
    }

    @Test
    fun testGetHouseSellAnyRubricByFilter() {
        val rubric = Filter.SellHouse().getRubric()
        assertEquals(Rubric.HOUSE_SELL, rubric)
    }

    @Test
    fun testGetHouseSellVillageRubricByFilter() {
        val rubric = Filter.VillageHouse().getRubric()
        assertEquals(Rubric.HOUSE_SELL_VILLAGE, rubric)
    }

    @Test
    fun testGetHouseSellSecondaryRubricByFilter() {
        val rubric = Filter.SellHouse(market = Filter.Market.SECONDARY).getRubric()
        assertEquals(Rubric.HOUSE_SELL, rubric)
    }

    @Test
    fun testGetLotSellRubricByFilter() {
        val rubric = Filter.SellLot().getRubric()
        assertEquals(Rubric.LOT_SELL, rubric)
    }

    @Test
    fun testGetLotSellVillageRubricByFilter() {
        val rubric = Filter.VillageLot().getRubric()
        assertEquals(Rubric.LOT_SELL_VILLAGE, rubric)
    }

    @Test
    fun testGetLotSellSecondaryRubricByFilter() {
        val rubric = Filter.SellLot(market = Filter.Market.SECONDARY).getRubric()
        assertEquals(Rubric.LOT_SELL, rubric)
    }

    @Test
    fun testGetCommercialSellRubricByFilter() {
        val rubric = Filter.SellCommercial().getRubric()
        assertEquals(Rubric.COMMERCIAL_SELL, rubric)
    }

    @Test
    fun testGetGarageSellRubricByFilter() {
        val rubric = Filter.SellGarage().getRubric()
        assertEquals(Rubric.GARAGE_SELL, rubric)
    }

    @Test
    fun testGetApartmentRentRubricByFilter() {
        val rubric = Filter.RentApartment().getRubric()
        assertEquals(Rubric.APARTMENT_RENT, rubric)
    }

    @Test
    fun testGetRoomRentRubricByFilter() {
        val rubric = Filter.RentRoom().getRubric()
        assertEquals(Rubric.ROOMS_RENT, rubric)
    }

    @Test
    fun testGetHouseRentRubricByFilter() {
        val rubric = Filter.RentHouse().getRubric()
        assertEquals(Rubric.HOUSE_RENT, rubric)
    }

    @Test
    fun testGetCommercialRentRubricByFilter() {
        val rubric = Filter.RentCommercial().getRubric()
        assertEquals(Rubric.COMMERCIAL_RENT, rubric)
    }

    @Test
    fun testGetGarageRentRubricByFilter() {
        val rubric = Filter.RentGarage().getRubric()
        assertEquals(Rubric.GARAGE_RENT, rubric)
    }
}
