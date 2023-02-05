/*
 * Copyright (c) 2019 Yandex LLC. All rights reserved.
 * Author: Kirill Grekhov grehhov@yandex-team.ru
 */

package ru.yandex.direct.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExtensionsTest {
    @Test
    fun replaceNans_shouldWork_withEmptyArray() {
        val actual = floatArrayOf().replaceNansWithZeros()
        val expected = floatArrayOf()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun replaceNans_shouldReplaceSingleValue_ifSingleNan() {
        val actual = floatArrayOf(Float.NaN).replaceNansWithZeros()
        val expected = floatArrayOf(0f)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun replaceNans_shouldReplaceAllValues_ifAllAreNans() {
        val actual = floatArrayOf(Float.NaN, Float.NaN, Float.NaN).replaceNansWithZeros()
        val expected = floatArrayOf(0f, 0f, 0f)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun replaceNans_shouldReplaceFirstValues_ifFirstAreNans() {
        val actual = floatArrayOf(Float.NaN, Float.NaN, 1f, 2f, 3f).replaceNansWithZeros()
        val expected = floatArrayOf(0f, 0f, 1f, 2f, 3f)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun replaceNans_shouldReplaceLastValues_ifLastAreNans() {
        val actual = floatArrayOf(1f, 2f, 3f, Float.NaN, Float.NaN).replaceNansWithZeros()
        val expected = floatArrayOf(1f, 2f, 3f, 0f, 0f)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun replaceNans_shouldReplaceValuesOnTwoEnds_ifTheyAreNans() {
        val actual = floatArrayOf(Float.NaN, 1f, Float.NaN).replaceNansWithZeros()
        val expected = floatArrayOf(0f, 1f, 0f)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun replaceNans_shouldKeepArrayAsIs_ifItContainsNoNans() {
        val actual = floatArrayOf(1f, 2f, 3f).replaceNansWithZeros()
        val expected = floatArrayOf(1f, 2f, 3f)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun replaceNans_shouldReplaceValuesOnTwoEndsAndInMiddle_ifTheyAreNans() {
        val actual = floatArrayOf(Float.NaN, 1f, Float.NaN, 3f, Float.NaN).replaceNansWithZeros()
        val expected = floatArrayOf(0f, 1f, 0f, 3f, 0f)
        assertThat(actual).isEqualTo(expected)
    }
}