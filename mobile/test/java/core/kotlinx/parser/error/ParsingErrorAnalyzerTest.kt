package core.kotlinx.parser.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.RuntimeException

class ParsingErrorAnalyzerTest {

    private val analyzer = ParsingErrorAnalyzer

    @Test
    fun `Check analyze of missing field error`() {
        @Serializable
        class TestType(
            val field: String
        )

        val (data, error) = try {
            success(Json.decodeFromString<TestType>("{}"))
        } catch (e: Throwable) {
            error(e)
        }

        assertThat(data).isEqualTo(null)
        assertThat(error).isNotEqualTo(null)
        assertThat(analyzer.analyze(requireNotNull(error))).isEqualTo("отсутствуют обязательные для типа 'core.kotlinx.parser.error.ParsingErrorAnalyzerTest.Check analyze of missing field error.TestType' поля 'field'")
    }

    @Test
    fun `Check analyze of missing serializer error`() {
        val (data, error) = try {
            val json = Json {
                ignoreUnknownKeys = true
                serializersModule = SerializersModule {
                    polymorphic(BaseType::class) {
                        subclass(FooType::class)
                        subclass(BarType::class)
                    }
                }
            }
            success(json.decodeFromString<BaseType>("""{"type": "baz"}"""))
        } catch (e: Throwable) {
            error(e)
        }

        assertThat(data).isEqualTo(null)
        assertThat(error).isNotEqualTo(null)
        assertThat(analyzer.analyze(requireNotNull(error))).isEqualTo("не найден парсер для типа 'baz'")
    }

    @Test
    fun `Check analyze of unknown error`() {
        assertThat(ParsingErrorAnalyzer.analyze(RuntimeException())).isEqualTo(null)
    }

    @Serializable
    @SerialName("base")
    private abstract class BaseType

    @Serializable
    @SerialName("foo")
    private class FooType : BaseType()

    @Serializable
    @SerialName("bar")
    private class BarType : BaseType()

    private fun <T> success(data: T) = Result(data, null)

    private fun error(throwable: Throwable) = Result(null, throwable)

    private data class Result<T>(val data: T?, val error: Throwable?)
}
