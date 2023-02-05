package ru.yandex.yandexbus.inhouse.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotspotTest {

    @Test
    fun equalsTest() {
        val h1 = Hotspot("HotspotId").apply { name = "IrrelevantName1" }
        val h2 = Hotspot(h1.id).apply { name = "IrrelevantName2" }
        val h3 = Hotspot("AnotherId").apply { name = h1.name }

        assertTrue(h1 == h1)
        assertTrue(h1 == h2)
        assertTrue(h1.hashCode() == h2.hashCode())
        assertFalse(h1 == h3)
    }
}
