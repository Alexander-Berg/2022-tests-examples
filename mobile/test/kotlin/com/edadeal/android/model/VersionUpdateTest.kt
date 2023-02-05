package com.edadeal.android.model

import com.edadeal.android.model.versionupdate.Version
import com.edadeal.android.model.versionupdate.getWhatsNewVersions
import org.junit.Test
import kotlin.test.assertEquals

class VersionUpdateTest {
    @Test
    fun `getWhatsNewVersions returns correct versions for 4_9_9 - 5_1_2 update`() {
        val prev = Version.fromString("4.9.9")!!
        val current = Version.fromString("5.1.2")!!
        val res = getWhatsNewVersions(prev, current)

        assertEquals(res, listOf(Version(5, 0, 0), Version(5, 1, 0), Version(5, 1, 2)))
    }

    @Test
    fun `getWhatsNewVersions returns correct versions for 5_0_0 - 5_0_2 update`() {
        val prev = Version.fromString("5.0.0")!!
        val current = Version.fromString("5.0.2")!!
        val res = getWhatsNewVersions(prev, current)

        assertEquals(res, listOf(Version(5, 0, 2)))
    }

    @Test
    fun `getWhatsNewVersions returns correct versions for 5_0_0 - 6_0_0 update`() {
        val prev = Version.fromString("5.0.0")!!
        val current = Version.fromString("6.0.0")!!
        val res = getWhatsNewVersions(prev, current)

        assertEquals(res, listOf(Version(6, 0, 0)))
    }

    @Test
    fun `getWhatsNewVersions returns correct versions for 5_0_3 - 5_2_3 update`() {
        val prev = Version.fromString("5.0.3")!!
        val current = Version.fromString("5.2.3")!!
        val res = getWhatsNewVersions(prev, current)

        assertEquals(res, listOf(Version(5, 2, 0), Version(5, 2, 3)))
    }
}
