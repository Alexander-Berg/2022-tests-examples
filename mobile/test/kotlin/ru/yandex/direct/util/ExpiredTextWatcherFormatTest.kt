// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ExpiredTextWatcherFormatTest(
    val source: String,
    val formatted: String
) {
    @Test
    fun findNewCursorPositionTest() {
        assertThat(ExpiredTextWatcher.formatText(source)).isEqualTo(formatted)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Source=\"{0}\"; expected=\"{1}\"")
        fun provideParameters(): Collection<Array<String>> = listOf(
                arrayOf("", ""),
                arrayOf("1", "1"),
                arrayOf("12", "12/"),
                arrayOf("123", "12/3"),
                arrayOf("1234", "12/34"),
                arrayOf("12345", "12/345"),
                arrayOf(" ", ""),
                arrayOf(".", ""),
                arrayOf(",", ""),
                arrayOf(" 1", "1"),
                arrayOf("1 ", "1"),
                arrayOf(" 12", "12/"),
                arrayOf("1 2", "12/"),
                arrayOf("12 ", "12/"),
                arrayOf(" 1234", "12/34"),
                arrayOf("1 234",  "12/34"),
                arrayOf("12 34",  "12/34"),
                arrayOf("123 4",  "12/34"),
                arrayOf("1234 ",  "12/34"),
                arrayOf("1 2 3 4", "12/34"),
                arrayOf("12 anime 34", "12/34")
        )
    }
}