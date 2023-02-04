package com.yandex.mobile.realty.data.model

import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.Sell
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author sorokinandrei on 4/15/21.
 */
class OfferPreviewDtoConverterTest {

    @Test
    fun shouldConvertSecondaryFree() {
        val dto = newOfferPreviewDto(
            offerId = "1",
            newFlatSale = false,
            flatType = "SECONDARY",
            hasPaidCalls = false
        )
        val converted = OfferPreviewDto.CONVERTER.map(dto, EmptyDescriptor)
        assertEquals(converted.isPaid, false)
        assertEquals((converted.property as Apartment).isNewFlat, false)
    }

    @Test
    fun shouldConvertPaidSecondary() {
        val dto = newOfferPreviewDto(
            offerId = "1",
            newFlatSale = false,
            flatType = "SECONDARY",
            hasPaidCalls = true
        )
        val converted = OfferPreviewDto.CONVERTER.map(dto, EmptyDescriptor)
        assertEquals(converted.isPaid, true)
        assertEquals((converted.property as Apartment).isNewFlat, false)
    }

    @Test
    fun shouldConvertNewFree() {
        val dto = newOfferPreviewDto(
            offerId = "1",
            newFlatSale = true,
            flatType = "NEW",
            hasPaidCalls = false,
            primarySale = false
        )
        val converted = OfferPreviewDto.CONVERTER.map(dto, EmptyDescriptor)
        assertEquals(converted.isPaid, false)
        assertEquals((converted.property as Apartment).isNewFlat, true)
        assertEquals((converted.deal as Sell).primarySale, false)
    }

    @Test
    fun shouldConvertNewPaid() {
        val dto = newOfferPreviewDto(
            offerId = "1",
            newFlatSale = true,
            flatType = "NEW",
            primarySale = false,
            hasPaidCalls = true
        )
        val converted = OfferPreviewDto.CONVERTER.map(dto, EmptyDescriptor)
        assertEquals(converted.isPaid, true)
        assertEquals((converted.property as Apartment).isNewFlat, true)
        assertEquals((converted.deal as Sell).primarySale, false)
    }

    @Test
    fun shouldConvertNewPrimaryFree() {
        val dto = newOfferPreviewDto(
            offerId = "1",
            newFlatSale = true,
            flatType = "NEW",
            primarySale = false,
            hasPaidCalls = false
        )
        val converted = OfferPreviewDto.CONVERTER.map(dto, EmptyDescriptor)
        assertEquals(converted.isPaid, false)
        assertEquals((converted.property as Apartment).isNewFlat, true)
        assertEquals((converted.deal as Sell).primarySale, false)
    }

    @Test
    fun shouldConvertNewPrimaryPaid() {
        val dto = newOfferPreviewDto(
            offerId = "1",
            newFlatSale = true,
            flatType = "NEW",
            primarySale = true,
            hasPaidCalls = true
        )
        val converted = OfferPreviewDto.CONVERTER.map(dto, EmptyDescriptor)
        assertEquals(converted.isPaid, true)
        assertEquals((converted.property as Apartment).isNewFlat, true)
        assertEquals((converted.deal as Sell).primarySale, true)
    }

    private fun newOfferPreviewDto(
        offerId: String,
        newFlatSale: Boolean? = null,
        flatType: String? = null,
        primarySale: Boolean? = null,
        offerType: String? = "SELL",
        offerCategory: String? = "APARTMENT",
        priceDto: PriceDto = newPriceDto(),
        hasPaidCalls: Boolean? = null,
    ): OfferPreviewDto {
        return OfferPreviewDto(
            offerId = offerId,
            offerType = offerType,
            offerCategory = offerCategory,
            price = PriceInfoDto(priceDto, null, null, null),
            partnerId = null,
            author = null,
            creationDate = null,
            updateDate = null,
            area = null,
            fullImages = null,
            appLargeSnippetImages = null,
            appMiddleSnippetImages = null,
            appSmallSnippetImages = null,
            appMiniSnippetImages = null,
            photoPreviews = null,
            location = null,
            active = null,
            vas = null,
            uid = null,
            floorsTotal = null,
            floorsOffered = null,
            livingSpace = null,
            house = null,
            roomsTotal = null,
            building = null,
            salesDepartments = null,
            roomsOffered = null,
            lot = null,
            commercial = null,
            garage = null,
            village = null,
            extImages = null,
            flatType = flatType,
            primarySaleV2 = primarySale,
            freeReportAccessibility = null,
            remoteReview = null,
            trustedOfferInfo = null,
            apartment = null,
            shareUrl = null,
            chargeForCallsType = null,
            userNote = null,
            yandexRent = null,
            newFlatSale = newFlatSale,
            virtualTours = null,
            offerChatType = null,
            hasPaidCalls = hasPaidCalls,
        )
    }

    private fun newPriceDto(price: Number? = 1_000_000): PriceDto {
        return PriceDto(price, currency = "RUB", priceType = "PER_OFFER", "WHOLE_LIFE")
    }
}
