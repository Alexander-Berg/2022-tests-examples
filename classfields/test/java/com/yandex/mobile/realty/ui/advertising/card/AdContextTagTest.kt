package com.yandex.mobile.realty.ui.advertising.card

import com.yandex.mobile.realty.domain.model.common.CommercialType
import com.yandex.mobile.realty.domain.model.common.ExactRoomsCount
import com.yandex.mobile.realty.domain.model.common.GarageType
import com.yandex.mobile.realty.domain.model.common.HouseType
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.Rooms
import com.yandex.mobile.realty.domain.model.common.Studio
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.Commercial
import com.yandex.mobile.realty.domain.model.offer.Deal
import com.yandex.mobile.realty.domain.model.offer.Garage
import com.yandex.mobile.realty.domain.model.offer.House
import com.yandex.mobile.realty.domain.model.offer.Lot
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Property
import com.yandex.mobile.realty.domain.model.offer.Rent
import com.yandex.mobile.realty.domain.model.offer.Room
import com.yandex.mobile.realty.domain.model.offer.Sell
import com.yandex.mobile.realty.domain.model.site.SitePreviewImpl
import com.yandex.mobile.realty.domain.model.village.VillagePreviewImpl
import com.yandex.mobile.realty.ui.advertising.getAdContextTag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * Created by Alena Malchikhina on 04.03.2020
 */
class AdContextTagTest {

    private val price = Price(
        0,
        Price.Unit.PER_OFFER,
        Price.Currency.RUB,
        Price.Period.PER_MONTH
    )
    private val priceInfo = PriceInfo(
        price = price,
        pricePerOffer = price,
        pricePerSquare = null,
        trend = Trend.UNCHANGED
    )
    private val detailedSell = Sell(
        priceInfo = priceInfo,
        primarySale = null
    )

