package com.yandex.mobile.realty.ui.advertising.listing

import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.ui.advertising.getAdContextTag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * @author shpigun on 2019-11-21
 */
class AdContextTagTest {

    @Test
    fun testAdContextTagEmptyBuyApartment() {
        val filter = Filter.SellApartment()
        val expectedTag = "Купить квартиру"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyRentApartment() {
        val filter = Filter.RentApartment()
        val expectedTag = "Снять квартиру в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptySiteApartment() {
        val filter = Filter.SiteApartment()
        val expectedTag = "Купить квартиру в новостройке"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyBuyRoom() {
        val filter = Filter.SellRoom()
        val expectedTag = "Купить комнату"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyRentRoom() {
        val filter = Filter.RentRoom()
        val expectedTag = "Снять комнату в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyBuyHouse() {
        val filter = Filter.SellHouse()
        val expectedTag = "Купить дом"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyRentHouse() {
        val filter = Filter.RentHouse()
        val expectedTag = "Снять дом в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyVillageHouse() {
        val filter = Filter.VillageHouse()
        val expectedTag = "Купить дом в коттеджном поселке"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyBuyLot() {
        val filter = Filter.SellLot()
        val expectedTag = "Купить участок"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyVillageLot() {
        val filter = Filter.VillageLot()
        val expectedTag = "Купить участок в коттеджном поселке"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyBuyGarage() {
        val filter = Filter.SellGarage()
        val expectedTag = "Купить гараж"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyRentGarage() {
        val filter = Filter.RentGarage()
        val expectedTag = "Снять гараж"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyBuyCommercial() {
        val filter = Filter.SellCommercial()
        val expectedTag = "Купить коммерческую недвижимость"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagEmptyRentCommercial() {
        val filter = Filter.RentCommercial()
        val expectedTag = "Снять коммерческую недвижимость в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagBuyApartmentWithRoomsCount() {
        val filter = Filter.SellApartment(
            roomsCount = EnumSet.allOf(Filter.RoomsCount::class.java)
        )
        val expectedTag = "Купить однокомнатную двухкомнатную трехкомнатную четырехкомнатную " +
            "квартиру студию"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentApartmentWithRoomsCount() {
        val filter = Filter.RentApartment(
            roomsCount = EnumSet.allOf(Filter.RoomsCount::class.java)
        )
        val expectedTag = "Снять однокомнатную двухкомнатную трехкомнатную четырехкомнатную " +
            "квартиру студию в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagSiteApartmentWithRoomsCount() {
        val filter = Filter.SiteApartment(
            roomsCount = EnumSet.allOf(Filter.RoomsCount::class.java)
        )
        val expectedTag = "Купить однокомнатную двухкомнатную трехкомнатную четырехкомнатную " +
            "квартиру студию в новостройке"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentApartmentWithLargeRentTime() {
        val filter = Filter.RentApartment(
            rentTime = Filter.RentTime.LARGE
        )
        val expectedTag = "Снять квартиру в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentApartmentWithShortRentTime() {
        val filter = Filter.RentApartment(
            rentTime = Filter.RentTime.SHORT
        )
        val expectedTag = "Снять квартиру посуточно"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentRoomWithLargeRentTime() {
        val filter = Filter.RentRoom(
            rentTime = Filter.RentTime.LARGE
        )
        val expectedTag = "Снять комнату в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentRoomWithShortRentTime() {
        val filter = Filter.RentRoom(
            rentTime = Filter.RentTime.SHORT
        )
        val expectedTag = "Снять комнату посуточно"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentHouseWithLargeRentTime() {
        val filter = Filter.RentHouse(
            rentTime = Filter.RentTime.LARGE
        )
        val expectedTag = "Снять дом в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentHouseWithShortRentTime() {
        val filter = Filter.RentHouse(
            rentTime = Filter.RentTime.SHORT
        )
        val expectedTag = "Снять дом посуточно"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagBuyHouseWithHouseTypes() {
        val filter = Filter.SellHouse(
            houseType = EnumSet.allOf(Filter.HouseType::class.java)
        )
        val expectedTag = "Купить таунхаус дуплекс часть дома дом"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentHouseWithHouseTypes() {
        val filter = Filter.RentHouse(
            houseType = EnumSet.allOf(Filter.HouseType::class.java)
        )
        val expectedTag = "Снять таунхаус дуплекс часть дома дом в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagVillageHouseWithHouseTypes() {
        val filter = Filter.VillageHouse(
            villageType = EnumSet.allOf(Filter.VillageType::class.java)
        )
        val expectedTag = "Купить дом таунхаус участок в коттеджном поселке"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagBuyCommercialWithCommercialTypes() {
        val filter = Filter.SellCommercial(
            commercialType = EnumSet.allOf(Filter.CommercialType::class.java)
        )
        val expectedTag = "Купить коммерческую недвижимость офис торговое помещение помещение " +
            "свободного назначения склад общепит гостиницу автосервис производственное " +
            "помещение юридический адрес участок коммерческого назначения готовый бизнес"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentCommercialWithCommercialTypes() {
        val filter = Filter.RentCommercial(
            commercialType = EnumSet.allOf(Filter.CommercialType::class.java)
        )
        val expectedTag = "Снять коммерческую недвижимость офис торговое помещение помещение " +
            "свободного назначения склад общепит гостиницу автосервис производственное " +
            "помещение юридический адрес участок коммерческого назначения готовый " +
            "бизнес в аренду"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagSellGarageWithGarageTypes() {
        val filter = Filter.SellGarage(
            garageType = EnumSet.allOf(Filter.GarageType::class.java)
        )
        val expectedTag = "Купить бокс гараж машиноместо"
        assertEquals(expectedTag, filter.getAdContextTag())
    }

    @Test
    fun testAdContextTagRentGarageWithGarageTypes() {
        val filter = Filter.RentGarage(
            garageType = EnumSet.allOf(Filter.GarageType::class.java)
        )
        val expectedTag = "Снять бокс гараж машиноместо"
        assertEquals(expectedTag, filter.getAdContextTag())
    }
}
