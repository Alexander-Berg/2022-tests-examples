package ru.yandex.market.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class ObjectExtensionsTest {

    @RunWith(Parameterized::class)
    class EqualsByTest(
        private val first: TestClass,
        private val second: TestClass,
        private val areEquals: Boolean
    ) {
        @Test
        fun `Method return expected value`() {
            val result = first.equalsBy(second, { it.i }, { it.s })
            assertThat(result).isEqualTo(areEquals)
        }

        companion object {

            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                arrayOf(TestClass(), TestClass(), true),
                arrayOf(TestClass(), TestClass(b = true), true),
                arrayOf(TestClass(), TestClass(i = 1), false),
                arrayOf(TestClass(), TestClass(s = "1"), false),
                arrayOf(TestClass(), TestClass(s = null), false),
                arrayOf(TestClass(i = null), TestClass(i = null), true)
            )
        }
    }
}

data class TestClass(
    val i: Int? = 0,
    val s: String? = "",
    val b: Boolean? = false
)