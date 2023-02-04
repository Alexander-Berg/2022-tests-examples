package com.yandex.mobile.realty.data.model

import com.yandex.mobile.realty.data.model.RecentSearchIdGenerator.GeoIntentEncoder
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.utils.getInstanceFieldsCount
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author shpigun on 2019-08-26
 */
class GeoEncoderTest {
    @Test
    fun testGeoIntentAllFieldsEncoded() {
        val geoIntent = GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
        val geoIntentEncoder = GeoIntentEncoder()
        geoIntentEncoder.encode(geoIntent)
        assertEquals(geoIntent.getInstanceFieldsCount(), geoIntentEncoder.getHandledFieldsCount())
    }
}
