package ru.yandex.supercheck.domain.geo

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.supercheck.model.domain.common.Accuracy
import ru.yandex.supercheck.model.domain.common.GeoLocation

@RunWith(MockitoJUnitRunner::class)
class GeoLocationComparisonTest {

    @Test
    fun isNearTest() {
        val myGeoLocation = GeoLocation(latitude = 55.733969, longitude = 37.587093)
        val nearGeoLocation = GeoLocation(latitude = 55.735687, longitude = 37.591494)
        testNearGeoLocations(myGeoLocation, nearGeoLocation, true)
    }

    @Test
    fun isNotNearTest() {
        val myGeoLocation = GeoLocation(latitude = 55.733969, longitude = 37.587093)
        val nearGeoLocation = GeoLocation(latitude = 55.769099, longitude = 37.680821)
        testNearGeoLocations(myGeoLocation, nearGeoLocation, false)
    }

    @Test
    fun testIsAcuratelyNearToWithoutAccuracy() {
        val myGeoLocation = GeoLocation(latitude = 55.733969, longitude = 37.587093)
        val nearGeoLocation = GeoLocation(latitude = 55.735687, longitude = 37.591494)
        testAccuratelyNearGeoLocation(myGeoLocation, nearGeoLocation, false)
    }

    @Test
    fun testIsAccuratelyNearToWithAccuracy() {
        val myGeoLocation = GeoLocation(
            latitude = 55.733969,
            longitude = 37.587093,
            accuracy = Accuracy.Radius(1f)
        )
        val nearGeoLocation = GeoLocation(latitude = 55.735687, longitude = 37.591494)
        testAccuratelyNearGeoLocation(myGeoLocation, nearGeoLocation, true)
    }

    @Test
    fun testIsAccuratelyNotNearBecauseOfAccuracy() {
        val myGeoLocation = GeoLocation(
            latitude = 55.733969,
            longitude = 37.587093,
            accuracy = Accuracy.Radius(300f)
        )
        val nearGeoLocation = GeoLocation(latitude = 55.735687, longitude = 37.591494)
        testAccuratelyNearGeoLocation(myGeoLocation, nearGeoLocation, false)
    }

    @Test
    fun testIsAccuratelyNotNear() {
        val myGeoLocation = GeoLocation(
            latitude = 55.733969,
            longitude = 37.587093,
            accuracy = Accuracy.Radius(1f)
        )
        val nearGeoLocation = GeoLocation(latitude = 55.769099, longitude = 37.680821)
        testAccuratelyNearGeoLocation(myGeoLocation, nearGeoLocation, false)
    }

    private fun testNearGeoLocations(first: GeoLocation, second: GeoLocation, isNear: Boolean) {
        assertEquals(null, isNear, first.isNearTo(second))
    }

    private fun testAccuratelyNearGeoLocation(
        first: GeoLocation,
        second: GeoLocation,
        isNear: Boolean
    ) {
        assertEquals(null, isNear, first.isAccuratelyNearTo(second))
    }

}