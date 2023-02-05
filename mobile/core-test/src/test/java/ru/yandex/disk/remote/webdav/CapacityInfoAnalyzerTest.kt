package ru.yandex.disk.remote.webdav

import ru.yandex.disk.remote.CapacityInfo
import ru.yandex.disk.spaceutils.ByteUnit
import ru.yandex.disk.util.CapacityInfoAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals

class CapacityInfoAnalyzerTest {

    @Test
    fun testNoData() {
        val info = CapacityInfo(-1,-2,-3)
        assertEquals(CapacityInfoAnalyzer.State.NO_DATA, CapacityInfoAnalyzer.getState(info))
    }

    @Test
    fun testOverdraft() {
        val info = CapacityInfo(200,100500,0)
        assertEquals(CapacityInfoAnalyzer.State.SPACE_OVERDRAFT, CapacityInfoAnalyzer.getState(info))
    }

    @Test
    fun testLowSpace() {
        val info = CapacityInfo(ByteUnit.GB.value(), ByteUnit.MB.toBytes(950), 0)
        assertEquals(CapacityInfoAnalyzer.State.LOW_SPACE, CapacityInfoAnalyzer.getState(info))
    }

    @Test
    fun testLowSpaceOn100GB() {
        val info = CapacityInfo(ByteUnit.GB.toBytes(100), ByteUnit.GB.toBytes(95), 0)
        assertEquals(CapacityInfoAnalyzer.State.LOW_SPACE, CapacityInfoAnalyzer.getState(info))
    }

    @Test
    fun testSpaceFinished() {
        val info = CapacityInfo(ByteUnit.GB.toBytes(100), ByteUnit.GB.toBytes(100) - ByteUnit.KB.toBytes(500), 0)
        assertEquals(CapacityInfoAnalyzer.State.SPACE_FINISHED, CapacityInfoAnalyzer.getState(info))
    }

    @Test
    fun testEnoughSpace() {
        val info = CapacityInfo(ByteUnit.GB.toBytes(100), ByteUnit.GB.toBytes(10), 0)
        assertEquals(CapacityInfoAnalyzer.State.ENOUGH_SPACE, CapacityInfoAnalyzer.getState(info))
    }
}
