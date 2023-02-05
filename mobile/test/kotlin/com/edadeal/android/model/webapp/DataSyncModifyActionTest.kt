package com.edadeal.android.model.webapp

import com.edadeal.android.model.webapp.handler.datasync.DataSyncField
import com.edadeal.android.model.webapp.handler.datasync.DataSyncHandlerError
import com.edadeal.android.model.webapp.handler.datasync.DataSyncModifyAction
import org.hamcrest.Description
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import kotlin.test.assertFailsWith

class DataSyncModifyActionTest {

    @Test
    fun `parse should return correct actions`() {
        val json = """[
{"type":"set","record":{"recordId":"rid.0","collectionId":"cid.0","fields":{"a":null,"b":"str","c":123}}},
{"type":"remove","record":{"recordId":"rid.1","collectionId":"cid.0","fields":{}}},
{"type":"set","record":{"recordId":"rid.1","collectionId":"cid.1","fields":{}}},
{"type":"remove","record":{"recordId":"rid.0","collectionId":"cid.1"}}
        ]""".trimIndent()
        val expected = listOf(
            set("rid.0", "cid.0",
                "a" to DataSyncField.NullField,
                "b" to DataSyncField.StringField("str"),
                "c" to DataSyncField.NumberField(123)
            ),
            remove("rid.1", "cid.0"),
            set("rid.1", "cid.1"),
            remove("rid.0", "cid.1")
        )

        val itemMatchers = expected.map { ActionMatcher(it) }
        MatcherAssert.assertThat(DataSyncModifyAction.parse(json), Matchers.contains(itemMatchers))
    }

    @Test
    fun `parse should fails if input data is invalid`() {
        val json = "[{\"type\":\"set\",\"record\":{\"collection\":1}}]"

        assertFailsWith<DataSyncHandlerError> { DataSyncModifyAction.parse(json) }
    }

    private fun set(
        recordId: String,
        collectionId: String,
        vararg fields: Pair<String, DataSyncField>
    ): DataSyncModifyAction {
        val record = DataSyncModifyAction.Record(
            recordId = recordId,
            collectionId = collectionId,
            fields = fields.asList()
        )
        return DataSyncModifyAction(DataSyncModifyAction.Type.Set, record)
    }

    private fun remove(
        recordId: String,
        collectionId: String
    ): DataSyncModifyAction {
        val record = DataSyncModifyAction.Record(
            recordId = recordId,
            collectionId = collectionId,
            fields = emptyList()
        )
        return DataSyncModifyAction(DataSyncModifyAction.Type.Remove, record)
    }

    class ActionMatcher(
        private val action: DataSyncModifyAction
    ) : TypeSafeMatcher<DataSyncModifyAction>() {

        override fun describeTo(description: Description) {
            describe(description, action)
        }

        override fun matchesSafely(item: DataSyncModifyAction): Boolean {
            return action.type == item.type &&
                action.record.fields == item.record.fields &&
                action.record.recordId == item.record.recordId &&
                action.record.collectionId == item.record.collectionId
        }

        override fun describeMismatchSafely(
            item: DataSyncModifyAction,
            mismatchDescription: Description
        ) {
            describe(mismatchDescription, item)
        }

        private fun describe(
            description: Description,
            action: DataSyncModifyAction
        ) {
            with(description) {
                appendText("{")
                appendText("type=${action.type.name.toLowerCase()}")
                appendText(", record={")
                appendText("collectionId=${action.record.collectionId}")
                appendText(", recordId=${action.record.recordId}")
                appendText(", fields=[")
                for (i in action.record.fields.indices) {
                    val field = action.record.fields[i]
                    DataSyncFieldTest.PairMatcher(field).describeTo(description)
                    if (i < action.record.fields.size - 1) {
                        appendText(", ")
                    }
                }
                appendText("]}}")
            }
        }
    }
}
