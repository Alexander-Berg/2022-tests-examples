package com.edadeal.android.ui.common.bindings.ad

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.edadeal.android.util.PromoSlotsImageHelper

class AdBindingTest {

    @Test
    fun `getDensityForImageKey should return density for valid key`() {
        assertEquals(1, PromoSlotsImageHelper.getDensityForImageKey("hdpi"))
        assertEquals(2, PromoSlotsImageHelper.getDensityForImageKey("xhdpi"))
        assertEquals(4, PromoSlotsImageHelper.getDensityForImageKey("xxxhdpi"))
        assertEquals(1, PromoSlotsImageHelper.getDensityForImageKey("xxx?hdpi"))
        assertNull(PromoSlotsImageHelper.getDensityForImageKey("dpi"))
    }

    @Test
    fun `getBestImageUrl should return nearest greater value for given display density`() {
        val sut = { displayDensity: Float, keys: Set<String> ->
            AdBinding.Content("", keys.associateBy { it }).getBestImageUrl(displayDensity)
        }

        assertEquals("hdpi", sut(1f, setOf("hdpi", "xhdpi", "?", "xxhdpi", "xxhdpi", "xxxhdpi")))
        assertEquals("xhdpi", sut(2f, setOf("hdpi", "xhdpi", "?", "xxhdpi", "xxhdpi", "xxxhdpi")))
        assertEquals("xxhdpi", sut(2f, setOf("hdpi", "xxhdpi", "xxxhdpi")))
        assertEquals("xxhdpi", sut(2.5f, setOf("hdpi", "xhdpi", "xxhdpi")))
        assertEquals("xxxhdpi", sut(3f, setOf("hdpi", "xhdpi", "xxxhdpi")))
    }
}
