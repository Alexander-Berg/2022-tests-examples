package com.yandex.mobile.realty.ui.offer

import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.Commercial
import com.yandex.mobile.realty.domain.model.offer.Deal
import com.yandex.mobile.realty.domain.model.offer.Garage
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Property
import com.yandex.mobile.realty.domain.model.offer.Rent
import com.yandex.mobile.realty.domain.model.offer.Room
import com.yandex.mobile.realty.domain.model.offer.Sell
import com.yandex.mobile.realty.ui.presenter.shouldShowDocumentsSection
import org.junit.Assert
import org.junit.Test

/**
 * @author pvl-zolotov on 24.03.2022
 */
class DocumentsTest {

    @Test
    fun shouldShowDocumentsRentApartment() {
        val offer = createRentApartmentOffer()
        Assert.assertTrue(offer.shouldShowDocumentsSection())
    }

    @Test
    fun shouldNotShowDocumentsSellNewApartment() {
        val offer = createSellOffer(sellPrimary, apartment)
        Assert.assertFalse(offer.shouldShowDocumentsSection())
    }

    @Test
    fun shouldNotShowDocumentsSellBcOffer() {
        val offer = createSellOffer(sellSecondary, commercial)
        Assert.assertFalse(offer.shouldShowDocumentsSection())
    }

    @Test
    fun shouldShowDocumentsSellGarageOffer() {
        val offer = createSellOffer(sell, garage)
        Assert.assertTrue(offer.shouldShowDocumentsSection())
    }

    @Test
    fun shouldShowDocumentsSellRoomOffer() {
        val offer = createSellOffer(sell, room)
        Assert.assertTrue(offer.shouldShowDocumentsSection())
    }

    private fun createSellOffer(deal: Deal, property: Property): OfferPreview {
        return OfferPreviewImpl(
            id = "",
            partnerId = null,
            author = null,
            createdAt = null,
            updateDate = null,
            images = null,
            locationInfo = null,
            active = true,
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

    private fun createRentApartmentOffer(): OfferPreview {
        return OfferPreviewImpl(
            id = "",
            partnerId = null,
            author = null,
            createdAt = null,
            updateDate = null,
            images = null,
            locationInfo = null,
            active = true,
            vas = null,
            uid = null,
            excerptFreeReportAccessible = null,
            onlineShow = null,
            videoId = null,
            deal = rentLong,
            property = apartment,
            isFullTrustedOwner = null,
            shareUrl = null,
            isPaid = false,
            isExtended = false,
            note = null,
            virtualTour = null,
            chatInfo = null,
        )
    }

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
    private val sellSecondary = Sell(
        priceInfo = priceInfo,
        primarySale = false
    )
    private val sellPrimary = Sell(
        priceInfo = priceInfo,
        primarySale = true
    )
    private val sell = Sell(
        priceInfo = priceInfo,
        primarySale = null
    )
    private val rentLong = Rent(
        priceInfo = priceInfo
    )
    private val apartment = Apartment(
        area = null,
        floorsCount = null,
        floor = null,
        livingArea = null,
        roomsCount = null,
        flatType = null,
        siteInfo = null,
        decoration = null,
        houseName = null,
        builtYear = null,
        builtQuarter = null,
        buildingState = null,
        isNewFlat = false
    )
    private val commercial = Commercial(
        area = null,
        commercialTypes = null
    )
    private val garage = Garage(
        area = null,
        garageType = null,
        parkingType = null
    )
    private val room = Room(
        area = null,
        floorsCount = null,
        floor = null,
        livingArea = null,
        roomsCount = null,
        roomsOffered = null,
        houseName = null,
        builtYear = null,
        builtQuarter = null,
        buildingState = null
    )
}
