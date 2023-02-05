package ru.yandex.market.testcase

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import ru.yandex.market.ResourceHelper
import ru.yandex.market.gson.GsonFactory
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs
import java.lang.reflect.Type

abstract class JsonSerializationTestCase {

    private val gson = GsonFactory.get()

    protected abstract val instance: Any
    protected abstract val type: Type
    protected abstract val jsonSource: JsonSource

    private val jsonText: String
        get() {
            return when (val source = jsonSource) {
                is JsonSource.File -> ResourceHelper.getResponse("/json/${source.fileName}")
                is JsonSource.Text -> source.jsonText
            }
        }

    @Test
    fun testJsonSerialization() {
        val expected = jsonText
        val actual = gson.toJson(instance)

        assertThat(actual).`is`(HamcrestCondition(sameJSONAs(expected)))
    }

    @Test
    fun testJsonDeserialization() {
        val json = jsonText
        val deserialize = gson.fromJson<Any>(json, type)

        val expected = instance
        assertThat(deserialize).isEqualTo(expected)
    }

    sealed class JsonSource {

        data class File(val fileName: String) : JsonSource()

        data class Text(val jsonText: String) : JsonSource()
    }

    companion object {

        @JvmStatic
        fun file(fileName: String) = JsonSource.File(fileName)

        @JvmStatic
        fun defaultFile(testClass: Any) = JsonSource.File("${testClass.javaClass.simpleName}.json")

        @JvmStatic
        fun text(jsonText: String) = JsonSource.Text(jsonText)
    }
}
