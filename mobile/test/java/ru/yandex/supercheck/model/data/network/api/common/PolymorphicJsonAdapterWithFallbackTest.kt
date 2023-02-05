package ru.yandex.supercheck.model.data.network.api.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class PolymorphicJsonAdapterWithFallbackTest {

    lateinit var adapter: JsonAdapter<BaseClass>

    @Before
    fun setUp() {
        val polymorphicJsonAdapterFactory =
            PolymorphicJsonAdapterFactory.of(BaseClass::class.java, "type")
                .withSubtype(BaseClass.ChildWithIntValue::class.java, "int")
                .withSubtype(BaseClass.ChildWithStringValue::class.java, "string")
                .withSubtype(BaseClass.ChildWithBooleanValue::class.java, "boolean")

        val fallbackJsonAdapterFactory = FallbackJsonAdapterFactory(
            factory = polymorphicJsonAdapterFactory,
            fallbackObject = BaseClass.EmptyChild
        )

        val moshi = Moshi.Builder()
            .add(fallbackJsonAdapterFactory)
            .add(KotlinJsonAdapterFactory())
            .build()

        adapter = moshi.adapter(BaseClass::class.java)
    }

    @Test
    fun testWithIntValue() {

        val jsonWithIntValue = """
            {
                "type": "int",
                "value": 9
            }
        """.trimIndent()

        val childWithIntValue = adapter.fromJson(jsonWithIntValue)

        assertThat(childWithIntValue, instanceOf(BaseClass.ChildWithIntValue::class.java))
        assertEquals(9, (childWithIntValue as BaseClass.ChildWithIntValue).value)
    }

    @Test
    fun testWithIncorrectValue() {

        val jsonWithIncorrectValue = """
            {
                "type": "boolean",
                "value": 22142441
            }
        """.trimIndent()

        val childWithIncorrectValue = adapter.fromJson(jsonWithIncorrectValue)

        assertThat(childWithIncorrectValue, instanceOf(BaseClass.EmptyChild::class.java))
    }

    @Test
    fun testWithUnknownType() {

        val jsonWithUnknownType = """
            {
                "type": "unknown",
                "value": 22142441,
                "params": "..."
            }
        """.trimIndent()

        val childWithUnknownType = adapter.fromJson(jsonWithUnknownType)

        assertThat(childWithUnknownType, instanceOf(BaseClass.EmptyChild::class.java))
    }

    @Test
    fun testWithEmptyJsonObject() {

        val jsonWithEmptyJsonObject = "{}"

        val childFromEmptyJson = adapter.fromJson(jsonWithEmptyJsonObject)

        assertThat(childFromEmptyJson, instanceOf(BaseClass.EmptyChild::class.java))
    }

    sealed class BaseClass(@Json(name = TYPE_TAG) val type: String) {

        companion object {
            const val TYPE_TAG = "type"
            const val VALUE_TAG = "value"

            const val TYPE_INT = "int"
            const val TYPE_STRING = "string"
            const val TYPE_BOOLEAN = "boolean"

            const val TYPE_EMPTY = "empty"
        }

        data class ChildWithIntValue(@Json(name = VALUE_TAG) val value: Int) : BaseClass(TYPE_INT)

        data class ChildWithStringValue(
            @Json(name = VALUE_TAG) val value: String
        ) : BaseClass(TYPE_STRING)

        data class ChildWithBooleanValue(
            @Json(name = VALUE_TAG) val value: Boolean
        ) : BaseClass(TYPE_BOOLEAN)

        object EmptyChild : BaseClass(TYPE_EMPTY)
    }
}