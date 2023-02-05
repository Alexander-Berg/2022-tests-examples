package ru.yandex.market.utils

import android.os.Build
import android.text.SpannedString
import com.annimon.stream.test.hamcrest.OptionalIntMatcher.hasValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(Enclosed::class)
class CharSequenceExtensionsTest {

    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class LastIndexBetweenTest {

        @Test
        fun `fun`() {
            assertThat(SpannedString("").toString()).isEqualTo(SpannedString("").toString())
        }

        @Test
        fun properlyFindsIndexOfCharInTheMiddleOfRange() {
            val index = "ABCDE".lastIndexBetween(
                char = 'C',
                startInclusive = 0,
                endInclusive = 4
            )

            assertThat(index).has(HamcrestCondition(hasValue(2)))
        }

        @Test
        fun properlyFindsIndexOfCharInTheStartOfRange() {
            val index = "ABCDE".lastIndexBetween(
                char = 'A',
                startInclusive = 0,
                endInclusive = 4
            )

            assertThat(index).has(HamcrestCondition(hasValue(0)))
        }

        @Test
        fun properlyFindsIndexOfCharInTheEndOfRange() {
            val index = "ABCDE".lastIndexBetween(
                char = 'E',
                startInclusive = 0,
                endInclusive = 4
            )

            assertThat(index).has(HamcrestCondition(hasValue(4)))
        }

        @Test(expected = IllegalArgumentException::class)
        fun throwsExceptionWhenStartIsOutOfTextBounds() {
            "ABCDE".lastIndexBetween(
                char = 'A',
                startInclusive = -1,
                endInclusive = 4
            )
        }

        @Test(expected = IllegalArgumentException::class)
        fun throwsExceptionWhenEndIsOutOfTextBounds() {
            "ABCDE".lastIndexBetween(
                char = 'A',
                startInclusive = 0,
                endInclusive = -1
            )
        }

        @Test(expected = IllegalArgumentException::class)
        fun throwsExceptionWhenStartIsGreaterThanEnd() {
            "ABCDE".lastIndexBetween(
                char = 'A',
                startInclusive = 3,
                endInclusive = 2
            )
        }
    }
}