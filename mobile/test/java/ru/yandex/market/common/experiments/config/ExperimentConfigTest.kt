package ru.yandex.market.common.experiments.config

import com.google.gson.annotations.SerializedName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExperimentConfigTest {
    @Test
    fun `Test data class properties serialized names`() {

        val fieldsToTest = ExperimentConfig.DEFAULT_CONFIG::class.java.fields
        for (aField in fieldsToTest) {
            val annotation = aField.getAnnotation(SerializedName::class.java)
            annotation?.run {
                assertThat(SERIALIZED_NAME_MAP.containsKey(aField.name)).isTrue

                val expectedSerializedName = SERIALIZED_NAME_MAP[aField.name]
                assertThat(value).isEqualTo(expectedSerializedName)
            }
        }
    }

    @Test
    fun `Test ExperimentConfig Builder`() {
        val experimentConfig = ExperimentConfig.Builder()
            .setAlias(ALIAS)
            .setBucketId(BUCKET_ID)
            .setIsOverride(IS_OVERRIDE)
            .setRearrFlags(REAR_FLAGS)
            .setTestId(TEST_ID)
            .build()

        assertThat(experimentConfig.alias).isEqualTo(ALIAS)
        assertThat(experimentConfig.bucketId).isEqualTo(BUCKET_ID)
        assertThat(experimentConfig.isOverride).isEqualTo(IS_OVERRIDE)
        assertThat(experimentConfig.rearrFlags).hasSameSizeAs(REAR_FLAGS).containsAll(REAR_FLAGS)
        assertThat(experimentConfig.testId).isEqualTo(TEST_ID)
    }

    companion object {

        const val ALIAS = "experiment_alias_test_098"
        const val BUCKET_ID = "bucket_id_test_675"
        const val IS_OVERRIDE = true
        const val TEST_ID = "test_id_1234"

        /** Field name to serialized name mapping. */
        val SERIALIZED_NAME_MAP = mapOf<String, String?>(
            "testId" to "testId",
            "alias" to "alias",
            "bucketId" to "bucketId",
            "rearrFlags" to "rearrFactors",
            "isOverride" to "isOverride",
        )

        val REAR_FLAGS = listOf("rear_flag1", "rear_flag2", "rear_flag3")
    }
}