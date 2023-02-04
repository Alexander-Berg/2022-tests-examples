package com.yandex.mobile.realty.analytics

import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.search.SearchTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author rogovalex on 2019-10-03.
 */
class SearchCategoriesTest {

    private val metricaEventHelper = MetricaEventHelper()

    @Test
    fun testSearchMixedFlatSell() {
        val categories = metricaEventHelper.getCategories(Filter.SellApartment())
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Flat_Sell"))
        assertTrue(categories.contains("MixedFlat_Sell"))
        assertTrue(categories.contains("Flat"))
        assertTrue(categories.contains("Flat_Sell, Room_Sell"))
    }

    @Test
    fun testSearchSecondaryFlatSell() {
        val categories =
            metricaEventHelper.getCategories(Filter.SellApartment(market = Filter.Market.SECONDARY))
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Flat_Sell"))
        assertTrue(categories.contains("SecondaryFlat_Sell"))
        assertTrue(categories.contains("Flat"))
        assertTrue(categories.contains("Flat_Sell, Room_Sell"))
    }

    @Test
    fun testSearchNewbuildingSell() {
        val categories = metricaEventHelper.getCategories(Filter.SiteApartment())
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Flat_Sell"))
        assertTrue(categories.contains("Newbuilding_Sell"))
        assertTrue(categories.contains("Flat"))
        assertTrue(categories.contains("Flat_Sell, Room_Sell"))
    }

