package com.yandex.mobile.realty.ui.model

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus.HAND_OVER
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus.IN_PROJECT
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus.SOON_AVAILABLE
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus.SUSPENDED
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus.UNDER_CONSTRUCTION
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus.UNDER_CONSTRUCTIONS_WITH_HAND_OVER
import com.yandex.mobile.realty.domain.model.common.DeliveryDates
import com.yandex.mobile.realty.domain.model.common.Quarter
import com.yandex.mobile.realty.domain.model.common.QuarterOfYear
import com.yandex.mobile.realty.domain.model.common.SiteBuildingClass
import com.yandex.mobile.realty.domain.model.site.SitePreviewImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author shpigun on 2020-03-11
 */
@RunWith(RobolectricTestRunner::class)
class SiteCardSummaryTest : RobolectricTest() {

    @Test
    fun buildingClassEconom() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(buildingClass = SiteBuildingClass.ECONOM)
        )
        assertEquals("эконом", summary?.getDisplayString())
    }

    @Test
    fun buildingClassBusiness() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(buildingClass = SiteBuildingClass.BUSINESS)
        )
        assertEquals("бизнес", summary?.getDisplayString())
    }

    @Test
    fun buildingClassComfort() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(buildingClass = SiteBuildingClass.COMFORT)
        )
        assertEquals("комфорт", summary?.getDisplayString())
    }

    @Test
    fun buildingClassComfortPlus() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(buildingClass = SiteBuildingClass.COMFORT_PLUS)
        )
        assertEquals("комфорт+", summary?.getDisplayString())
    }

    @Test
    fun buildingClassElite() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(buildingClass = SiteBuildingClass.ELITE)
        )
        assertEquals("элитное жильё", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusUnfinished() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(commissioningStatus = UNDER_CONSTRUCTION)
        )
        assertEquals("Строится", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusUnfinishedWithDelivered() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(
                commissioningStatus = UNDER_CONSTRUCTIONS_WITH_HAND_OVER,
                deliveryDates = DeliveryDates(
                    setOf(QuarterOfYear(2019, Quarter.I)),
                    emptySet()
                )
            )
        )
        assertEquals("Строится, есть\u00A0сданные", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusSoonAvailable() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(commissioningStatus = SOON_AVAILABLE)
        )
        assertEquals("Скоро в продаже", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusConstructionSuspended() {
        val summary = SiteCardSummary.valueOf(createSitePreview(commissioningStatus = SUSPENDED))
        assertEquals("Стройка заморожена", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusConstructionSuspendedWithDelivered() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(
                commissioningStatus = SUSPENDED,
                deliveryDates = DeliveryDates(
                    setOf(QuarterOfYear(2019, Quarter.I)),
                    emptySet()
                )
            )
        )
        assertEquals("Стройка заморожена", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusHandOver() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(commissioningStatus = HAND_OVER)
        )
        assertEquals("Сдан", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusInProject() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(commissioningStatus = IN_PROJECT)
        )
        assertEquals("В проекте, срок сдачи уточняется", summary?.getDisplayString())
    }

    @Test
    fun commissioningStatusInProjectWithUnfinishedDeliveryDates() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(
                commissioningStatus = IN_PROJECT,
                deliveryDates = DeliveryDates(
                    emptySet(),
                    setOf(QuarterOfYear(2030, Quarter.II))
                )
            )
        )
        assertEquals("В проекте, 2‑й кв. 2030 г", summary?.getDisplayString())
    }

    @Test
    fun deliveryDateSingle() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(
                commissioningStatus = UNDER_CONSTRUCTION,
                deliveryDates = DeliveryDates(
                    emptySet(),
                    setOf(QuarterOfYear(2030, Quarter.II))
                )
            )
        )
        assertEquals("2‑й\u00A0кв.\u00A02030\u00A0г", summary?.getDisplayString())
    }

    @Test
    fun deliveryDateRange() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(
                commissioningStatus = UNDER_CONSTRUCTIONS_WITH_HAND_OVER,
                deliveryDates = DeliveryDates(
                    emptySet(),
                    setOf(
                        QuarterOfYear(2030, Quarter.II),
                        QuarterOfYear(2035, Quarter.IV)
                    )
                )
            )
        )
        assertEquals(
            "2‑й\u00A0кв.\u00A02030\u00A0г – 4‑й\u00A0кв.\u00A02035\u00A0г",
            summary?.getDisplayString()
        )
    }

    @Test
    fun deliveryDateSingleWithDelivered() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(
                commissioningStatus = UNDER_CONSTRUCTIONS_WITH_HAND_OVER,
                deliveryDates = DeliveryDates(
                    setOf(QuarterOfYear(2019, Quarter.I)),
                    setOf(QuarterOfYear(2030, Quarter.II))
                )
            )
        )
        assertEquals(
            "2‑й\u00A0кв.\u00A02030\u00A0г, есть\u00A0сданные",
            summary?.getDisplayString()
        )
    }

    @Test
    fun deliveryDateRangeWithDelivered() {
        val summary = SiteCardSummary.valueOf(
            createSitePreview(
                commissioningStatus = UNDER_CONSTRUCTIONS_WITH_HAND_OVER,
                deliveryDates = DeliveryDates(
                    setOf(QuarterOfYear(2019, Quarter.I)),
                    setOf(
                        QuarterOfYear(2030, Quarter.II),
                        QuarterOfYear(2035, Quarter.IV)
                    )
                )
            )
        )
        assertEquals(
            "2‑й\u00A0кв.\u00A02030\u00A0г – 4‑й\u00A0кв.\u00A02035\u00A0г, есть\u00A0сданные",
            summary?.getDisplayString()
        )
    }

    @Test
    fun emptySiteSummary() {
        val summary = SiteCardSummary.valueOf(createSitePreview())
        assertNull(summary)
    }

    private fun createSitePreview(
        buildingClass: SiteBuildingClass? = null,
        commissioningStatus: CommissioningStatus? = null,
        deliveryDates: DeliveryDates? = null
    ): SitePreviewImpl {
        return SitePreviewImpl(
            id = "123",
            name = "name",
            fullName = "fullName",
            locativeFullName = null,
            developers = null,
            buildingClass = buildingClass,
            type = null,
            images = null,
            price = null,
            pricePerMeter = null,
            totalOffers = null,
            locationInfo = null,
            commissioningStatus = commissioningStatus,
            deliveryDates = deliveryDates,
            specialProposalLabels = null,
            flatStatus = null,
            salesClosed = null,
            housingType = null,
            shareUrl = null,
            isPaid = false,
            isExtended = false,
            hasDeveloperChat = false,
            briefRoomsStats = null,
        )
    }
}
