package com.yandex.launcher.wallpapers

import android.graphics.Color
import android.os.Handler
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import org.junit.Test

/**
 * It is difficult to make unit tests for [PreviousWallpaperLoader.calculateAverageColors], because bitmaps are shadowed by Robolectric
 */
class PreviousWallpaperLoaderTest : BaseRobolectricTest() {
    @Test
    fun `one set of color, match to itself`() {
        val loader = PreviousWallpaperLoader(mock(), Handler())

        val colors = createRandomColors(loader.TOTAL_SAMPLES_COUNT)

        assertThat(loader.isColorsMatch(colors, colors), equalTo(true))
    }

    @Test
    fun `two random set of colors, not matched`() {
        val loader = PreviousWallpaperLoader(mock(), Handler())

        val colors1 = createRandomColors(loader.TOTAL_SAMPLES_COUNT)
        val colors2 = createRandomColors(loader.TOTAL_SAMPLES_COUNT)

        assertThat(loader.isColorsMatch(colors1, colors2), equalTo(false))
    }

    @Test
    fun `two random set of colors, with different size, not matched`() {
        val loader = PreviousWallpaperLoader(mock(), Handler())

        val colors1 = createRandomColors(loader.TOTAL_SAMPLES_COUNT)
        val colors2 = createRandomColors(loader.TOTAL_SAMPLES_COUNT / 2)

        assertThat(loader.isColorsMatch(colors1, colors2), equalTo(false))
    }

    @Test
    fun `slightly altered colors, match to original`() {
        val loader = PreviousWallpaperLoader(mock(), Handler())

        val colors1 = createRandomColors(loader.TOTAL_SAMPLES_COUNT)
        val colors2 = alterColors(colors1, loader.MATCH_COLOR_THRESHOLD_VALUE)

        assertThat(loader.isColorsMatch(colors1, colors2), equalTo(true))
    }

    @Test
    fun `massive altered colors, not match to original`() {
        val loader = PreviousWallpaperLoader(mock(), Handler())

        val colors1 = createRandomColors(loader.TOTAL_SAMPLES_COUNT)
        val colors2 = alterColors(colors1, loader.MATCH_COLOR_THRESHOLD_VALUE * 3)

        assertThat(loader.isColorsMatch(colors1, colors2), equalTo(false))
    }

    @Test
    fun `massive altered minor part of colors, match to original`() {
        val loader = PreviousWallpaperLoader(mock(), Handler())

        val colors1 = createRandomColors(loader.TOTAL_SAMPLES_COUNT)

        val colors2 = alterColors(
            origColors = colors1,
            alterDelta = loader.MATCH_COLOR_THRESHOLD_VALUE * 3,
            alterFirst = colors1.size - loader.MATCHED_COLORS_COUNT_THRESHOLD
        )

        assertThat(loader.isColorsMatch(colors1, colors2), equalTo(true))
    }

    private fun alterColors(
        origColors: IntArray,
        alterDelta: Int,
        alterFirst: Int = origColors.size
    ): IntArray {

        fun getRandomAlterValue() = (alterDelta * 2) * Math.random() - alterDelta

        val result = origColors.copyOf()
        repeat(alterFirst) { i ->
            val color = result[i]

            val red = Color.red(color) + getRandomAlterValue()
            val green = Color.green(color) + getRandomAlterValue()
            val blue = Color.blue(color) + getRandomAlterValue()

            result[i] = createSafeColor(red, green, blue)
        }

        return result
    }

    private fun createSafeColor(red: Double, green: Double, blue: Double): Int {

        fun Double.fitBounds() = maxOf(minOf(this.toInt(), 255), 0)

        return Color.argb(255, red.fitBounds(), green.fitBounds(), blue.fitBounds())
    }

    private fun createRandomColors(size: Int): IntArray {
        return IntArray(size) { getRandomColor() }
    }

    private fun getRandomColor(): Int {
        return Color.argb(
            255,
            (255 * Math.random()).toInt(),
            (255 * Math.random()).toInt(),
            (255 * Math.random()).toInt()
        )
    }
}
