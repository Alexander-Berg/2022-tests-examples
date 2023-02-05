package ru.yandex.yandexmaps.tools.bumpmapkit.mapkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MapkitVersionTest {

    @Test
    fun validVersions() {
        val tests = arrayOf(
            "2022012718.9100982" to MapkitVersion(2022012718, 9100982, null),
            "2021042118.8350239" to MapkitVersion(2021042118, 8350239, null),
            "20220210.3-0b630911e-10.1" to MapkitVersion(20220210, 3, "0b630911e-10.1"),
            "0.0" to MapkitVersion(0, 0, null),
        )

        for (test in tests) {
            val str = test.first
            val expected = test.second
            assertEquals(expected, MapkitVersion.parse(str))
        }
    }

    @Test
    fun invalidVersions() {
        val invalidVersions = arrayOf(
            "",
            "hello",
            "-1.-2",
            ".",
            "hello.world",
            "1.2.3"
        )

        for (invalidVersion in invalidVersions) {
            assertFailsWith<IllegalStateException>("Failed test for '$invalidVersion'") {
                MapkitVersion.parse(invalidVersion)
            }
        }
    }

    @Test
    fun compareMainPartTo() {
        assert(ver("9.0").compareMainPartTo(ver("10.0")) < 0)
        assert(ver("10.10").compareMainPartTo(ver("10.9")) > 0)
        assert(ver("1.2-rc1").compareMainPartTo(ver("1.2-rc2")) == 0)
    }

    @Test
    fun testToString() {
        assertEquals(
            "2022012718.9100982",
            MapkitVersion(2022012718, 9100982).toString()
        )
    }

    @Test
    fun testComponents() {
        val major = 2022012718
        val minor = 9100982
        val version = MapkitVersion(major, minor)
        assertEquals(major, version.major)
        assertEquals(minor, version.minor)
    }

    @Test
    fun testCommitHash() {
        assertEquals(null, ver("2021090921.8796565").commitHash)
        assertEquals("73308f777", ver("2022031023.11-73308f777-1.9").commitHash)
    }

    @Test
    fun testReleaseNumber() {
        assertEquals(null, ver("2021090921.8796565").releaseNumber)
        assertEquals(1, ver("2022031023.11-73308f777-1.9").releaseNumber)
    }

    private fun ver(value: String): MapkitVersion {
        return MapkitVersion.parse(value)
    }
}
