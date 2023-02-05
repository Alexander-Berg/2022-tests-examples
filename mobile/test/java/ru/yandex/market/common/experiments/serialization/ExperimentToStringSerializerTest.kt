package ru.yandex.market.common.experiments.serialization

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.common.experiments.config.ExperimentConfig

@RunWith(Parameterized::class)
class ExperimentToStringSerializerTest(
    private val input: List<ExperimentConfig>,
    private val expectedOutput: String
) {
    @Test
    fun `Test experiment config serialization`() {
        val serializer = ExperimentToStringSerializer()

        val emptyListSerializationResult = serializer.serialize(input)
        assertThat(emptyListSerializationResult).isEqualTo(expectedOutput)
    }


    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(emptyList<ExperimentConfig>(), ""),
            arrayOf(
                listOf(
                    ExperimentConfig("testId", "alias", null, listOf("1","2"), true)
                ), "" // undefined bucket id results in skipping config object from serialization
            ),
            arrayOf(
                listOf(
                    ExperimentConfig("testId", null, "bucketId", null, true)
                ), "testId,bucketId,"  // serializer does not crash with empty alias
            ),
            arrayOf(
                listOf(
                    ExperimentConfig("1", "aliasName", "2", null, false)
                ), "1,2,aliasName"  // serializer processe test id, bucket id and alias id correctly
            ),
            arrayOf(
                listOf(
                    ExperimentConfig("10", "azaza", "bucket", listOf("rear_flag_1=1;"), true)
                ), "10,bucket,azaza"  // serializer ignores rears and override flags
            ),
        )
    }
}