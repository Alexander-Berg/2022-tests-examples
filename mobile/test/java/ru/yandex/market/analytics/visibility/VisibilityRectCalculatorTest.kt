package ru.yandex.market.analytics.visibility

import android.graphics.Rect
import android.os.Build
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class VisibilityRectCalculatorTest : TestCase() {

    @Test
    fun `Check 100 percent intersection`() {
        val calculator = VisibilityRectCalculator()
        val elevetedRect = Rect(0, 0, 100, 100)
        val treckableRect = Rect(10, 10, 20, 20)
        val res = calculator.calculateIntersection(elevetedRect, 1f, treckableRect, 0f)
        assertEquals(100.0, res)
    }

    @Test
    fun `Check 50 percent intersection`() {
        val calculator = VisibilityRectCalculator()
        val elevetedRect = Rect(0, 0, 100, 100)
        val treckableRect = Rect(50, 0, 150, 100)
        val res = calculator.calculateIntersection(elevetedRect, 1f, treckableRect, 0f)
        assertEquals(50.0, res)
    }

    @Test
    fun `Check no intersection in same elevation`() {
        val calculator = VisibilityRectCalculator()
        val elevetedRect = Rect(0, 0, 100, 100)
        val treckableRect = Rect(50, 0, 150, 100)
        val res = calculator.calculateIntersection(elevetedRect, 0f, treckableRect, 0f)
        assertEquals(0.0, res)
    }

    @Test
    fun `Check no intersection`() {
        val calculator = VisibilityRectCalculator()
        val elevetedRect = Rect(0, 0, 100, 100)
        val treckableRect = Rect(300, 300, 350, 400)
        val res = calculator.calculateIntersection(elevetedRect, 0f, treckableRect, 0f)
        assertEquals(0.0, res)
    }

    @Test
    fun `Check bottom 50 percent intersection`() {
        val calculator = VisibilityRectCalculator()
        val treckableRect = Rect(0, 3950, 100, 4050)
        val treckableZize = Rect(0, 0, 100, 100)
        val windowsRect = Rect(0, 0, 3000, 4000)
        val res = calculator.calculateBottomIntersection(treckableRect, windowsRect, treckableZize, 0f)
        assertEquals(50.0, res)
    }

    @Test
    fun `Check top 50 percent intersection`() {
        val calculator = VisibilityRectCalculator()
        val treckableRect = Rect(0, -50, 100, 50)
        val treckableZize = Rect(0, 0, 100, 100)
        val windowsRect = Rect(0, 0, 3000, 4000)
        val res = calculator.calculateTopIntersection(treckableRect, windowsRect, treckableZize, 0f)
        assertEquals(50.0, res)
    }

    @Test
    fun `Check left 50 percent intersection`() {
        val calculator = VisibilityRectCalculator()
        val treckableRect = Rect(-50, 0, 50, 100)
        val treckableSize = Rect(0, 0, 100, 100)
        val windowsRect = Rect(0, 0, 3000, 4000)
        val res = calculator.calculateLeftIntersection(treckableRect, windowsRect, treckableSize, 0f)
        assertEquals(50.0, res)
    }

    @Test
    fun `Check right 50 percent intersection`() {
        val calculator = VisibilityRectCalculator()
        val treckableRect = Rect(2950, 0, 3050, 100)
        val treckableSize = Rect(0, 0, 100, 100)
        val windowsRect = Rect(0, 0, 3000, 4000)
        val res = calculator.calculateRightIntersection(treckableRect, windowsRect, treckableSize, 0f)
        assertEquals(50.0, res)
    }

    @Test
    fun `Check right no intersection`() {
        val calculator = VisibilityRectCalculator()
        val treckableRect = Rect(2900, 0, 3000, 100)
        val treckableSize = Rect(0, 0, 100, 100)
        val windowsRect = Rect(0, 0, 3000, 4000)
        val res = calculator.calculateRightIntersection(treckableRect, windowsRect, treckableSize, 0f)
        assertEquals(0.0, res)
    }

    @Test
    fun `Check left no intersection`() {
        val calculator = VisibilityRectCalculator()
        val treckableRect = Rect(0, 0, 100, 100)
        val treckableSize = Rect(0, 0, 100, 100)
        val windowsRect = Rect(0, 0, 3000, 4000)
        val res = calculator.calculateLeftIntersection(treckableRect, windowsRect, treckableSize, 0f)
        assertEquals(0.0, res)
    }

    @Test
    fun `Check left no intersection second`() {
        val calculator = VisibilityRectCalculator()
        val treckableRect = Rect(2900, 0, 3000, 100)
        val treckableSize = Rect(0, 0, 100, 100)
        val windowsRect = Rect(0, 0, 3000, 4000)
        val res = calculator.calculateLeftIntersection(treckableRect, windowsRect, treckableSize, 0f)
        assertEquals(0.0, res)
    }
}