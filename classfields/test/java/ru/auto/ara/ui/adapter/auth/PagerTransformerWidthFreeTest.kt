package ru.auto.ara.ui.adapter.auth

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.util.viewpager.TransformerWidthFree

/**
 * @author dumchev on 03/10/2018.
 */
@RunWith(AllureRunner::class) class PagerTransformerWidthFreeTest {

    private val width = 100

    @Test
    fun testNormalization() {
        val factor = 1f
        val normalize = TransformerWidthFree.Normalize(
                percentRangeMax = factor,
                itemWidth = width
        )
        TestNormalization(width, factor, normalize).run {
            invoke(forScrollDistance = 0, expectedNormalizedInPercent = 0f)
            invoke(forScrollDistance = 50, expectedNormalizedInPercent = 0.5f)
            invoke(forScrollDistance = -50, expectedNormalizedInPercent = -0.5f)
            invoke(forScrollDistance = 100, expectedNormalizedInPercent = 1f)
        }
    }

    @Test
    fun `testNormalization with max range == 0,5f`() {

        val factor = 0.5f
        val normalize = TransformerWidthFree.Normalize(
                percentRangeMax = factor,
                itemWidth = width
        )
        TestNormalization(width, factor, normalize).run {
            invoke(forScrollDistance = 0, expectedNormalizedInPercent = 0f)
            invoke(forScrollDistance = 50, expectedNormalizedInPercent = 0.25f)
            invoke(forScrollDistance = -50, expectedNormalizedInPercent = -0.25f)
            invoke(forScrollDistance = 100, expectedNormalizedInPercent = 0.5f)
        }
    }

    private class TestNormalization(
            private val width: Int,
            private val rangeMax: Float,
            private val normalize: TransformerWidthFree.Normalize
    ) {
        operator fun invoke(
                forScrollDistance: Int,
                expectedNormalizedInPercent: Float
        ) {
            val result = normalize.factor(forScrollDistance)
            check(result == expectedNormalizedInPercent) {
                """expected $expectedNormalizedInPercent
                   for distance $forScrollDistance,width $width and rangeMax $rangeMax
                   but have $result
                   """.trimIndent()
            }
        }
    }
}
