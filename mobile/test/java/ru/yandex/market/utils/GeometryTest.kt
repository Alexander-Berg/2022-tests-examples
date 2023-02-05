package ru.yandex.market.utils

import android.graphics.PointF
import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config

@RunWith(Enclosed::class)
class GeometryTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class LineLengthTest(
        private val x1: Float,
        private val y1: Float,
        private val x2: Float,
        private val y2: Float,
        private val expectedResult: Float,
        private val precision: Double
    ) {
        @Test
        fun `Check line length calculation`() {
            val length = lineLength(x1, y1, x2, y2)
            assertThat(length.toDouble()).isCloseTo(expectedResult.toDouble(), within(precision))
        }

        companion object {

            @Parameters(name = "{index}: Line from [{0}, {1}] to [{2}, {3}] length ~ {4}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(0f, 0f, 0f, 0f, 0f, 0.0),
                arrayOf(0f, 0f, 190f, 0f, 190f, 0.0),
                arrayOf(0f, 0f, 0f, 10f, 10f, 0.0),
                arrayOf(0f, 10f, 10f, 0f, 14.14f, 0.01)
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class ConstructDottedLineTest(
        private val startX: Float,
        private val startY: Float,
        private val endX: Float,
        private val endY: Float,
        private val radius: Float,
        private val gap: Float,
        private val centerLine: Boolean,
        private val expectedResult: Array<PointF>
    ) {
        @Test
        fun `Check line length calculation`() {
            val circleCenters = ArrayList<PointF>()
            constructDottedLine(startX, startY, endX, endY, radius, gap, centerLine) { x, y ->
                circleCenters.add(p(x, y))
            }
            if (expectedResult.isEmpty()) {
                assertThat(circleCenters).isEmpty()
            } else {
                assertThat(circleCenters).contains(*expectedResult)
            }
        }

        companion object {

            @Parameters(name = "{index}: Line from [{0}, {1}] to [{2}, {3}] with radius {4}, gap {5} and centerLine {6}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(
                    0f, 0f,
                    6f, 0f,
                    1f, 2f,
                    false,
                    arrayOf(p(1f, 0f), p(5f, 0f))
                ),
                arrayOf(
                    0f, 0f,
                    6f, 0f,
                    3f, 2f,
                    false,
                    arrayOf(p(3f, 0f))
                ),
                arrayOf(
                    0f, 0f,
                    6f, 0f,
                    1f, 6f,
                    false,
                    arrayOf(p(1f, 0f))
                ),
                arrayOf(
                    0f, 0f,
                    6f, 0f,
                    4f, 8f,
                    false,
                    kotlin.emptyArray<PointF>()
                ),
                arrayOf(
                    0f, 0f,
                    6f, 0f,
                    1f, 1f,
                    true,
                    arrayOf(p(1.5f, 0f), p(4.5f, 0f))
                )
            )

            private fun p(x: Float, y: Float) = PointF().also {
                it.x = x
                it.y = y
            }
        }
    }
}