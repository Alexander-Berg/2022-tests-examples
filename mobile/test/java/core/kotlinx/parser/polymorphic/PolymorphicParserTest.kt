package core.kotlinx.parser.polymorphic

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PolymorphicParserTest {

    private val json by lazy { Json { ignoreUnknownKeys = true } }

    @Test
    fun `Check parsing of supported type`() {
        val expected = FooType(true)
        val actual = json.decodeFromString(
            MyTypeParser(),
            """
                {
                    "type": "foo",
                    "value": true
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test(expected = SerializationException::class)
    fun `Check parsing of unsupported type`() {
        json.decodeFromString(
            MyTypeParser(),
            """
                {
                    "type": "baz",
                    "value": true
                }
            """.trimIndent()
        )
    }

    @Serializable
    private abstract class BaseType

    @Serializable
    private data class FooType(
        val value: Boolean
    ) : BaseType()

    @Serializable
    private data class BarType(
        val value: Int
    ) : BaseType()

    private class MySerializerSelector : SerializerSelector<BaseType> {

        override fun select(typeName: String): KSerializer<out BaseType>? {
            return when (typeName) {
                "foo" -> FooType.serializer()
                "bar" -> BarType.serializer()
                else -> null
            }
        }
    }

    private class MyTypeParser : PolymorphicParser<BaseType>(
        typeFieldKey = "type",
        selector = MySerializerSelector()
    )
}
