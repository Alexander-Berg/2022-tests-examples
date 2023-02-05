package core.kotlinx.parser.safe

import core.kotlinx.parser.error.ParserErrorChannel
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions
import org.junit.Test

class SafeParserTest {

    @Test
    fun `Check parsing of correct value`() {
        val expected = true
        val actual = Json.decodeFromString(
            SafeParser(
                Boolean.serializer(),
                TestChannel
            ) { _, _, _ -> false },
            "true"
        )
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Check parsing of incorrect value`() {
        val expected = false
        val actual = Json.decodeFromString(
            SafeParser(
                Boolean.serializer(),
                TestChannel
            ) { _, _, _ -> false },
            "42"
        )
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    private object TestChannel : ParserErrorChannel()
}
