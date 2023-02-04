package com.yandex.mobile.realty.detekt

import com.yandex.mobile.realty.detekt.rules.SingleExpressionFunction
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author andrikeev on 31/12/2019.
 */
class SingleExpressionFunctionTest {

    @Test
    fun shouldReportMultilineSingleExpressionFunction() {
        val findings = SingleExpressionFunction().lint(
            """
                    fun square(a: Int?): Int = if (a != null) {
                        a * a
                    } else {
                        0
                    }
            """.trimIndent()
        )

        assertEquals(1, findings.size)
        assertEquals("Function 'square' has no block body.", findings.first().message)
    }

    @Test
    fun shouldReportMultilineSingleExpressionFunction2() {
        val findings = SingleExpressionFunction().lint(
            """
                    fun square(a: Int) = 
                            a * a
            """.trimIndent()
        )

        assertEquals(1, findings.size)
        assertEquals("Function 'square' has no block body.", findings.first().message)
    }

    @Test
    fun shouldReportSingleLineSingleExpressionFunction() {
        val findings = SingleExpressionFunction().lint(
            """
                    fun square(a: Int) = a * a
            """.trimIndent()
        )

        assertEquals(1, findings.size)
        assertEquals("Function 'square' has no block body.", findings.first().message)
    }

    @Test
    fun shouldNotReportFunctionWithBlockBody() {
        val findings = SingleExpressionFunction().lint(
            """
                    fun square(a: Int): Int {
                        return a * a
                    }
            """.trimIndent()
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportAbstractFunction() {
        val findings = SingleExpressionFunction().lint(
            """
                    abstract class Math {
                        abstract fun square(a: Int): Int
                    }
            """.trimIndent()
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportInterfaceFunction() {
        val findings = SingleExpressionFunction().lint(
            """
                    interface Math {
                        fun square(a: Int): Int
                    }
            """.trimIndent()
        )

        assertEquals(0, findings.size)
    }
}
