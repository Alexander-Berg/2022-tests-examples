package ru.yandex.metro.utils.format.time

import android.content.res.Resources
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.joda.time.Duration
import org.mockito.ArgumentCaptor
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import ru.yandex.metro.R

class DurationFormatSpec : Spek({
    describe("time duration SHORT_MINUTES formatter") {
        val resources = mock<Resources> {
            val quantityCaptor = ArgumentCaptor.forClass(Any::class.java)
            on { getString(eq(R.string.time_hours_short), quantityCaptor.capture()) } doAnswer { "${quantityCaptor.value} h." }
            on { getString(eq(R.string.time_minutes_short), quantityCaptor.capture()) } doAnswer { "${quantityCaptor.value} min." }
        }

        context("input < 0") {
            it("should throw exception") {
                val throwingFunction: () -> Unit = { DurationFormat.formatDuration(resources, Duration.millis(-1)) }
                throwingFunction shouldThrow IllegalArgumentException::class
            }
        }

        context("input range is less than one minute") {
            it("should write 0 minutes") {
                DurationFormat.formatDuration(resources, Duration.ZERO) shouldEqual "0 min."
                DurationFormat.formatDuration(resources, Duration.standardSeconds(29)) shouldEqual "0 min."
            }
            it("should write 1 minute") {
                DurationFormat.formatDuration(resources, Duration.standardSeconds(30)) shouldEqual "1 min."
                DurationFormat.formatDuration(resources, Duration.standardSeconds(59)) shouldEqual "1 min."
            }
        }
        context("input range less than one hour") {
            it("should write time in \" \$1  minutes \" only") {
                DurationFormat.formatDuration(resources, Duration.standardSeconds(60)) shouldEqual "1 min."
                DurationFormat.formatDuration(resources, Duration.standardSeconds(59 * 60)) shouldEqual "59 min."
            }
        }

        context("input equals n hours, where n > 0") {
            it("should write duration in \"\$1 hours\" format") {
                DurationFormat.formatDuration(resources, Duration.standardSeconds(60 * 60)) shouldEqual "1 h."
                DurationFormat.formatDuration(resources, Duration.standardSeconds(60 * 60 * 2)) shouldEqual "2 h."
            }
        }

        context("input equals n hours m minutes, where n > 0 and m > 0") {
            it("should write time in \"\$1 hours \$2 minutes\" format") {
                DurationFormat.formatDuration(resources, Duration.standardSeconds(60 * 60 + 60)) shouldEqual "1 h. 1 min."
            }
        }
    }
})
