package com.edadeal.android.ui

import android.graphics.RectF
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class RectFScaleTest(
    private val input: RectF,
    private val scaleX: Float,
    private val scaleY: Float,
    private val expected: RectF
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(RectF(100f, 100f, 200f, 200f), 0.5f, 0.5f, RectF(125f, 125f, 175f, 175f)),
            arrayOf(RectF(25f, 12f, 32f, 16f), 0.7f, 0.8f, RectF(26.05f, 12.4f, 30.95f, 15.6f)),
            arrayOf(RectF(40f, 40f, 80f, 100f), 1.5f, 2f, RectF(30f, 10f, 90f, 130f))
        )
    }

    @Test
    fun `should correctly scale rect bounds relative to it center coordinate`() {
        input.scale(scaleX, scaleY)
        // using hashCode, because there was bug in equals, it returns false negative on some SDK version
        // see: https://github.com/aosp-mirror/platform_frameworks_base/commit/3b577ddb1bca8b1c1682951fea69de24502bcf5d
        assertEquals(expected.hashCode(), input.hashCode())
    }
}
