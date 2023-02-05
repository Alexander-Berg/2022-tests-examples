package ru.yandex.disk.util

import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class RangesTest {

    @Test
    fun `should parse correctly`() {
        assertThat(Ranges.parse("305-307").contains(305), `is`(true))
        assertThat(Ranges.parse("305-307").contains(306), `is`(true))
        assertThat(Ranges.parse("305-307").contains(307), `is`(true))
        assertThat(Ranges.parse("305").contains(305), `is`(true))


        assertThat(Ranges.parse("305-").contains(305), `is`(true))

        assertThat(Ranges.parse("300").contains(305), `is`(false))

        assertThat(Ranges.parse("305-307,309,311-").contains(305), `is`(true))
        assertThat(Ranges.parse("305-307,309,311-").contains(309), `is`(true))
        assertThat(Ranges.parse("305-307,309,311-").contains(312), `is`(true))
    }
}