package com.yandex.mobile.realty.ui.advertising.card

import com.yandex.mobile.realty.domain.model.common.CommercialType
import com.yandex.mobile.realty.domain.model.common.GarageType
import com.yandex.mobile.realty.domain.model.common.HouseType
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.Commercial
import com.yandex.mobile.realty.domain.model.offer.Deal
import com.yandex.mobile.realty.domain.model.offer.FlatType
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
import com.yandex.mobile.realty.ui.advertising.putOfferCardAdRequestParameters
import com.yandex.mobile.realty.ui.advertising.putSiteCardAdRequestParameters
import com.yandex.mobile.realty.ui.advertising.putVillageCardAdRequestParameters
import org.junit.Assert
import org.junit.Test
import java.util.*

/**
 * Created by Alena Malchikhina on 05.03.2020
 */
class AdRequestParamsTest {

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
    private val dealSell = Sell(
        priceInfo = priceInfo,
        primarySale = null
    )
    private val dealRent = Rent(
        priceInfo = priceInfo
    )

    @Test
    fun testAdRequestParamsSellCommercial() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "COMMERCIAL",
            "adf_puid21" to "COMMERCIAL"
        )
        val offer = createCommercialOffer(dealSell)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentCommercial() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "COMMERCIAL",
            "adf_puid21" to "COMMERCIAL"
        )
        val offer = createCommercialOffer(dealRent)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellLot() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "LOT"
        )
        val offer = createLotOffer()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentHouse() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "RENT",
            "adf_puid21" to "HOUSE"
        )
        val offer = createHouseOffer(dealRent)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellHouse() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "HOUSE"
        )
        val offer = createHouseOffer(dealSell)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellGarage() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "GARAGE"
        )
        val offer = createGarageOffer(dealSell)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentGarage() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "RENT",
            "adf_puid21" to "GARAGE"
        )
        val offer = createGarageOffer(dealRent)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellRoom() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "ROOMS"
        )
        val offer = createRoomOffer(dealSell)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentRoom() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "RENT",
            "adf_puid21" to "ROOMS"
        )
        val offer = createRoomOffer(dealRent)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellApartment() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "APARTMENT"
        )
        val offer = createApartment(dealSell, null)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentApartment() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "RENT",
            "adf_puid21" to "APARTMENT"
        )
        val offer = createApartment(dealRent, null)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSiteApartment() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "NEWFLAT",
            "adf_puid21" to "APARTMENT"
        )
        val offer = createApartment(dealSell, FlatType.NEW_FLAT)
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putOfferCardAdRequestParameters(offer)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSite() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "NEWFLAT",
            "adf_puid21" to "APARTMENT"
        )
        val siteDetail = SitePreviewImpl(
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
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSiteCardAdRequestParameters(siteDetail)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsVillage() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbj",
            "adf_puid10" to "NEWFLAT",
            "adf_puid21" to "HOUSE"
        )
        val villageDetail = VillagePreviewImpl(
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
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putVillageCardAdRequestParameters(villageDetail)
        Assert.assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    private fun createLotOffer(): OfferPreview {
        val property = Lot(
            area = null,
            lotType = null,
            villageInfo = null
        )
        return createOffer(dealSell, property)
    }

    private fun createApartment(deal: Deal, flatType: FlatType?): OfferPreview {
        val property = Apartment(
            area = null,
            roomsCount = null,
            livingArea = null,
            floor = null,
            flatType = flatType,
            siteInfo = null,
            floorsCount = null,
            decoration = null,
            houseName = null,
            builtYear = null,
            builtQuarter = null,
            buildingState = null,
            isNewFlat = flatType == FlatType.NEW_FLAT
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

    private fun createHouseOffer(deal: Deal): OfferPreview {
        val property = House(
            area = null,
            houseType = HouseType.HOUSE,
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

    private fun createGarageOffer(deal: Deal): OfferPreview {
        val property = Garage(
            area = null,
            garageType = GarageType.GARAGE,
            parkingType = null
        )
        return createOffer(deal, property)
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
