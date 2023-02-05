// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.util.singletones

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NumberUtilsTest(private val first: Float, private val second: Float, private val expected: Boolean) {
    @Test
    fun bitwiseEquals_worksCorrectly() {
        assertThat(NumberUtils.bitwiseEquals(first, second)).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "bitwiseEquals({0}, {1}) == {2}")
        fun parameters(): Collection<Array<Any>> = listOf(
                arrayOf(0F, 0F, true),
                arrayOf(-0F, -0F, true),
                arrayOf(1F, 1F, true),
                arrayOf(-1F, -1F, true),
                arrayOf(0F, 1F, false),
                arrayOf(0F, -1F, false),
                arrayOf(0F, -0F, false),
                arrayOf(-0F, 0F, false),
                arrayOf(1F, -1F, false),

                arrayOf(Float.MAX_VALUE, Float.MAX_VALUE, true),
                arrayOf(Float.MIN_VALUE, Float.MIN_VALUE, true),
                arrayOf(Float.MIN_VALUE, Float.MAX_VALUE, false),
                arrayOf(0F, Float.MAX_VALUE, false),
                arrayOf(0F, Float.MIN_VALUE, false),

                arrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, true),
                arrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, true),
                arrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, false),
                arrayOf(0F, Float.NEGATIVE_INFINITY, false),
                arrayOf(0F, Float.NEGATIVE_INFINITY, false),

                arrayOf(0F, Float.NaN, false),
                arrayOf(-0F, Float.NaN, false),

                // NaNs may be equal if they bitwise representations are the same.
                arrayOf(Float.NaN, Float.NaN, true),

                // If representations of two NaNs are different, method will return false.
                arrayOf(
                        Float.fromBits(0x7ff80000), // NaN
                        Float.fromBits(0x7ff80001), // Also NaN
                        false
                )
        )
    }
}