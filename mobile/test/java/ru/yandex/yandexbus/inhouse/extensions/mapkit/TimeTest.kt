package ru.yandex.yandexbus.inhouse.extensions.mapkit

import com.yandex.mapkit.Time
import org.junit.Assert
import org.junit.Test

class TimeTest {

    @Test
    fun equalsTest() {
        val t1 = Time(1L, 2, "")
        val t2 = Time(1L, 2, "")

        Assert.assertFalse(t1 === t2)
        Assert.assertTrue(t1.equalsTo(t2))
        Assert.assertTrue(t2.equalsTo(t1))
    }

    @Test
    fun timeNotEquals() {
        Assert.assertFalse(TIME1.equalsTo(TIME2))
    }

    @Test
    fun timeEqualsAfterClone() {
        val t1 = TIME1
        val t2 = t1.deepClone()

        Assert.assertFalse(t1 === t2)
        Assert.assertTrue(t1.equalsTo(t2))
    }

    companion object {
        private val TIME1 = Time(1L, 2, "")
        private val TIME2 = Time(2L, 3, "null")
    }
}