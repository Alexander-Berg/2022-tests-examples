package com.yandex.mobile.realty.ui.presenter

import com.yandex.mobile.realty.domain.model.common.Image
import com.yandex.mobile.realty.domain.model.common.ImageType
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Sell
import com.yandex.mobile.realty.domain.model.virtualtour.VirtualTour
import org.junit.Assert
import org.junit.Test

/**
 * @author pvl-zolotov on 26.10.2021
 */
class OfferImagesInfoTest {

    @Test
    fun testOfferWithApartmentPlan() {
        val offer = OfferImagesInfo.valueOf(
            newOffer(
                listOf(
                    newImage(ImageType.APARTMENT_PLAN),
                    newImage(null)
                )
            )
        )
        Assert.assertFalse(offer.isApartmentButtonVisible(0))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(0))

        Assert.assertFalse(offer.isApartmentButtonVisible(1))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(1))

        Assert.assertTrue(offer.isApartmentButtonVisible(2))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(2))

        Assert.assertTrue(offer.isApartmentButtonVisible(3))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(3))
    }

    @Test
    fun testOfferWithFloorPlan() {
        val offer = OfferImagesInfo.valueOf(
            newOffer(
                listOf(
                    newImage(null),
                    newImage(ImageType.FLOOR_PLAN)
                )
            )
        )
        Assert.assertFalse(offer.isApartmentButtonVisible(0))
        Assert.assertTrue(offer.isFloorPlanButtonVisible(0))

        Assert.assertFalse(offer.isApartmentButtonVisible(1))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(1))

        Assert.assertFalse(offer.isApartmentButtonVisible(2))
        Assert.assertTrue(offer.isFloorPlanButtonVisible(2))

        Assert.assertFalse(offer.isApartmentButtonVisible(3))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(3))
    }

    @Test
    fun testOfferWithoutPlans() {
        val offer = OfferImagesInfo.valueOf(
            newOffer(
                listOf(
                    newImage(null)
                )
            )
        )
        Assert.assertFalse(offer.isApartmentButtonVisible(0))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(0))

        Assert.assertFalse(offer.isApartmentButtonVisible(1))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(1))

        Assert.assertFalse(offer.isApartmentButtonVisible(2))
        Assert.assertFalse(offer.isFloorPlanButtonVisible(2))
    }

    private fun newOffer(images: List<Image>): OfferPreview {
        return OfferPreviewImpl(
            id = "1",
            deal = newSell(),
            property = newApartment(),
            partnerId = null,
            author = null,
            createdAt = null,
            updateDate = null,
            images = images,
            locationInfo = null,
            active = null,
            vas = null,
            uid = null,
            excerptFreeReportAccessible = null,
            onlineShow = null,
            videoId = "0",
            isFullTrustedOwner = null,
            shareUrl = null,
            isPaid = false,
            isExtended = false,
            note = null,
            virtualTour = newVirtualTour(),
            chatInfo = null,
        )
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

    private fun newSell(): Sell {
        val price = Price(1, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE)
        return Sell(PriceInfo(price, null, price, Trend.UNCHANGED), null)
    }

    private fun newVirtualTour(): VirtualTour {
        return VirtualTour("http://example.com", null)
    }

    private fun newImage(type: ImageType?): Image {
        return Image(
            "image.png",
            null,
            null,
            null,
            null,
            null,
            type = type
        )
    }
}
