package com.yandex.mobile.realty.ui.presenter

import androidx.annotation.StringRes
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.site.HousingType
import com.yandex.mobile.realty.domain.model.site.SitePreviewImpl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author sorokinandrei on 8/13/21.
 */
@RunWith(RobolectricTestRunner::class)
class SitePreviewTest : RobolectricTest() {

    @Test
    fun testCatalogInfoLabelNoOffers() {
        val sitePreview = createSitePreview(totalOffers = 0)
        assertEquals(R.string.site_info_button, sitePreview.getSiteCatalogInfoLabelRes())
    }

    @Test
    fun testCatalogInfoLabelUnknownOffers() {
        val sitePreview = createSitePreview(totalOffers = null)
        assertEquals(R.string.site_info_button, sitePreview.getSiteCatalogInfoLabelRes())
    }

    @Test
    fun testCatalogInfoLabelFlats() {
        val sitePreview = createSitePreview(housingType = HousingType.FLATS, totalOffers = 10)
        assertEquals(
            R.string.all_rooms_from_developer,
            sitePreview.getSiteCatalogInfoLabelRes()
        )
    }

    @Test
    fun testCatalogInfoLabelApartments() {
        val sitePreview = createSitePreview(housingType = HousingType.APARTMENTS, totalOffers = 10)
        assertEquals(
            R.string.all_apartments_from_developer,
            sitePreview.getSiteCatalogInfoLabelRes()
        )
    }

    @Test
    fun testCatalogInfoLabelApartmentsAndFlats() {
        val sitePreview =
            createSitePreview(housingType = HousingType.APARTMENTS_AND_FLATS, totalOffers = 10)
        assertEquals(
            R.string.all_offers_from_developer,
            sitePreview.getSiteCatalogInfoLabelRes()
        )
    }

    @Test
    fun testCatalogInfoLabelUnknownHousingType() {
        val sitePreview = createSitePreview(housingType = null, totalOffers = 10)
        assertEquals(
            R.string.all_rooms_from_developer,
            sitePreview.getSiteCatalogInfoLabelRes()
        )
    }

    private fun assertEquals(@StringRes expected: Int, @StringRes actual: Int) {
        assertEquals(context.getString(expected), context.getString(actual))
    }

    private fun createSitePreview(
        totalOffers: Int?,
        housingType: HousingType? = null
    ): SitePreviewImpl {
        return SitePreviewImpl(
            id = "123",
            name = "name",
            fullName = "fullName",
            locativeFullName = null,
            developers = null,
            buildingClass = null,
            type = null,
            images = null,
            price = null,
            pricePerMeter = null,
            totalOffers = totalOffers,
            locationInfo = null,
            commissioningStatus = null,
            deliveryDates = null,
            specialProposalLabels = null,
            flatStatus = null,
            salesClosed = null,
            housingType = housingType,
            shareUrl = null,
            isPaid = false,
            isExtended = false,
            hasDeveloperChat = false,
            briefRoomsStats = null,
        )
    }
}
