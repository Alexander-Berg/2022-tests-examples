package com.yandex.mobile.realty.filters.mappers

import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.Rubric
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.search.isSamolet
import com.yandex.mobile.realty.domain.model.search.validateWithRegionParams
import com.yandex.mobile.realty.filters.getPopulatedVillageHouseFilter
import com.yandex.mobile.realty.filters.getPopulatedVillageLotFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author rogovalex on 09.05.18.
 */
class RegionParamsRestrictionsTest {

    val config = RegionParamsConfigImpl()

    @Test
    fun testRemoveRegionDependentFiltersForBuyAnyApartmentFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.SellApartment(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
        assertNull(result.demolition)
        assertNull(result.buildingEpoch)
    }

    @Test
    fun testRetainRegionDependentFiltersForBuyAnyApartmentFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.APARTMENT_SELL to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport",
                    "expectDemolition", "buildingEpoch"
                )
            )
        )

        val filter = Filter.SellApartment(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
        assertEquals(true, result.demolition)
        assertEquals(setOf(Filter.BuildingEpoch.STALIN), result.buildingEpoch)
    }

    @Test
    fun testRemoveRegionDependentFiltersForBuyOldApartmentFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.SellApartment(
            market = Filter.Market.SECONDARY,
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
        assertNull(result.demolition)
    }

    @Test
    fun testRetainRegionDependentFiltersForBuyOldApartmentFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.APARTMENT_SELL to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport", "expectDemolition"
                )
            )
        )

        val filter = Filter.SellApartment(
            market = Filter.Market.SECONDARY,
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
        assertEquals(true, result.demolition)
    }

    @Test
    fun testRemoveRegionDependentFiltersForBuyNewBuildingApartmentFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.SiteApartment(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            developer = Filter.Developer.SAMOLET,
            hasSpecialProposal = true
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SiteApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
        assertNull(result.developer)
        assertNull(result.hasSpecialProposal)
    }

    @Test
    fun testRetainRegionDependentFiltersForBuyNewBuildingApartmentFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.APARTMENT_SELL_NEWBUILDING to setOf(
                    "parkType", "pondType",
                    "expectMetro", "timeToMetro", "metroTransport",
                    "hasSpecialProposal", "onlySamolet"
                )
            )
        )

        val filter = Filter.SiteApartment(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            developer = Filter.Developer.SAMOLET,
            hasSpecialProposal = true
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SiteApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
        assertTrue(filter.developer.isSamolet())
        assertEquals(true, result.hasSpecialProposal)
    }

    @Test
    fun testRemoveRegionDependentFiltersForBuyRoomFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.SellRoom(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellRoom

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
        assertNull(result.demolition)
        assertNull(result.buildingEpoch)
    }

    @Test
    fun testRetainRegionDependentFiltersForBuyRoomFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.ROOMS_SELL to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport",
                    "expectDemolition", "buildingEpoch"
                )
            )
        )

        val filter = Filter.SellRoom(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellRoom

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
        assertEquals(true, result.demolition)
        assertEquals(setOf(Filter.BuildingEpoch.STALIN), result.buildingEpoch)
    }

    @Test
    fun testRemoveRegionDependentFiltersForBuyHouseFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.SellHouse(
            market = Filter.Market.SECONDARY,
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellHouse

        assertNull(result.market)
        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
    }

    @Test
    fun testRetainRegionDependentFiltersForBuyHouseFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.HOUSE_SELL to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport"
                )
            ),
            hasVillages = true
        )

        val filter = Filter.SellHouse(
            market = Filter.Market.SECONDARY,
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellHouse

        assertEquals(Filter.Market.SECONDARY, result.market)
        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
    }

    @Test
    fun testResetVillageHouseFilterToBuyHouseFilter() {
        val regionParams = createRegionParams()

        val filter = getPopulatedVillageHouseFilter()

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellHouse

        assertEquals(filter.price, result.price)
        assertEquals(filter.priceType, result.priceType)
        assertNull(result.showFromAgents)
        assertEquals(filter.houseArea, result.houseArea)
        assertEquals(filter.lotArea, result.lotArea)
        assertNull(result.houseType)
        assertNull(result.metroRemoteness)
        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.includeTags)
        assertNull(result.excludeTags)
        assertEquals(filter.withWaterSupply, result.withWaterSupply)
        assertEquals(filter.withHeatingSupply, result.withHeatingSupply)
        assertEquals(filter.withElectricitySupply, result.withElectricitySupply)
        assertEquals(filter.withGasSupply, result.withGasSupply)
        assertEquals(filter.withSewerageSupply, result.withSewerageSupply)
        assertNull(result.withPhoto)
        assertNull(result.market)
    }

    @Test
    fun testResetVillageLotFilterToBuyLotFilter() {
        val regionParams = createRegionParams()

        val filter = getPopulatedVillageLotFilter()

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellLot

        assertEquals(filter.price, result.price)
        assertEquals(filter.priceType, result.priceType)
        assertNull(result.showFromAgents)
        assertEquals(filter.lotArea, result.lotArea)
        assertNull(result.lotType)
        assertNull(result.metroRemoteness)
        assertNull(result.withPhoto)
        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.includeTags)
        assertNull(result.excludeTags)
        assertNull(result.market)
    }

    @Test
    fun testRemoveRegionDependentFiltersForBuyLotFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.SellLot(
            market = Filter.Market.SECONDARY,
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellLot

        assertNull(result.market)
        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
    }

    @Test
    fun testRetainRegionDependentFiltersForBuyLotFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.LOT_SELL to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport"
                )
            ),
            hasVillages = true
        )

        val filter = Filter.SellLot(
            market = Filter.Market.SECONDARY,
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellLot

        assertEquals(Filter.Market.SECONDARY, result.market)
        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
    }

    @Test
    fun testRemoveRegionDependentFiltersForByCommercialFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.SellCommercial(
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellCommercial

        assertNull(result.metroRemoteness)
    }

    @Test
    fun testRetainRegionDependentFiltersForBuyCommercialFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.COMMERCIAL_SELL to setOf("timeToMetro", "metroTransport")
            )
        )

        val filter = Filter.SellCommercial(
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.SellCommercial

        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
    }

    @Test
    fun testRemoveRegionDependentFiltersForRentApartmentFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.RentApartment(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN),
            yandexRent = true
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
        assertNull(result.demolition)
        assertNull(result.buildingEpoch)
        assertNull(result.yandexRent)
    }

    @Test
    fun testRetainRegionDependentFiltersForRentApartmentFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.APARTMENT_RENT to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport",
                    "expectDemolition", "buildingEpoch",
                    "yandexRent"
                )
            )
        )

        val filter = Filter.RentApartment(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN),
            yandexRent = true
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentApartment

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
        assertEquals(true, result.demolition)
        assertEquals(setOf(Filter.BuildingEpoch.STALIN), result.buildingEpoch)
        assertEquals(true, result.yandexRent)
    }

    @Test
    fun testRemoveRegionDependentFiltersForRentRoomFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.RentRoom(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentRoom

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
        assertNull(result.demolition)
        assertNull(result.buildingEpoch)
    }

    @Test
    fun testRetainRegionDependentFiltersForRentRoomFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.ROOMS_RENT to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport",
                    "expectDemolition", "buildingEpoch"
                )
            )
        )

        val filter = Filter.RentRoom(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
            demolition = true,
            buildingEpoch = setOf(Filter.BuildingEpoch.STALIN)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentRoom

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
        assertEquals(true, result.demolition)
        assertEquals(setOf(Filter.BuildingEpoch.STALIN), result.buildingEpoch)
    }

    @Test
    fun testRemoveRegionDependentFiltersForRentHouseFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.RentHouse(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentHouse

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertNull(result.expectMetro)
        assertNull(result.metroRemoteness)
    }

    @Test
    fun testRetainRegionDependentFiltersForRentHouseFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.HOUSE_RENT to setOf(
                    "parkType", "pondType", "expectMetro",
                    "timeToMetro", "metroTransport"
                )
            )
        )

        val filter = Filter.RentHouse(
            parkType = setOf(Filter.ParkType.FOREST),
            pondType = setOf(Filter.PondType.RIVER),
            expectMetro = true,
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentHouse

        assertEquals(setOf(Filter.ParkType.FOREST), result.parkType)
        assertEquals(setOf(Filter.PondType.RIVER), result.pondType)
        assertEquals(true, result.expectMetro)
        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
    }

    @Test
    fun testRemoveRegionDependentFiltersForRentCommercialFilter() {
        val regionParams = createRegionParams()

        val filter = Filter.RentCommercial(
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentCommercial

        assertNull(result.metroRemoteness)
    }

    @Test
    fun testRetainRegionDependentFiltersForRentCommercialFilter() {
        val regionParams = createRegionParams(
            filters = mapOf(
                Rubric.COMMERCIAL_RENT to setOf("timeToMetro", "metroTransport")
            )
        )

        val filter = Filter.RentCommercial(
            metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT)
        )

        val result = filter.validateWithRegionParams(regionParams, config) as Filter.RentCommercial

        assertEquals(10, result.metroRemoteness?.minutes)
        assertEquals(Filter.MetroRemoteness.Unit.ON_FOOT, result.metroRemoteness?.unit)
    }

    private fun createRegionParams(
        filters: Map<Rubric, Set<String>> = emptyMap(),
        hasVillages: Boolean = false
    ): RegionParams {
        return RegionParams(
            0,
            0,
            "",
            emptyMap(),
            filters,
            null,
            hasVillages,
            false,
            false,
            false,
            false,
            false,
            false,
            0,
            null
        )
    }
}
