package com.edadeal.android.model.webapp

import com.edadeal.android.model.webapp.handler.datasync.DataSyncField
import com.edadeal.android.model.webapp.handler.datasync.DataSyncHandlerError
import com.edadeal.android.util.Utils
import com.nhaarman.mockito_kotlin.mock
import com.yandex.datasync.Datatype
import com.yandex.datasync.internal.model.FieldChangeType
import com.yandex.datasync.internal.model.ValueDto
import com.yandex.datasync.wrappedModels.Value
import org.hamcrest.Description
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DataSyncFieldTest {

    @Test
    fun `parse should return correct fields`() {
        val json = "{\"a\":\"text\",\"b\":true,\"c\":42,\"d\":3.01,\"list\":[\"a\",1,1.1]}"
        val expected = listOf(
            "a" to DataSyncField.StringField("text"),
            "b" to DataSyncField.BooleanField(true),
            "c" to DataSyncField.NumberField(42),
            "d" to DataSyncField.NumberField(3.01),
            "list" to DataSyncField.ListField(
                listOf(
                    DataSyncField.StringField("a"),
                    DataSyncField.NumberField(1),
                    DataSyncField.NumberField(1.1)
                )
            )
        )

        val itemMatchers = expected.map { PairMatcher(it) }
        assertThat(DataSyncField.parse(json), contains(itemMatchers))
    }

    @Test
    fun `parse should fails if input data is invalid`() {
        val json = "{\"obj\":{\"a\":1}}"

        assertFailsWith<DataSyncHandlerError> { DataSyncField.parse(json) }
    }

    @Test
    fun `should return correct fields from values`() {
        val values = listOf(
            makeValue(Datatype.STRING to "text"),
            makeValue(Datatype.NULL to ""),
            makeValue(Datatype.DOUBLE to "3.14"),
            makeValue(Datatype.INTEGER to "42"),
            makeValue(Datatype.DOUBLE to "21"),
            makeValue(Datatype.BOOLEAN to "false"),
            makeValue(Datatype.NULL to "", Datatype.STRING to "", Datatype.BOOLEAN to "true")
        )
        val expected = listOf(
            DataSyncField.StringField("text"),
            DataSyncField.NullField,
            DataSyncField.NumberField(3.14),
            DataSyncField.NumberField(42),
            DataSyncField.NumberField(21),
            DataSyncField.BooleanField(false),
            DataSyncField.ListField(
                listOf(
                    DataSyncField.NullField,
                    DataSyncField.StringField(""),
                    DataSyncField.BooleanField(true)
                )
            )
        )

        val actual = values.map { DataSyncField.from(it) }
        assertEquals(expected, actual)
    }

    private fun makeValue(vararg input: Pair<Datatype, String>): Value {
        val value = makeValueDto(input.asList())
        return Value(mock(), mock(), "", "", "", "", mock(), value)
    }

    private fun makeValueDto(input: List<Pair<Datatype, String>>): ValueDto {
        val listValues = when (input.size > 1) {
            true -> input.map { makeValueDto(listOf(it)) }
            else -> emptyList()
        }
        val type = if (input.size > 1) Datatype.LIST else input[0].first
        val value = when {
            input.size > 1 -> ""
            type == Datatype.NULL -> "true"
            else -> input[0].second
        }
        return ValueDto(type, value, 0L, 0, 0, FieldChangeType.SET, listValues)
    }

    class PairMatcher(
        private val pair: Pair<String, DataSyncField>
    ) : TypeSafeMatcher<Pair<String, DataSyncField>>() {

        override fun describeTo(description: Description) {
            description.appendText(describe(pair))
        }
        override fun matchesSafely(item: Pair<String, DataSyncField>): Boolean {
            return pair.first == item.first && pair.second == item.second
        }

        override fun describeMismatchSafely(
            item: Pair<String, DataSyncField>,
            mismatchDescription: Description
        ) {
            mismatchDescription.appendText(describe(item))
        }

        private fun describe(item: Pair<String, DataSyncField>): String {
            return "${item.second.javaClass.simpleName}(${item.first}: ${describe(item.second)})"
        }

        private fun describe(field: DataSyncField): String {
            return when (field) {
                is DataSyncField.StringField -> field.value
                is DataSyncField.NumberField -> describe(field)
                is DataSyncField.BooleanField -> field.value.toString()
                is DataSyncField.NullField -> "null"
                is DataSyncField.ListField -> field.values.joinToString(prefix = "[", postfix = "]") { describe(it) }
            }
        }

        private fun describe(field: DataSyncField.NumberField): String {
            val integer = field.value.toLong()
            return when (Utils.isEquals(integer.toDouble(), field.value)) {
                true -> integer.toString()
                else -> "%.2f".format(Locale.US, field.value)
            }
        }
    }
}