    @Test
    fun testSearchMixedHouseSell() {
        val categories = metricaEventHelper.getCategories(Filter.SellHouse())
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("MixedHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchSecondaryHouseSell() {
        val categories =
            metricaEventHelper.getCategories(Filter.SellHouse(market = Filter.Market.SECONDARY))
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("SecondaryHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchVillageHouseSell() {
        val categories = metricaEventHelper.getCategories(Filter.VillageHouse())
        assertEquals(6, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("Village_Sell"))
        assertTrue(categories.contains("VillageHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchCottageVillageHouseSell() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                villageType = setOf(Filter.VillageType.COTTAGE)
            )
        )
        assertEquals(7, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("Village_Sell"))
        assertTrue(categories.contains("Cottage_VillageHouse_Sell"))
        assertTrue(categories.contains("VillageHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchTownhouseVillageHouseSell() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                villageType = setOf(Filter.VillageType.TOWNHOUSE)
            )
        )
        assertEquals(7, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("Village_Sell"))
        assertTrue(categories.contains("Townhouse_VillageHouse_Sell"))
        assertTrue(categories.contains("VillageHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchLandVillageHouseSell() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                villageType = setOf(Filter.VillageType.LAND)
            )
        )
        assertEquals(7, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("Village_Sell"))
        assertTrue(categories.contains("Land_VillageHouse_Sell"))
        assertTrue(categories.contains("VillageHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchTwoVillageTypesHouseSell() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                villageType = setOf(Filter.VillageType.LAND, Filter.VillageType.COTTAGE)
            )
        )
        assertEquals(8, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("Village_Sell"))
        assertTrue(categories.contains("Cottage_VillageHouse_Sell"))
        assertTrue(categories.contains("Land_VillageHouse_Sell"))
        assertTrue(categories.contains("VillageHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchThreeVillageTypesHouseSell() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                villageType = setOf(
                    Filter.VillageType.LAND,
                    Filter.VillageType.COTTAGE,
                    Filter.VillageType.TOWNHOUSE
                )
            )
        )
        assertEquals(9, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("House_Sell"))
        assertTrue(categories.contains("Village_Sell"))
        assertTrue(categories.contains("Cottage_VillageHouse_Sell"))
        assertTrue(categories.contains("Townhouse_VillageHouse_Sell"))
        assertTrue(categories.contains("Land_VillageHouse_Sell"))
        assertTrue(categories.contains("VillageHouse_Sell"))
        assertTrue(categories.contains("House"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchRoomSell() {
        val categories = metricaEventHelper.getCategories(Filter.SellRoom())
        assertEquals(4, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Room_Sell"))
        assertTrue(categories.contains("Room"))
        assertTrue(categories.contains("Flat_Sell, Room_Sell"))
    }

    @Test
    fun testSearchMixedLotSell() {
        val categories = metricaEventHelper.getCategories(Filter.SellLot())
        assertEquals(4, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Lot_Sell"))
        assertTrue(categories.contains("MixedLot_Sell"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchSecondaryLotSell() {
        val categories =
            metricaEventHelper.getCategories(Filter.SellLot(market = Filter.Market.SECONDARY))
        assertEquals(4, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Lot_Sell"))
        assertTrue(categories.contains("SecondaryLot_Sell"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchVillageLotSell() {
        val categories = metricaEventHelper.getCategories(Filter.VillageLot())
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Lot_Sell"))
        assertTrue(categories.contains("Village_Sell"))
        assertTrue(categories.contains("VillageLot_Sell"))
        assertTrue(categories.contains("Lot_Sell, House_Sell"))
    }

    @Test
    fun testSearchFlatLongRent() {
        val filter = Filter.RentApartment(rentTime = Filter.RentTime.LARGE)
        val categories = metricaEventHelper.getCategories(filter)
        assertEquals(6, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("LongRent"))
        assertTrue(categories.contains("Flat_Rent"))
        assertTrue(categories.contains("Flat_LongRent"))
        assertTrue(categories.contains("Flat_LongRent_Realty"))
        assertTrue(categories.contains("Flat"))
    }

    @Test
    fun testSearchYandexRentFlatLongRent() {
        val filter = Filter.RentApartment(rentTime = Filter.RentTime.LARGE, yandexRent = true)
        val categories = metricaEventHelper.getCategories(filter)
        assertEquals(6, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("LongRent"))
        assertTrue(categories.contains("Flat_Rent"))
        assertTrue(categories.contains("Flat_LongRent"))
        assertTrue(categories.contains("Flat_LongRent_Arenda"))
        assertTrue(categories.contains("Flat"))
    }

    @Test
    fun testSearchFlatDailyRent() {
        val categories = metricaEventHelper.getCategories(
            Filter.RentApartment(
                rentTime = Filter.RentTime.SHORT
            )
        )
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("DailyRent"))
        assertTrue(categories.contains("Flat_Rent"))
        assertTrue(categories.contains("Flat_DailyRent"))
        assertTrue(categories.contains("Flat"))
    }

    @Test
    fun testSearchHouseLongRent() {
        val categories = metricaEventHelper.getCategories(
            Filter.RentHouse(
                rentTime = Filter.RentTime.LARGE
            )
        )
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("LongRent"))
        assertTrue(categories.contains("House_Rent"))
        assertTrue(categories.contains("House_LongRent"))
        assertTrue(categories.contains("House"))
    }

    @Test
    fun testSearchHouseDailyRent() {
        val categories = metricaEventHelper.getCategories(
            Filter.RentHouse(
                rentTime = Filter.RentTime.SHORT
            )
        )
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("DailyRent"))
        assertTrue(categories.contains("House_Rent"))
        assertTrue(categories.contains("House_DailyRent"))
        assertTrue(categories.contains("House"))
    }

    @Test
    fun testSearchRoomLongRent() {
        val categories = metricaEventHelper.getCategories(
            Filter.RentRoom(
                rentTime = Filter.RentTime.LARGE
            )
        )
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("LongRent"))
        assertTrue(categories.contains("Room_Rent"))
        assertTrue(categories.contains("Room_LongRent"))
        assertTrue(categories.contains("Room"))
    }

    @Test
    fun testSearchRoomDailyRent() {
        val categories = metricaEventHelper.getCategories(
            Filter.RentRoom(
                rentTime = Filter.RentTime.SHORT
            )
        )
        assertEquals(5, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("DailyRent"))
        assertTrue(categories.contains("Room_Rent"))
        assertTrue(categories.contains("Room_DailyRent"))
        assertTrue(categories.contains("Room"))
    }

    @Test
    fun testSearchBuyCommercial() {
        val categories = metricaEventHelper.getCategories(Filter.SellCommercial())
        assertEquals(3, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Commercial"))
        assertTrue(categories.contains("Commercial_Sell"))
    }

    @Test
    fun testSearchRentCommercial() {
        val categories = metricaEventHelper.getCategories(Filter.RentCommercial())
        assertEquals(3, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("Commercial"))
        assertTrue(categories.contains("Commercial_Rent"))
    }

    @Test
    fun testSearchBuyGarage() {
        val categories = metricaEventHelper.getCategories(Filter.SellGarage())
        assertEquals(3, categories.size)
        assertTrue(categories.contains("Sell"))
        assertTrue(categories.contains("Garage"))
        assertTrue(categories.contains("Garage_Sell"))
    }

    @Test
    fun testSearchRentGarage() {
        val categories = metricaEventHelper.getCategories(Filter.RentGarage())
        assertEquals(3, categories.size)
        assertTrue(categories.contains("Rent"))
        assertTrue(categories.contains("Garage"))
        assertTrue(categories.contains("Garage_Rent"))
    }

    @Test
    fun testSearchParkTypeForest() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                parkType = setOf(Filter.ParkType.FOREST)
            )
        )
        assertTrue(categories.contains("ParkTypeForest"))
    }

    @Test
    fun testSearchParkTypePark() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                parkType = setOf(Filter.ParkType.PARK)
            )
        )
        assertTrue(categories.contains("ParkTypePark"))
    }

    @Test
    fun testSearchParkTypeGarden() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                parkType = setOf(Filter.ParkType.GARDEN)
            )
        )
        assertTrue(categories.contains("ParkTypeGarden"))
    }

    @Test
    fun testSearchParkTypesAll() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                parkType = setOf(
                    Filter.ParkType.FOREST,
                    Filter.ParkType.GARDEN,
                    Filter.ParkType.PARK
                )
            )
        )
        assertTrue(categories.contains("ParkTypeForest, ParkTypeGarden, ParkTypePark"))
    }

    @Test
    fun testSearchPondTypeSea() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                pondType = setOf(Filter.PondType.SEA)
            )
        )
        assertTrue(categories.contains("PondTypeSea"))
    }

    @Test
    fun testSearchPondTypeBay() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                pondType = setOf(Filter.PondType.BAY)
            )
        )
        assertTrue(categories.contains("PondTypeBay"))
    }

    @Test
    fun testSearchPondTypeStrait() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                pondType = setOf(Filter.PondType.STRAIT)
            )
        )
        assertTrue(categories.contains("PondTypeStrait"))
    }

    @Test
    fun testSearchPondTypePond() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                pondType = setOf(Filter.PondType.POND)
            )
        )
        assertTrue(categories.contains("PondTypePond"))
    }

    @Test
    fun testSearchPondTypeRiver() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                pondType = setOf(Filter.PondType.RIVER)
            )
        )
        assertTrue(categories.contains("PondTypeRiver"))
    }

    @Test
    fun testSearchPondTypesAll() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                pondType = setOf(
                    Filter.PondType.SEA,
                    Filter.PondType.BAY,
                    Filter.PondType.STRAIT,
                    Filter.PondType.LAKE,
                    Filter.PondType.POND,
                    Filter.PondType.RIVER
                )
            )
        )
        assertTrue(
            categories.contains(
                "PondTypeSea, PondTypeBay, PondTypeStrait, PondTypeLake, " +
                    "PondTypePond, PondTypeRiver"
            )
        )
    }

    @Test
    fun testSearchVillageParkTypeForest() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                parkType = setOf(Filter.ParkType.FOREST)
            )
        )
        assertTrue(categories.contains("ParkTypeForest"))
    }

    @Test
    fun testSearchVillageParkTypePark() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                parkType = setOf(Filter.ParkType.PARK)
            )
        )
        assertTrue(categories.contains("ParkTypePark"))
    }

    @Test
    fun testSearchVillageParkTypeGarden() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                parkType = setOf(Filter.ParkType.GARDEN)
            )
        )
        assertTrue(categories.contains("ParkTypeGarden"))
    }

    @Test
    fun testSearchVillageParkTypesAll() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                parkType = setOf(
                    Filter.ParkType.FOREST,
                    Filter.ParkType.GARDEN,
                    Filter.ParkType.PARK
                )
            )
        )
        assertTrue(categories.contains("ParkTypeForest, ParkTypeGarden, ParkTypePark"))
    }

    @Test
    fun testSearchVillagePondTypeSea() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                pondType = setOf(Filter.PondType.SEA)
            )
        )
        assertTrue(categories.contains("PondTypeSea"))
    }

    @Test
    fun testSearchVillagePondTypeBay() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                pondType = setOf(Filter.PondType.BAY)
            )
        )
        assertTrue(categories.contains("PondTypeBay"))
    }

    @Test
    fun testSearchVillagePondTypeStrait() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                pondType = setOf(Filter.PondType.STRAIT)
            )
        )
        assertTrue(categories.contains("PondTypeStrait"))
    }

    @Test
    fun testSearchVillagePondTypePond() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                pondType = setOf(Filter.PondType.POND)
            )
        )
        assertTrue(categories.contains("PondTypePond"))
    }

    @Test
    fun testSearchVillagePondTypeRiver() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                pondType = setOf(Filter.PondType.RIVER)
            )
        )
        assertTrue(categories.contains("PondTypeRiver"))
    }

    @Test
    fun testSearchVillagePondTypesAll() {
        val categories = metricaEventHelper.getCategories(
            Filter.VillageHouse(
                pondType = setOf(
                    Filter.PondType.SEA,
                    Filter.PondType.BAY,
                    Filter.PondType.STRAIT,
                    Filter.PondType.LAKE,
                    Filter.PondType.POND,
                    Filter.PondType.RIVER
                )
            )
        )
        assertTrue(
            categories.contains(
                "PondTypeSea, PondTypeBay, PondTypeStrait, PondTypeLake, " +
                    "PondTypePond, PondTypeRiver"
            )
        )
    }

    @Test
    fun testSearchExpectMetro() {
        val categories = metricaEventHelper.getCategories(Filter.SellApartment(expectMetro = true))
        assertTrue(categories.contains("ExpectMetro"))
    }

    @Test
    fun testSearchTagsInclude() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                includeTags = setOf(SearchTag(42, "Tag"))
            )
        )
        assertTrue(categories.contains("TagsInclude"))
    }

    @Test
    fun testSearchTagsExclude() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                excludeTags = setOf(SearchTag(42, "Tag"))
            )
        )
        assertTrue(categories.contains("TagsExclude"))
    }

    @Test
    fun testSearchNonGrandmotherRenovation() {
        val categories = metricaEventHelper.getCategories(
            Filter.SellApartment(
                renovation = setOf(Filter.Renovation.NON_GRANDMOTHER)
            )
        )
        assertTrue(categories.contains("NonGrandmotherRenovation"))
    }
}
