// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ExpiredTextWatcherCursorTest(
    val source: String,
    val formatted: String,
    val oldCursorPosition: Int,
    val expectedPosition: Int,
    val isSlashRemoved: Boolean
) {
    @Test
    fun findNewCursorPositionTest() {
        assertThat(ExpiredTextWatcher.findNewCursorPosition(source, formatted, oldCursorPosition, isSlashRemoved))
                .isEqualTo(expectedPosition)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Given \"{0}\" to \"{1}\" with cursor at {2} returns {3}")
        fun provideParameters(): Collection<Array<Any>> = listOf(
                arrayOf("",         "",         -1,         0,      false),
                arrayOf("2",        "",         -1,         0,      false),
                arrayOf("2",        "2",        -1,         0,      false),
                arrayOf("",         "",         0,          0,      false),
                arrayOf("2",        "",         1,          0,      false),
                arrayOf("22",       "",         2,          0,      false),
                arrayOf("2",        "2",        0,          0,      false),
                arrayOf("2",        "2",        1,          1,      false),
                arrayOf("2",        "2",        2,          1,      false),
                arrayOf("22",       "22",       0,          0,      false),
                arrayOf("22",       "22",       1,          1,      false),
                arrayOf("22",       "22",       2,          2,      false),

                arrayOf("22",       "22/",      0,          0,      false),
                arrayOf("22",       "22/",      1,          1,      false),
                arrayOf("22",       "22/",      2,          3,      false),
                arrayOf("222",      "22/2",     0,          0,      false),
                arrayOf("222",      "22/2",     1,          1,      false),
                arrayOf("222",      "22/2",     2,          3,      false),
                arrayOf("222",      "22/2",     3,          4,      false),
                arrayOf("2222",     "22/22",    0,          0,      false),
                arrayOf("2222",     "22/22",    1,          1,      false),
                arrayOf("2222",     "22/22",    2,          3,      false),
                arrayOf("2222",     "22/22",    3,          4,      false),

                arrayOf("22",       "22/",      0,          0,      true),
                arrayOf("22",       "22/",      1,          1,      true),
                arrayOf("22",       "22/",      2,          2,      true),
                arrayOf("222",      "22/2",     0,          0,      true),
                arrayOf("222",      "22/2",     1,          1,      true),
                arrayOf("222",      "22/2",     2,          2,      true),
                arrayOf("222",      "22/2",     3,          3,      true),
                arrayOf("2222",     "22/22",    0,          0,      true),
                arrayOf("2222",     "22/22",    1,          1,      true),
                arrayOf("2222",     "22/22",    2,          2,      true),
                arrayOf("2222",     "22/22",    3,          3,      true),

                arrayOf(" ",         "",        1,          0,      false),
                arrayOf("2 ",        "2",       0,          0,      false),
                arrayOf("2 ",        "2",       1,          1,      false),
                arrayOf("2 ",        "2",       2,          1,      false),
                arrayOf("2 2",       "22",      0,          0,      false),
                arrayOf("2 2",       "22",      1,          1,      false),
                arrayOf("2 2",       "22",      2,          2,      false),
                arrayOf("2 2",       "22",      3,          2,      false)
        )
    }
}