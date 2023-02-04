package com.yandex.mobile.realty.ui.presenter

import com.yandex.mobile.realty.domain.SUBJECT_ID_MOSCOW
import com.yandex.mobile.realty.domain.model.common.Author
import com.yandex.mobile.realty.domain.model.common.Location
import com.yandex.mobile.realty.domain.model.common.LocationInfo
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.RealtyPoint
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.geo.GeoPoint
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.Deal
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Property
import com.yandex.mobile.realty.domain.model.offer.Rent
import com.yandex.mobile.realty.domain.model.offer.Room
import com.yandex.mobile.realty.domain.model.offer.Sell
import com.yandex.mobile.realty.domain.model.offer.isYandexRent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author rogovalex on 20/02/2021.
 */
class OfferPreviewTest {

    @Test
    fun isFromOwner() {
        assertTrue(newOffer(author = newAuthor(Author.Category.OWNER)).isFromOwner())
    }

    @Test
    fun isNotFromOwnerWhenNoAuthor() {
        assertFalse(newOffer().isFromOwner())
    }

    @Test
    fun isNotFromOwnerWhenNoCategory() {
        assertFalse(newOffer(author = newAuthor()).isFromOwner())
    }

    @Test
    fun isNotFromOwnerWhenFromAgency() {
        assertFalse(newOffer(author = newAuthor(Author.Category.AGENCY)).isFromOwner())
    }

    @Test
    fun isNotFromOwnerWhenFromAgent() {
        assertFalse(newOffer(author = newAuthor(Author.Category.AGENT)).isFromOwner())
    }

    @Test
    fun isNotFromOwnerWhenFromDeveloper() {
        assertFalse(newOffer(author = newAuthor(Author.Category.DEVELOPER)).isFromOwner())
    }

    @Test
    fun isNotFromOwnerFromPrivateAgent() {
        assertFalse(newOffer(author = newAuthor(Author.Category.PRIVATE_AGENT)).isFromOwner())
    }

    @Test
    fun isFromFullTrustedOwner() {
        val offer = newOffer(author = newAuthor(Author.Category.OWNER), isFullTrustedOwner = true)
        assertTrue(offer.isFromFullTrustedOwner())
    }

    @Test
    fun isNotFromFullTrustedOwnerWhenNotFromOwner() {
        val offer = newOffer(author = newAuthor(), isFullTrustedOwner = true)
        assertFalse(offer.isFromFullTrustedOwner())
    }

    @Test
    fun isNotFromFullTrustedOwner() {
        val offer = newOffer(author = newAuthor(Author.Category.OWNER), isFullTrustedOwner = false)
        assertFalse(offer.isFromFullTrustedOwner())
    }

    @Test
    fun isYandexRent() {
        assertTrue(newOffer(newLongRent(true), newApartment()).isYandexRent())
    }

    @Test
    fun isNotYandexRent() {
        assertFalse(newOffer(newLongRent(false), newApartment()).isYandexRent())
    }

    @Test
    fun isNotYandexRentWhenNotApartment() {
        assertFalse(newOffer(newLongRent(true), newRoom()).isYandexRent())
    }

    @Test
    fun isNotYandexRentWhenDailyRent() {
        assertFalse(newOffer(newDailyRent(), newApartment()).isYandexRent())
    }

