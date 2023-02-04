package com.yandex.mobile.realty.ui.offer

import com.yandex.mobile.realty.domain.model.common.Author
import com.yandex.mobile.realty.domain.model.common.Location
import com.yandex.mobile.realty.domain.model.common.LocationInfo
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.RealtyPoint
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.geo.GeoPoint
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Rent
import com.yandex.mobile.realty.domain.model.offer.Sell
import com.yandex.mobile.realty.ui.presenter.isTargetForMosRu
import org.junit.Assert
import org.junit.Test

/**
 * @author solovevai on 04.09.2020.
 */
class AuthorDegradationTest {

    @Test
    fun testAuthorBlockIsSuitableForDegradation() {
        val subjectFederationId = 1
        val authorCategory = Author.Category.OWNER
        val offer = createSellApartmentOffer(subjectFederationId, authorCategory)
        Assert.assertTrue(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationSubjectFederationId() {
        val subjectFederationId = 2
        val authorCategory = Author.Category.OWNER
        val offer = createSellApartmentOffer(subjectFederationId, authorCategory)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationAgent() {
        val subjectFederationId = 1
        val authorCategory = Author.Category.AGENT
        val offer = createSellApartmentOffer(subjectFederationId, authorCategory)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationPrivateAgent() {
        val subjectFederationId = 1
        val authorCategory = Author.Category.PRIVATE_AGENT
        val offer = createSellApartmentOffer(subjectFederationId, authorCategory)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationAgency() {
        val subjectFederationId = 1
        val authorCategory = Author.Category.AGENCY
        val offer = createSellApartmentOffer(subjectFederationId, authorCategory)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationDeveloper() {
        val subjectFederationId = 1
        val authorCategory = Author.Category.AGENCY
        val offer = createSellApartmentOffer(subjectFederationId, authorCategory)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsSuitableForDegradationRentLong() {
        val subjectFederationId = 1
        val authorCategory = Author.Category.OWNER
        val offer = createRentApartmentOffer(subjectFederationId, authorCategory, rentLong)
        Assert.assertTrue(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationRentShort() {
        val subjectFederationId = 1
        val authorCategory = Author.Category.OWNER
        val offer = createRentApartmentOffer(subjectFederationId, authorCategory, rentShort)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationEmptySubjectFederationId() {
        val subjectFederationId = null
        val authorCategory = Author.Category.OWNER
        val offer = createRentApartmentOffer(subjectFederationId, authorCategory, rentLong)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun testAuthorBlockIsNotSuitableForDegradationEmptyAuthorCategory() {
        val subjectFederationId = 1
        val authorCategory = null
        val offer = createRentApartmentOffer(subjectFederationId, authorCategory, rentLong)
        Assert.assertFalse(offer.isTargetForMosRu())
    }

    private fun createSellApartmentOffer(
        subjectFederationId: Int?,
        category: Author.Category?
    ): OfferPreview {
        return OfferPreviewImpl(
            id = "",
            partnerId = null,
            author = createAuthor(category),
            createdAt = null,
            updateDate = null,
            images = null,
            locationInfo = createLocationInfo(subjectFederationId),
            active = null,
            vas = null,
            uid = null,
            excerptFreeReportAccessible = null,
            onlineShow = null,
            videoId = null,
            deal = sell,
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

    private fun createRentApartmentOffer(
        subjectFederationId: Int?,
        category: Author.Category?,
        dealRent: Rent
    ): OfferPreview {
        return OfferPreviewImpl(
            id = "",
            partnerId = null,
            author = createAuthor(category),
            createdAt = null,
            updateDate = null,
            images = null,
            locationInfo = createLocationInfo(subjectFederationId),
            active = null,
            vas = null,
            uid = null,
            excerptFreeReportAccessible = null,
            onlineShow = null,
            videoId = null,
            deal = dealRent,
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

    private fun createLocationInfo(subjectFederationId: Int?): LocationInfo {
        val point = RealtyPoint(GeoPoint(0.0, 0.0), null)
        val location = Location(1L, subjectFederationId, null, null, point, null, null, null)
        return LocationInfo(
            location,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )
    }

    private fun createAuthor(category: Author.Category?): Author {
        return Author(category, null, null, null, null, null)
    }

    private val price = Price(
        0,
        Price.Unit.PER_OFFER,
        Price.Currency.RUB,
        Price.Period.PER_MONTH
    )
    private val pricePerDay = Price(
        0,
        Price.Unit.PER_OFFER,
        Price.Currency.RUB,
        Price.Period.PER_DAY
    )
    private val priceInfo = PriceInfo(
        price = price,
        pricePerOffer = price,
        pricePerSquare = null,
        trend = Trend.UNCHANGED
    )
    private val sell = Sell(
        priceInfo = priceInfo,
        primarySale = null
    )
    private val rentLong = Rent(
        priceInfo = priceInfo
    )
    private val rentShort = Rent(
        priceInfo = PriceInfo(
            price = pricePerDay,
            pricePerOffer = pricePerDay,
            pricePerSquare = null,
            trend = Trend.UNCHANGED
        )
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
}
