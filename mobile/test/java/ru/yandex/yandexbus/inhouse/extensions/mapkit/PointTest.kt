package ru.yandex.yandexbus.inhouse.extensions.mapkit

import com.yandex.mapkit.geometry.Point
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest

class PointTest : BaseTest() {

    @Test
    fun equalsTest() {
        val p1 = Point(0.0, 1.0)
        val p2 = Point(0.0, 1.0)

        assertTrue(p1.equalsTo(p2))
        assertTrue(p2.equalsTo(p1))
    }

    @Test
    fun notEqualsTest() {
        assertFalse(GeoPlaces.Minsk.CENTER.equalsTo(GeoPlaces.Kiev.CENTER))
    }

    @Test
    fun pointNotEqualsToNull() {
        assertFalse(GeoPlaces.Minsk.CENTER.equalsTo(null))
    }

    @Test
    fun pointEqualsAfterClone() {
        val p1 = GeoPlaces.Minsk.CENTER
        val p2 = p1.deepClone()

        assertFalse(p1 === p2)
        assertTrue(p1.equalsTo(p2))
    }

    @Test
    fun nullPoint() {
        val point: Point? = null
        assertFalse(point.equalsTo(GeoPlaces.Minsk.CENTER))
        assertTrue(point.equalsTo(null))
    }
}
