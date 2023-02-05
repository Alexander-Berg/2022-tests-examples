package com.edadeal.android.util

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class AndroidVersionHelperTest(
    private val buildSdkInt: Int,
    private val buildVersionRelease: String,
    private val expectedVersion: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(-1, "9.0.1-patch by PaintYourAndroid", "9.0.1"),
            arrayOf(-1, "7.0 [SDK 24 | ARM", "7.0.0"),
            arrayOf(-1, "Android 8.1 oreo", "8.1.0"),
            arrayOf(-1, "6.0 Marshmallow", "6.0.0"),
            arrayOf(-1, "Android 8.0", "8.0.0"),
            arrayOf(-1, "7.0N", "7.0.0"),
            arrayOf(-1, "8.1Go", "8.1.0"),
            arrayOf(-1, "8.1.0", "8.1.0"),
            arrayOf(-1, "Android 10.1.99+20200313144700?", "10.1.99"),
            arrayOf(-1, "Android 11.0.0-beta+exp.sha.5114f85", "11.0.0"),
            arrayOf(-1, "Android 11.0.0-beta+exp.sha.5114f85", "11.0.0"),
            arrayOf(-1, "12", "12.0.0"),
            arrayOf(28, "P", "9.0.0"),
            arrayOf(29, "Q", "10.0.0"),
            arrayOf(30, "R", "11.0.0"),
            arrayOf(31, "S", "12.0.0"),
            arrayOf(10000, "S", "12.0.0")
        )
    }

    private val helper = AndroidVersionHelper()

    @Test
    fun `should return expected version`() {
        val osVersion = helper.getReleaseVersion(buildVersionRelease) ?: helper.guessVersion(buildSdkInt)
        assertEquals(expectedVersion, osVersion)
    }
}
