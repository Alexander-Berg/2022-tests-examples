package com.yandex.mobile.realty.data.model

import com.yandex.mobile.realty.data.model.RecentSearchIdGenerator.FilterEncoder
import com.yandex.mobile.realty.filters.getPopulatedBuyApartmentAnyFilter
import com.yandex.mobile.realty.filters.getPopulatedBuyApartmentNewBuildingFilter
import com.yandex.mobile.realty.filters.getPopulatedBuyCommercialAnyFilter
import com.yandex.mobile.realty.filters.getPopulatedBuyGarageFitler
import com.yandex.mobile.realty.filters.getPopulatedBuyHouseFilter
import com.yandex.mobile.realty.filters.getPopulatedBuyLotFilter
import com.yandex.mobile.realty.filters.getPopulatedBuyRoomFilter
import com.yandex.mobile.realty.filters.getPopulatedRentApartmentFilter
import com.yandex.mobile.realty.filters.getPopulatedRentCommercialAnyFilter
import com.yandex.mobile.realty.filters.getPopulatedRentGarageFilter
import com.yandex.mobile.realty.filters.getPopulatedRentHouseFilter
import com.yandex.mobile.realty.filters.getPopulatedRentRoomFilter
import com.yandex.mobile.realty.filters.getPopulatedVillageHouseFilter
import com.yandex.mobile.realty.filters.getPopulatedVillageLotFilter
import com.yandex.mobile.realty.utils.getInstanceFieldsCount
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author shpigun on 2019-08-26
 */
class FilterEncoderTest {

    @Test
    fun testBuyApartmentAnyAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedBuyApartmentAnyFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testBuyRoomAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedBuyRoomFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testBuyHouseAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedBuyHouseFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testBuyLotAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedBuyLotFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testRentApartmentAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedRentApartmentFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testRentRoomAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedRentRoomFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testRentHouseAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedRentHouseFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testBuyCommercialAnyAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedBuyCommercialAnyFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testRentCommercialAnyAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedRentCommercialAnyFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testBuyGarageAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedBuyGarageFitler()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testRentGarageAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedRentGarageFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testSiteApartmentAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedBuyApartmentNewBuildingFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testVillageHouseAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedVillageHouseFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }

    @Test
    fun testVillageLotAllFieldsEncoded() {
        val filterEncoder = FilterEncoder()
        val filter = getPopulatedVillageLotFilter()
        filterEncoder.encode(filter)
        assertEquals(filter.getInstanceFieldsCount(), filterEncoder.getHandledFieldsCount())
    }
}
