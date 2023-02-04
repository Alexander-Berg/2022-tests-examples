package com.yandex.mobile.realty.ui.advertising.listing

import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.ui.advertising.putIsDebug
import com.yandex.mobile.realty.ui.advertising.putIsLarge
import com.yandex.mobile.realty.ui.advertising.putPassportUid
import com.yandex.mobile.realty.ui.advertising.putSearchListAdRequestParameters
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author shpigun on 2019-11-26
 */
class AdRequestParamsTest {

    @Test
    fun testAdRequestParamsSellApartment() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "APARTMENT",
            "adf_puid18" to "1"
        )
        val filter = Filter.SellApartment()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSiteApartment() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "NEWFLAT",
            "adf_puid21" to "APARTMENT",
            "adf_puid18" to "1"
        )
        val filter = Filter.SiteApartment()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentApartment() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "RENT",
            "adf_puid21" to "APARTMENT",
            "adf_puid18" to "1"
        )
        val filter = Filter.RentApartment()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellRoom() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "ROOMS",
            "adf_puid18" to "1"
        )
        val filter = Filter.SellRoom()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentRoom() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "RENT",
            "adf_puid21" to "ROOMS",
            "adf_puid18" to "1"
        )
        val filter = Filter.RentRoom()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellHouse() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "HOUSE",
            "adf_puid18" to "1"
        )
        val filter = Filter.SellHouse()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsVillageHouse() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "NEWFLAT",
            "adf_puid21" to "HOUSE",
            "adf_puid18" to "1"
        )
        val filter = Filter.VillageHouse()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentHouse() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "RENT",
            "adf_puid21" to "HOUSE",
            "adf_puid18" to "1"
        )
        val filter = Filter.RentHouse()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellLot() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "LOT",
            "adf_puid18" to "1"
        )
        val filter = Filter.SellLot()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsVillageLot() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "NEWFLAT",
            "adf_puid21" to "LOT",
            "adf_puid18" to "1"
        )
        val filter = Filter.VillageLot()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellCommercial() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "COMMERCIAL",
            "adf_puid21" to "COMMERCIAL",
            "adf_puid18" to "1"
        )
        val filter = Filter.SellCommercial()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentCommercial() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "COMMERCIAL",
            "adf_puid21" to "COMMERCIAL",
            "adf_puid18" to "1"
        )
        val filter = Filter.RentCommercial()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsSellGarage() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "SECOND",
            "adf_puid21" to "GARAGE",
            "adf_puid18" to "1"
        )
        val filter = Filter.SellGarage()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsRentGarage() {
        val expectedAdRequestParams = mapOf(
            "adf_p1" to "cdrbh",
            "adf_puid10" to "RENT",
            "adf_puid21" to "GARAGE",
            "adf_puid18" to "1"
        )
        val filter = Filter.RentGarage()
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putSearchListAdRequestParameters(filter, 1)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsWithDebug() {
        val expectedAdRequestParams = mapOf("adf_puid9" to "debug")
        val actualAdRequestParams = hashMapOf<String, String>()
        actualAdRequestParams.putIsDebug(true)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsWithoutDebug() {
        val expectedAdRequestParams = mapOf<String, String>()
        val actualAdRequestParams = hashMapOf<String, String>()
        actualAdRequestParams.putIsDebug(false)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsIsLarge() {
        val expectedAdRequestParams = mapOf("adf_puid13" to "other")
        val actualAdRequestParams = hashMapOf<String, String>()
        actualAdRequestParams.putIsLarge(true)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsIsSmall() {
        val expectedAdRequestParams = mapOf("adf_puid13" to "phone")
        val actualAdRequestParams = hashMapOf<String, String>()
        actualAdRequestParams.putIsLarge(false)
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }

    @Test
    fun testAdRequestParamsPassportUid() {
        val expectedAdRequestParams = mapOf("passportuid" to "1")
        val actualAdRequestParams = mutableMapOf<String, String>()
        actualAdRequestParams.putPassportUid("1")
        assertEquals(expectedAdRequestParams, actualAdRequestParams)
    }
}