    @Test
    fun isTargetForMosRuWhenSellApartment() {
        val offer = newOffer(
            newSell(),
            newApartment(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow()
        )
        assertTrue(offer.isTargetForMosRu())
    }

    @Test
    fun isTargetForMosRuWhenLongRentApartment() {
        val offer = newOffer(
            newLongRent(),
            newApartment(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow()
        )
        assertTrue(offer.isTargetForMosRu())
    }

    @Test
    fun isNotTargetForMosRuWhenSellRoom() {
        val offer = newOffer(
            newSell(),
            newRoom(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow()
        )
        assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun isNotTargetForMosRuWhenDailyRentApartment() {
        val offer = newOffer(
            newDailyRent(),
            newApartment(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow()
        )
        assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun isNotTargetForMosRuWhenNotFromOwner() {
        val offer = newOffer(
            newSell(),
            newApartment(),
            null,
            locationInfoMoscow()
        )
        assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun isNotTargetForMosRuWhenNotInMoscow() {
        val offer = newOffer(
            newLongRent(),
            newApartment(),
            newAuthor(Author.Category.OWNER)
        )
        assertFalse(offer.isTargetForMosRu())
    }

    @Test
    fun shouldHideAuthorInfoWhenSellApartment() {
        val offer = newOffer(
            newSell(),
            newApartment(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow(),
            false
        )
        assertTrue(offer.shouldHideAuthorInfo())
    }

    @Test
    fun shouldHideAuthorInfoWhenLongRentApartment() {
        val offer = newOffer(
            newLongRent(),
            newApartment(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow(),
            false
        )
        assertTrue(offer.shouldHideAuthorInfo())
    }

    @Test
    fun shouldNotHideAuthorInfoWhenFullTrustedOwner() {
        val offer = newOffer(
            newLongRent(),
            newApartment(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow(),
            true
        )
        assertFalse(offer.shouldHideAuthorInfo())
    }

    @Test
    fun shouldNotHideAuthorInfoWhenNotFromOwner() {
        val offer = newOffer(
            newLongRent(),
            newApartment(),
            newAuthor(Author.Category.AGENT),
            locationInfoMoscow(),
            false
        )
        assertFalse(offer.shouldHideAuthorInfo())
    }

    @Test
    fun shouldNotHideAuthorInfoWhenNotInMoscow() {
        val offer = newOffer(
            newLongRent(),
            newApartment(),
            newAuthor(Author.Category.AGENT),
            null,
            false
        )
        assertFalse(offer.shouldHideAuthorInfo())
    }

    @Test
    fun shouldNotHideAuthorInfoWhenDailyRentApartment() {
        val offer = newOffer(
            newDailyRent(),
            newApartment(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow(),
            false
        )
        assertFalse(offer.shouldHideAuthorInfo())
    }

    @Test
    fun shouldNotHideAuthorInfoWhenNotApartment() {
        val offer = newOffer(
            newSell(),
            newRoom(),
            newAuthor(Author.Category.OWNER),
            locationInfoMoscow(),
            false
        )
        assertFalse(offer.shouldHideAuthorInfo())
    }

    private fun newSell(): Sell {
        val price = Price(1, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE)
        return Sell(PriceInfo(price, null, price, Trend.UNCHANGED), null)
    }

    private fun newLongRent(yandexRent: Boolean = false): Rent {
        val price = Price(1, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH)
        return Rent(PriceInfo(price, null, price, Trend.UNCHANGED), yandexRent)
    }

    private fun newDailyRent(): Rent {
        val price = Price(1, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_DAY)
        return Rent(PriceInfo(price, null, price, Trend.UNCHANGED))
    }

    private fun newApartment(): Apartment {
        return Apartment(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
        )
    }

    private fun newRoom(): Room {
        return Room(null, null, null, null, null, null, null, null, null, null)
    }

    private fun newAuthor(category: Author.Category? = null): Author {
        return Author(category, null, null, null, null, null)
    }

    private fun locationInfoMoscow(): LocationInfo {
        return LocationInfo(
            location = Location(
                rgid = 100,
                subjectFederationId = SUBJECT_ID_MOSCOW,
                subjectFederationRegionId = null,
                subjectFederationName = null,
                point = RealtyPoint(GeoPoint(45.0, 45.0), null),
                address = null,
                streetAddress = null,
                geoCoderAddress = null
            ),
            metroList = emptyList(),
            metroUnderConstruction = emptyList(),
            parks = emptyList(),
            ponds = emptyList(),
            heatMaps = emptyList(),
            schools = emptyList(),
            highways = emptyList(),
            stations = emptyList()
        )
    }

    private fun newOffer(
        deal: Deal = newSell(),
        property: Property = newApartment(),
        author: Author? = null,
        locationInfo: LocationInfo? = null,
        isFullTrustedOwner: Boolean? = null
    ): OfferPreview {
        return OfferPreviewImpl(
            id = "1",
            deal = deal,
            property = property,
            partnerId = null,
            author = author,
            createdAt = null,
            updateDate = null,
            images = null,
            locationInfo = locationInfo,
            active = null,
            vas = null,
            uid = null,
            excerptFreeReportAccessible = null,
            onlineShow = null,
            videoId = null,
            isFullTrustedOwner = isFullTrustedOwner,
            shareUrl = null,
            isPaid = false,
            isExtended = false,
            note = null,
            virtualTour = null,
            chatInfo = null,
        )
    }
}
