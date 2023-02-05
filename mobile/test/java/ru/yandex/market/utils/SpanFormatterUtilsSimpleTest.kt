package ru.yandex.market.utils

import android.os.Build
import android.text.SpannedString
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SpanFormatterUtilsSimpleTest {

    @Test
    fun `Correctly insert arguments into source char sequence 0`() {
        val source = "hello"
        val arguments = emptyArray<Any>()
        val expected = SpannedString("hello")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }

    @Test
    fun `Correctly insert arguments into source char sequence 1`() {
        val source = "от %1${'$'}s до %2${'$'}s"
        val arguments = arrayOf("1", "2")
        val expected = SpannedString("от 1 до 2")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }

    @Test
    fun `Correctly insert arguments into source char sequence 2`() {
        val source = "от %1${'$'}s до %2${'$'}s"
        val arguments = arrayOf("1", "2", "3")
        val expected = SpannedString("от 1 до 2")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }

    @Test
    fun `Correctly insert arguments into source char sequence 3`() {
        val source = "от %1${'$'}s до %2${'$'}s"
        val arguments = arrayOf("1")
        val expected = SpannedString("от 1 до %2${'$'}s")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }

    @Test
    fun `Correctly insert arguments into source char sequence 4`() {
        val source = "от %1${'$'}s до %2${'$'}s"
        val arguments = arrayOf("1", "2")
        val expected = SpannedString("от 1 до 2")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }

    @Test
    fun `Correctly insert arguments into source char sequence 5`() {
        val source = "hello"
        val arguments = arrayOf("1", "2", "3")
        val expected = SpannedString("hello")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }

    @Test
    fun `Correctly insert arguments into source char sequence 6`() {
        val source = ""
        val arguments = arrayOf("1", "2")
        val expected = SpannedString("")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }

    @Test
    fun `Correctly insert arguments into source char sequence 7`() {
        val source = "от %1${'$'}s до %2${'$'}s"
        val arguments = arrayOf("от %1${'$'}s до %2${'$'}s")
        val expected = SpannedString("от от %1${'$'}s до %2${'$'}s до %2${'$'}s")
        val mapped = SpanFormatterUtils.format(source, *arguments)
        Assert.assertThat(mapped.toString(), Matchers.equalTo(expected.toString()))
    }
}