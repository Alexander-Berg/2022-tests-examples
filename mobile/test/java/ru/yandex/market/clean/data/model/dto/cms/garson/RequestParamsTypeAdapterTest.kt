package ru.yandex.market.clean.data.model.dto.cms.garson

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.utils.Json

class RequestParamsTypeAdapterTest {

    private val typeAdapter = RequestParamsDto.RequestParamsTypeAdapter()

    @Test
    fun `Empty params when empty object`() {
        val requestParamsDTO = typeAdapter.deserialize(Json.buildObject {}, null, null)
        assertThat(requestParamsDTO.params.size).isEqualTo(0)
    }

    @Test
    fun `Params ignore empty inner objects`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject { "inner" to obj {} }, null, null
        )
        assertThat(requestParamsDTO.params.size).isEqualTo(0)
    }

    @Test
    fun `Params expand inner objects`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject {
                "inner" to obj {
                    "boolean" to true
                    "double" to 1.2
                    "long" to 1L
                    "int" to 1
                    "string" to "sequence"
                    "array" to array(listOf(1, 2, 3))
                }
            }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("boolean", "double", "long", "int", "string", "array")
        assertThat(
            requestParamsDTO.params.values
        ).contains(
            listOf("true"), listOf("1.2"), listOf("1"), listOf("1"), listOf("sequence"), listOf("1", "2", "3")
        )
    }

    @Test
    fun `Params give param from outer object, not inner`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject {
                "string" to "sequence1"
                "inner" to obj {
                    "string" to "sequence2"
                }
            }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("string")
        assertThat(requestParamsDTO.params.values).contains(listOf("sequence1"))
    }

    @Test
    fun `Params give param from outer object, not inner if inner positions before outer`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject {
                "inner" to obj {
                    "string" to "sequence2"
                }
                "string" to "sequence1"
            }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("string")
        assertThat(requestParamsDTO.params.values).contains(listOf("sequence1"))
    }

    @Test
    fun `Params contains boolean`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject { "boolean" to true }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("boolean")
        assertThat(requestParamsDTO.params.values).contains(listOf("true"))
    }

    @Test
    fun `Params contains double`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject { "double" to 1.2 }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("double")
        assertThat(requestParamsDTO.params.values).contains(listOf("1.2"))
    }

    @Test
    fun `Params contains long`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject { "long" to 1L }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("long")
        assertThat(requestParamsDTO.params.values).contains(listOf("1"))
    }

    @Test
    fun `Params contains int`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject { "int" to 1 }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("int")
        assertThat(requestParamsDTO.params.values).contains(listOf("1"))
    }

    @Test
    fun `Params contains String`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject { "string" to "sequence" }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("string")
        assertThat(requestParamsDTO.params.values).contains(listOf("sequence"))
    }

    @Test
    fun `Params contains arrays`() {
        val requestParamsDTO = typeAdapter.deserialize(
            Json.buildObject { "array" to array(listOf(1, 2, 3)) }, null, null
        )
        assertThat(requestParamsDTO.params.keys).contains("array")
        assertThat(requestParamsDTO.params.values).contains(listOf("1", "2", "3"))
    }
}