    @Test
    fun testAdContextTagOfferBuyApartment() {
        val offer = createApartmentWithRoomsOffer(null, detailedSell)
        val expectedTag = "Купить квартиру"
        assertEquals(expectedTag, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentApartment() {
        val offer = createApartmentWithRoomsOffer(null, createDetailedRent())
        val expectedTag = "Снять квартиру"
        assertEquals(expectedTag, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyApartmentWith4Rooms() {
        val offer = createApartmentWithRoomsOffer(ExactRoomsCount(4), detailedSell)
        val expected = "Купить четырехкомнатную квартиру"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentApartmentWith4Rooms() {
        val offer = createApartmentWithRoomsOffer(ExactRoomsCount(1), createDetailedRent())
        val expected = "Снять однокомнатную квартиру"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyApartmentWithManyRooms() {
        val offer = createApartmentWithRoomsOffer(ExactRoomsCount(5), detailedSell)
        val expected = "Купить многокомнатную квартиру"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentApartmentWithManyRooms() {
        val offer = createApartmentWithRoomsOffer(ExactRoomsCount(10), createDetailedRent())
        val expected = "Снять многокомнатную квартиру"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyApartmentStudio() {
        val offer = createApartmentWithRoomsOffer(Studio, detailedSell)
        val expected = "Купить квартиру студию"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentApartmentStudio() {
        val offer = createApartmentWithRoomsOffer(Studio, createDetailedRent())
        val expected = "Снять квартиру студию"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyRoom() {
        val offer = createRoomOffer(detailedSell)
        val expected = "Купить комнату"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentRoomLongPeriod() {
        val offer = createRoomOffer(createDetailedRent())
        val expected = "Снять комнату в аренду"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentRoomShortPeriod() {
        val offer = createRoomOffer(createDetailedRent(Price.Period.PER_DAY))
        val expected = "Снять комнату посуточно"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyTownHouse() {
        val offer = createHouseOffer(detailedSell, HouseType.TOWNHOUSE)
        val expected = "Купить таунхаус"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyPartHouse() {
        val offer = createHouseOffer(detailedSell, HouseType.PARTHOUSE)
        val expected = "Купить часть дома"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentHouse() {
        val offer = createHouseOffer(createDetailedRent(), HouseType.HOUSE)
        val expected = "Снять дом в аренду"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentDuplex() {
        val detailedRent = createDetailedRent(Price.Period.PER_DAY)
        val offer = createHouseOffer(detailedRent, HouseType.DUPLEX)
        val expected = "Снять дуплекс посуточно"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyLot() {
        val property = Lot(
            area = null,
            lotType = null,
            villageInfo = null
        )
        val offer = createOffer(detailedSell, property)
        val expected = "Купить участок"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyCommercial() {
        val offer = createCommercialOffer(detailedSell)
        val expected = "Купить коммерческую недвижимость торговое помещение помещение свободного " +
            "назначения общепит автосервис готовый бизнес юридический адрес " +
            "участок коммерческого назначения гостиницу офис склад производственное помещение"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentCommercial() {
        val offer = createCommercialOffer(createDetailedRent())
        val expected = "Снять коммерческую недвижимость торговое помещение помещение свободного " +
            "назначения общепит автосервис готовый бизнес юридический адрес участок " +
            "коммерческого назначения гостиницу офис склад производственное помещение в аренду"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentDailyCommercial() {
        val offer = createCommercialOffer(createDetailedRent(Price.Period.PER_DAY))
        val expected = "Снять коммерческую недвижимость торговое помещение помещение свободного " +
            "назначения общепит автосервис готовый бизнес юридический адрес участок " +
            "коммерческого назначения гостиницу офис склад производственное помещение посуточно"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentGarage() {
        val offer = createGarageOffer(createDetailedRent(), GarageType.GARAGE)
        val expected = "Снять гараж"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferBuyParkingPlace() {
        val offer = createGarageOffer(detailedSell, GarageType.PARKING_PLACE)
        val expected = "Купить машиноместо"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagOfferRentBox() {
        val offer = createGarageOffer(createDetailedRent(), GarageType.BOX)
        val expected = "Снять бокс"
        assertEquals(expected, offer.getAdContextTag())
    }

    @Test
    fun testAdContextTagSiteBuy() {
        val site = SitePreviewImpl(
            id = "1",
            name = "ЖК жк",
            fullName = "Жилой комплекс",
            locativeFullName = null,
            developers = null,
            buildingClass = null,
            type = null,
            images = null,
            price = null,
            pricePerMeter = null,
            totalOffers = null,
            locationInfo = null,
            commissioningStatus = null,
            deliveryDates = null,
            flatStatus = null,
            salesClosed = null,
            specialProposalLabels = null,
            housingType = null,
            shareUrl = null,
            isPaid = false,
            isExtended = false,
            hasDeveloperChat = false,
            briefRoomsStats = null,
        )
        val expected = "Купить квартиру в новостройке"
        assertEquals(expected, site.getAdContextTag())
    }

    @Test
    fun testAdContextTagVillageBuy() {
        val village = VillagePreviewImpl(
            id = "1",
            name = "Киров",
            fullName = "Киров",
            developers = null,
            villageClass = null,
            type = null,
            images = null,
            price = null,
            locationInfo = null,
            commissioningState = null,
            filteredStats = null,
            shareUrl = null,
            isPaid = false
        )
        val expected = "Купить дом в коттеджном поселке"
        assertEquals(expected, village.getAdContextTag())
    }

    private fun createApartmentWithRoomsOffer(rooms: Rooms?, deal: Deal): OfferPreview {
        val property = Apartment(
            area = null,
            roomsCount = rooms,
            livingArea = null,
            floor = null,
            flatType = null,
            siteInfo = null,
            floorsCount = null,
            decoration = null,
            houseName = null,
            builtYear = null,
            builtQuarter = null,
            buildingState = null,
            isNewFlat = false
        )
        return createOffer(deal, property)
    }

    private fun createRoomOffer(deal: Deal): OfferPreview {
        val property = Room(
            area = null,
            floorsCount = null,
            floor = null,
            livingArea = null,
            roomsCount = null,
            roomsOffered = null,
            buildingState = null,
            houseName = null,
            builtYear = null,
            builtQuarter = null
        )
        return createOffer(deal, property)
    }

    private fun createHouseOffer(deal: Deal, houseType: HouseType): OfferPreview {
        val property = House(
            area = null,
            houseType = houseType,
            lotArea = null,
            lotType = null,
            villageInfo = null
        )
        return createOffer(deal, property)
    }

    private fun createCommercialOffer(deal: Deal): OfferPreview {
        val property = Commercial(
            area = null,
            commercialTypes = EnumSet.allOf(CommercialType::class.java)
        )
        return createOffer(deal, property)
    }

    private fun createGarageOffer(deal: Deal, garageType: GarageType): OfferPreview {
        val property = Garage(
            area = null,
            garageType = garageType,
            parkingType = null
        )
        return createOffer(deal, property)
    }

    private fun createDetailedRent(period: Price.Period = Price.Period.PER_MONTH): Deal {
        val price = Price(
            value = 0,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = period
        )
        val priceInfo = PriceInfo(
            price = price,
            pricePerOffer = price,
            pricePerSquare = null,
            trend = Trend.UNCHANGED
        )
        return Rent(priceInfo = priceInfo)
    }

    private fun createOffer(
        deal: Deal,
        property: Property
    ): OfferPreview {
        return OfferPreviewImpl(
            id = "",
            partnerId = null,
            author = null,
            createdAt = null,
            updateDate = null,
            images = null,
            locationInfo = null,
            active = null,
            vas = null,
            uid = null,
            excerptFreeReportAccessible = null,
            onlineShow = null,
            videoId = null,
            deal = deal,
            property = property,
            isFullTrustedOwner = null,
            shareUrl = null,
            isPaid = false,
            isExtended = false,
            note = null,
            virtualTour = null,
            chatInfo = null,
        )
    }
}
