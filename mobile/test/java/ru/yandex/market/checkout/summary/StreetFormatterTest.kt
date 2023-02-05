package ru.yandex.market.checkout.summary

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.presentation.formatter.StreetFormatter

class StreetFormatterTest {

    private val streetFormatter = StreetFormatter()

    @Test
    fun `get street if both equals`() {
        val result = streetFormatter.format("Street", "District")
        assertThat(result).isEqualTo("Street")
    }

    @Test
    fun `get street if street is ok and district null`() {
        val result = streetFormatter.format("Street", null)
        assertThat(result).isEqualTo("Street")
    }

    @Test
    fun `get district if street is null`() {
        val result = streetFormatter.format(null, "District")
        assertThat(result).isEqualTo("District")
    }

    @Test
    fun `get district if street is empty`() {
        val result = streetFormatter.format("", "District")
        assertThat(result).isEqualTo("District")
    }

    @Test
    fun `get district if street is blank`() {
        val result = streetFormatter.format("    ", "District")
        assertThat(result).isEqualTo("District")
    }
}