package ru.yandex.market.clean.data.mapper.experiment

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.market.analitycs.ExperimentsConfigMapper
import ru.yandex.market.common.experiments.config.ExperimentConfig

class ExperimentsConfigMapperTest {

    private val mapper = ExperimentsConfigMapper()

    @Test
    fun `Empty list return empty array`() {
        val array = mapper.mapExperimentsConfig(emptyList())
        assertTrue(array.isEmpty)
    }

    @Test
    fun `Array without null property return json without empty property`() {
        val array = mapper.mapExperimentsConfig(
            generateListConfig(
                testId = "testTestId",
                bucketId = "testAlias",
                alias = "testBucketId"
            )
        )
        assertEquals(
            array,
            generateJsonArray(
                testId = "testTestId",
                bucketId = "testAlias",
                alias = "testBucketId"
            )
        )
    }

    @Test
    fun `Property alias is null return json so alias is empty `() {
        val array = mapper.mapExperimentsConfig(
            generateListConfig(
                testId = "testTestId",
                bucketId = "testAlias",
                alias = null
            )
        )
        assertEquals(
            array,
            generateJsonArray(
                testId = "testTestId",
                bucketId = "testAlias",
                alias = null
            )
        )
    }

    @Test
    fun `Property bucketId is null return json so bucketId is empty `() {
        val array = mapper.mapExperimentsConfig(
            generateListConfig(
                testId = "testTestId",
                bucketId = null,
                alias = "testBucketId"
            )
        )
        assertEquals(
            array,
            generateJsonArray(
                testId = "testTestId",
                bucketId = null,
                alias = "testBucketId"
            )
        )
    }

    companion object {
        private const val PROPERTY_TEST_ID = "testId"
        private const val PROPERTY_BUCKET_ID = "bucketId"
        private const val PROPERTY_ALIAS = "alias"
        private const val EMPTY_VALUE = ""

        private fun generateListConfig(
            testId: String,
            bucketId: String?,
            alias: String?
        ): List<ExperimentConfig> {
            return listOf(
                ExperimentConfig(
                    testId = testId,
                    alias = alias,
                    bucketId = bucketId,
                    rearrFlags = emptyList(),
                    isOverride = false
                )
            )
        }

        private fun generateJsonArray(
            testId: String,
            bucketId: String?,
            alias: String?
        ): JsonArray {
            val array = JsonArray()
            val json = JsonObject()
            json.addProperty(PROPERTY_TEST_ID, testId)
            json.addProperty(PROPERTY_BUCKET_ID, bucketId ?: EMPTY_VALUE)
            json.addProperty(PROPERTY_ALIAS, alias ?: EMPTY_VALUE)
            array.add(json)
            return array
        }
    }
}
