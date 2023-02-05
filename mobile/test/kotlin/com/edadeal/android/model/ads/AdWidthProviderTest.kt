package com.edadeal.android.model.ads

import android.os.Build
import com.edadeal.android.Res
import com.edadeal.android.dto.Promo
import com.edadeal.android.ui.dp
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AdWidthProviderTest {
    private val res = RuntimeEnvironment.application.resources

    @Test
    fun `getWidth should return correct values`() {
        val defaultSlideWidth = Res.getDefaultAdSlideWidth(res)

        assertEquals(res.dp(112), makeProvider(112, "pt").getWidth(res))
        assertEquals(defaultSlideWidth, makeProvider(100, "%").getWidth(res))
        assertEquals(Res.getCatalogCoverWidth(res), makeProvider(1, "cover").getWidth(res))
        assertEquals(defaultSlideWidth, makeProvider(0, "?").getWidth(res))
        assertEquals((defaultSlideWidth * .5).roundToInt(), makeProvider(50, "%").getWidth(res))
    }

    @Test
    fun `hasDynamicWidth should return true only for items with dynamic width unit`() {
        assertTrue(makeProvider(112, "pt").hasDynamicWidth)
        assertTrue(makeProvider(100, "%").hasDynamicWidth)
        assertTrue(makeProvider(0, "cover").hasDynamicWidth)
        assertFalse(makeProvider(0, "?").hasDynamicWidth)
        assertFalse(makeProvider(0, "").hasDynamicWidth)
    }

    private fun makeProvider(width: Int, unit: String): AdWidthProvider {
        return DefaultAdWidthProvider(Promo.Slot(dynamicWidth = width.toDouble(), dynamicWidthUnit = unit))
    }
}
