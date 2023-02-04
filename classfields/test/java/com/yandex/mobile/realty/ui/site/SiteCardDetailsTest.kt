package com.yandex.mobile.realty.ui.site

import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.common.AgreementType
import com.yandex.mobile.realty.domain.model.common.SiteParking
import com.yandex.mobile.realty.domain.model.site.SiteParkingType
import com.yandex.mobile.realty.ui.site.adapter.adapteritems.DetailAgreementItem
import com.yandex.mobile.realty.ui.site.adapter.adapteritems.DetailCeilingHeightItem
import com.yandex.mobile.realty.ui.site.adapter.adapteritems.DetailCeilingHeightRangeItem
import com.yandex.mobile.realty.ui.site.adapter.adapteritems.DetailFloorItem
import com.yandex.mobile.realty.ui.site.adapter.adapteritems.DetailParkingItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Created by Alena Malchikhina on 20.04.2020
 */
@RunWith(RobolectricTestRunner::class)
class SiteCardDetailsTest : RobolectricTest() {

    @Test
    fun agreementTypeLaw214AndDdu() {
        val agreementDetailItem = createDetailAgreementItem(
            AgreementType.DDU,
            true
        )
        assertEquals("214 ФЗ, ДДУ", agreementDetailItem.getFormattedValue(context.resources))
    }

    @Test
    fun agreementTypeLaw214AndZhsk() {
        val agreementDetailItem = createDetailAgreementItem(
            AgreementType.ZHSK,
            true
        )
        assertEquals("214 ФЗ, ЖСК", agreementDetailItem.getFormattedValue(context.resources))
    }

    @Test
    fun agreementTypeLaw214() {
        val agreementDetailItem = createDetailAgreementItem(null, true)
        assertEquals("214 ФЗ", agreementDetailItem.getFormattedValue(context.resources))
    }

    @Test
    fun agreementTypeDdu() {
        val agreementDetailItem = createDetailAgreementItem(AgreementType.DDU, false)
        assertEquals("ДДУ", agreementDetailItem.getFormattedValue(context.resources))
    }

    @Test
    fun agreementTypeZhsk() {
        val agreementDetailItem = createDetailAgreementItem(AgreementType.ZHSK, false)
        assertEquals("ЖСК", agreementDetailItem.getFormattedValue(context.resources))
    }

    @Test
    fun floorItemRange() {
        val floorItem = createDetailFloorItem(Range.valueOf(1, 10))
        assertEquals("от 1 до 10", floorItem.getFormattedValue(context.resources))
    }

    @Test
    fun floorItemSingleFloor() {
        val floorItem = createDetailFloorItem(Range.valueOf(10, 10))
        assertEquals("10", floorItem.getFormattedValue(context.resources))
    }

    @Test
    fun ceilingHeightValid() {
        val ceilingItem = DetailCeilingHeightItem(
            R.drawable.ic_ceiling_height,
            R.string.nb_ceiling_height,
            3.011f
        )
        assertEquals("3,01 м", ceilingItem.getFormattedValue(context.resources))
    }

    @Test
    fun ceilingHeightRangeValid() {
        val ceilingItem = DetailCeilingHeightRangeItem(
            R.drawable.ic_ceiling_height,
            R.string.nb_ceiling_height,
            3.011f,
            3.213f
        )
        assertEquals("3,01–3,21 м", ceilingItem.getFormattedValue(context.resources))
    }

    @Test
    fun parkingWithPlaces() {
        val parkingItem = createDetailParkingItem(
            listOf(
                SiteParking(SiteParkingType.UNDERGROUND, 1220),
                SiteParking(SiteParkingType.CLOSED, 1111),
                SiteParking(SiteParkingType.OPEN, 100)
            )
        )
        assertEquals(
            "подземный - 1220,\nкрытый - 1111,\nоткрытый - 100",
            parkingItem.getFormattedValue(context.resources)
        )
    }

    @Test
    fun parkingWithoutPlaces() {
        val parkingItem = createDetailParkingItem(
            listOf(
                SiteParking(SiteParkingType.UNDERGROUND, null),
                SiteParking(SiteParkingType.CLOSED, null),
                SiteParking(SiteParkingType.OPEN, null)
            )
        )
        assertEquals(
            "есть подземный,\nесть крытый,\nесть открытый",
            parkingItem.getFormattedValue(context.resources)
        )
    }

    @Test
    fun parkingOpen() {
        val parkingItem = createDetailParkingItem(
            listOf(
                SiteParking(SiteParkingType.OPEN, null)
            )
        )
        assertEquals(
            "есть открытый",
            parkingItem.getFormattedValue(context.resources)
        )
    }

    @Test
    fun parkingClosed() {
        val parkingItem = createDetailParkingItem(
            listOf(
                SiteParking(SiteParkingType.CLOSED, null)
            )
        )
        assertEquals(
            "есть крытый",
            parkingItem.getFormattedValue(context.resources)
        )
    }

    @Test
    fun parkingUnderground() {
        val parkingItem = createDetailParkingItem(
            listOf(
                SiteParking(SiteParkingType.UNDERGROUND, null)
            )
        )
        assertEquals(
            "есть подземный",
            parkingItem.getFormattedValue(context.resources)
        )
    }

    private fun createDetailParkingItem(parkings: List<SiteParking>): DetailParkingItem {
        return DetailParkingItem(
            R.drawable.ic_guest_parking,
            R.string.site_parking_places,
            parkings
        )
    }

    private fun createDetailFloorItem(floors: Range<Int>): DetailFloorItem {
        return DetailFloorItem(R.drawable.ic_floors, R.string.site_floors, floors)
    }

    private fun createDetailAgreementItem(
        agreementType: AgreementType?,
        law214: Boolean
    ): DetailAgreementItem {
        return DetailAgreementItem(
            R.drawable.ic_agreement,
            R.string.site_agreement_type,
            agreementType,
            law214
        )
    }
}
