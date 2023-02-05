package core.kotlinx.parser.safe

import core.kotlinx.parser.error.ParserErrorChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SafeListParserTest {

    @Test
    fun `Check parsing of empty array`() {
        val expected = Container(emptyList())
        val actual = Json.decodeFromString<Container>(
            """
                {
                    "items": []
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Check parsing of array with correct elements`() {
        val expected = Container(listOf(1, 2, 3).map { Item(it) })
        val actual = Json.decodeFromString<Container>(
            """
                {
                    "items": [
                        {
                            "value": 1
                        },
                        {
                            "value": 2
                        },
                        {
                            "value": 3
                        }
                    ]
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Check parsing of array with incorrect element`() {
        val expected = Container(listOf(1, 3).map { Item(it) })
        val actual = Json.decodeFromString<Container>(
            """
                {
                    "items": [
                        {
                            "value": 1
                        },
                        {
                            "value": "not a number"
                        },
                        {
                            "value": 3
                        },
                        {
                            "value": true
                        }
                    ]
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Check parsing of non array`() {
        val expected = Container(emptyList())
        val actual = Json.decodeFromString<Container>(
            """
                {
                    "items": {
                        "foo": 42
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Serializable
    private data class Container(
        @Serializable(TestSafeListParser::class)
        val items: List<Item>
    )

    @Serializable
    private data class Item(
        val value: Int
    )

    private object TestParserErrorChannel : ParserErrorChannel()
    private class TestSafeListParser : SafeListParser<Item>(Item.serializer(), TestParserErrorChannel)
}
