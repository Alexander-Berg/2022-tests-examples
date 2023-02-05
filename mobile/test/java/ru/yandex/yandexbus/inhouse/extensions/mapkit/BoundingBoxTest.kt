package ru.yandex.yandexbus.inhouse.extensions.mapkit

import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundingBoxTest {

    @Test
    fun containsPointFromBoundingBox() {
        assertTrue(GeoPlaces.Minsk.CENTER in GeoPlaces.Minsk.BOUNDS)
    }

    @Test
    fun doesNotContainPointOutsideOfBoundingBox() {
        assertFalse(GeoPlaces.Kiev.CENTER in GeoPlaces.Minsk.BOUNDS)
    }

    @Test
    fun doesNotContainCornerPoint() {
        assertFalse(GeoPlaces.Minsk.BOUNDS.southWest in GeoPlaces.Minsk.BOUNDS)
        assertFalse(GeoPlaces.Minsk.BOUNDS.northEast in GeoPlaces.Minsk.BOUNDS)
    }

    @Test
    fun intersectsItself() {
        assertTrue(GeoPlaces.Minsk.BOUNDS.intersects(GeoPlaces.Minsk.BOUNDS))
    }

    @Test
    fun intersectsIntersectingBounds() {
        assertTrue(GeoPlaces.Europe.BALTIC_SEA_BLACK_SEA_BOUNDS.intersects(GeoPlaces.Minsk.BOUNDS))
        assertTrue(GeoPlaces.Minsk.BOUNDS.intersects(GeoPlaces.Europe.BALTIC_SEA_BLACK_SEA_BOUNDS))
    }

    @Test
    fun doesNotIntersectNotIntersectingBounds() {
        assertFalse(GeoPlaces.Minsk.BOUNDS.intersects(GeoPlaces.Kiev.BOUNDS))
        assertFalse(GeoPlaces.Kiev.BOUNDS.intersects(GeoPlaces.Minsk.BOUNDS))
    }

    @Test
    fun equalsTest() {
        val b1 = BoundingBox(Point(0.0, 1.0), Point(5.0, 6.0))
        val b2 = BoundingBox(Point(0.0, 1.0), Point(5.0, 6.0))

        assertFalse(b1 === b2)
        assertTrue(b1.equalsTo(b2))
        assertTrue(b2.equalsTo(b1))
    }

    @Test
    fun boundingBoxEqualsAfterClone() {
        val b1 = GeoPlaces.Minsk.BOUNDS
        val b2 = b1.deepClone()

        assertFalse(b1 === b2)
        assertTrue(b1.equalsTo(b2))
    }

    @Test
    fun equalsBoundingBox() {
        assertTrue(GeoPlaces.Minsk.BOUNDS.equalsTo(GeoPlaces.Minsk.BOUNDS.deepClone()))
    }

    @Test
    fun differentBoundingBoxes() {
        assertFalse(GeoPlaces.Minsk.BOUNDS.equalsTo(GeoPlaces.Kiev.BOUNDS))
    }

    @Test
    fun equalityCheckWithNullability() {
        assertTrue((null as BoundingBox?).equalsTo(null))
        assertFalse((null as BoundingBox?).equalsTo(GeoPlaces.Minsk.BOUNDS))
        assertFalse(GeoPlaces.Minsk.BOUNDS.equalsTo(null))
        assertFalse(GeoPlaces.Minsk.BOUNDS.equalsTo(null as BoundingBox?))
    }
}
